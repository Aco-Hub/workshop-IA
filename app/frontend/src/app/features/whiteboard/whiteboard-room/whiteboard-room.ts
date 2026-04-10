import { Component, ElementRef, HostListener, OnDestroy, OnInit, ViewChild, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { Subscription } from 'rxjs';

import { WhiteboardToolbarComponent } from '../whiteboard-toolbar/whiteboard-toolbar';
import { WhiteboardTopbarComponent } from '../whiteboard-topbar/whiteboard-topbar';
import { WhiteboardCommentsComponent } from '../whiteboard-comments/whiteboard-comments';
import { WhiteboardBottomBarComponent } from '../whiteboard-bottom-bar/whiteboard-bottom-bar';
import { WhiteboardZoomComponent } from '../whiteboard-zoom/whiteboard-zoom';
import { ModalComponent } from '../../../shared/components/modal/modal';
import { WhiteboardService } from '../../../core/services/whiteboard.service';
import { WebSocketService } from '../../../core/services/websocket.service';
import { AuthService } from '../../../core/services/auth.service';
import { TranslatePipe } from '../../../shared/pipes/translate.pipe';
import { CanvasEngine, CanvasElement, ToolType } from '../../../core/services/canvas-engine.service';
import { WhiteboardRoom, WhiteboardComment, WhiteboardElement, CursorPosition } from '../../../core/models/whiteboard.model';
import { getDevColor } from '../../../core/utils/dev-colors';

@Component({
  selector: 'app-whiteboard-room',
  standalone: true,
  imports: [
    FormsModule,
    WhiteboardToolbarComponent,
    WhiteboardTopbarComponent,
    WhiteboardCommentsComponent,
    WhiteboardBottomBarComponent,
    WhiteboardZoomComponent,
    ModalComponent,
    TranslatePipe,
  ],
  templateUrl: './whiteboard-room.html',
  styleUrl: './whiteboard-room.scss',
})
export class WhiteboardRoomComponent implements OnInit, OnDestroy {

  // Attributes

  @ViewChild('canvasRef', { static: true }) private readonly _canvasRef!: ElementRef<HTMLCanvasElement>;
  @ViewChild('fileInput', { static: true }) private readonly _fileInput!: ElementRef<HTMLInputElement>;

  private readonly _route = inject(ActivatedRoute);
  private readonly _router = inject(Router);
  private readonly _whiteboardService = inject(WhiteboardService);
  private readonly _wsService = inject(WebSocketService);
  private readonly _auth = inject(AuthService);

  private _engine = new CanvasEngine();
  private _roomId = 0;
  private _subscriptions: Subscription[] = [];
  private _cursorThrottle = 0;
  private _spaceHeld = false;
  private _tempIdCounter = -1;
  private readonly _pendingTempIds: number[] = [];

  readonly room = signal<WhiteboardRoom | null>(null);
  readonly comments = signal<WhiteboardComment[]>([]);
  readonly remoteCursors = signal<CursorPosition[]>([]);
  readonly connectedUsers = signal<CursorPosition[]>([]);
  readonly showComments = signal(false);
  readonly activeTool = signal<ToolType>('pen');
  readonly activeColor = signal('#000000');
  readonly zoomLevel = signal(1);
  readonly pendingTextElement = signal<CanvasElement | null>(null);
  readonly pendingTextPosition = signal<{ left: number; top: number }>({ left: 0, top: 0 });
  readonly showLinkModal = signal(false);
  readonly linkUrl = signal('');
  private _pendingLinkElement: CanvasElement | null = null;

  // Methods

  ngOnInit(): void {
    this._roomId = Number(this._route.snapshot.paramMap.get('id'));
    this._engine.init(this._canvasRef.nativeElement);
    this._engine.render();

    this._loadRoom();
    this._loadElements();
    this._loadComments();
    this._connectWebSocket();
  }

  ngOnDestroy(): void {
    this._subscriptions.forEach(s => s.unsubscribe());
    this._wsService.disconnect();
    this._engine.destroy();
  }

  @HostListener('window:resize')
  onResize(): void {
    this._engine.resize();
  }

  @HostListener('window:keydown', ['$event'])
  onKeyDown(event: KeyboardEvent): void {
    if (event.code === 'Space' && !event.repeat) {
      this._spaceHeld = true;
    }
    if (event.target instanceof HTMLInputElement || event.target instanceof HTMLSelectElement) return;

    switch (event.key.toLowerCase()) {
      case 'v': this.onToolChange('select'); break;
      case 'r': this.onToolChange('rectangle'); break;
      case 't': this.onToolChange('text'); break;
      case 'p': this.onToolChange('pen'); break;
      case 'e': this.onToolChange('eraser'); break;
      case 'l': this.onToolChange('line'); break;
    }
  }

  @HostListener('window:keyup', ['$event'])
  onKeyUp(event: KeyboardEvent): void {
    if (event.code === 'Space') {
      this._spaceHeld = false;
    }
  }

  onToolChange(tool: ToolType): void {
    this.activeTool.set(tool);
    this._engine.setTool(tool);
  }

  onColorChange(color: string): void {
    this.activeColor.set(color);
    this._engine.setColor(color);
  }

  onPointerDown(event: PointerEvent): void {
    const rect = this._canvasRef.nativeElement.getBoundingClientRect();
    this._engine.onPointerDown(event.clientX - rect.left, event.clientY - rect.top, this._spaceHeld);
  }

  onPointerMove(event: PointerEvent): void {
    const rect = this._canvasRef.nativeElement.getBoundingClientRect();
    const x = event.clientX - rect.left;
    const y = event.clientY - rect.top;
    this._engine.onPointerMove(x, y);
    this._broadcastCursor(x, y);
    this.zoomLevel.set(this._engine.getZoom());
  }

  onPointerUp(event: PointerEvent): void {
    const rect = this._canvasRef.nativeElement.getBoundingClientRect();
    const result = this._engine.onPointerUp(event.clientX - rect.left, event.clientY - rect.top);
    this.zoomLevel.set(this._engine.getZoom());

    if (!result) return;

    if (this.activeTool() === 'eraser' && result.zIndex === -1) {
      this._wsService.sendElementDelete(this._roomId, result.id);
      return;
    }

    if (this.activeTool() === 'text' && result.type === 'TEXT') {
      this.pendingTextElement.set(result);
      this.pendingTextPosition.set({
        left: event.clientX - rect.left,
        top: event.clientY - rect.top,
      });
      return;
    }

    if (result.type === 'IMAGE') {
      this._fileInput.nativeElement.click();
      return;
    }

    if (result.type === 'LINK') {
      this._pendingLinkElement = result;
      this.linkUrl.set('');
      this.showLinkModal.set(true);
      return;
    }

    this._broadcastElement(result);
  }

  onImageSelected(event: Event): void {
    const file = (event.target as HTMLInputElement).files?.[0];
    if (!file) return;

    const reader = new FileReader();
    reader.onload = () => {
      const base64 = reader.result as string;
      const img = new Image();
      img.onload = () => {
        const maxW = 400;
        const ratio = Math.min(maxW / img.width, 1);
        const w = img.width * ratio;
        const h = img.height * ratio;

        const data = JSON.stringify({ x: 100, y: 100, width: w, height: h, imageData: base64 });
        this._wsService.sendElementCreate(this._roomId, {
          type: 'IMAGE',
          data,
          color: '#000000',
          strokeWidth: 0,
          zIndex: 0,
        });
      };
      img.src = base64;
    };
    reader.readAsDataURL(file);
    (event.target as HTMLInputElement).value = '';
  }

  onImageRequested(): void {
    this._fileInput.nativeElement.click();
  }

  onLinkRequested(): void {
    this._pendingLinkElement = {
      id: -1,
      type: 'LINK',
      x: 200,
      y: 200,
      url: '',
      color: '#4774D5',
      strokeWidth: 0,
      zIndex: 0,
    };
    this.linkUrl.set('');
    this.showLinkModal.set(true);
  }

  submitTextInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    const text = input.value.trim();
    const pending = this.pendingTextElement();
    if (!text || !pending) {
      this.pendingTextElement.set(null);
      return;
    }
    pending.text = text;
    this._broadcastElement(pending);
    this.pendingTextElement.set(null);
  }

  cancelTextInput(): void {
    this.pendingTextElement.set(null);
  }

  submitLinkModal(): void {
    const url = this.linkUrl().trim();
    if (!url || !this._pendingLinkElement) {
      this.showLinkModal.set(false);
      this._pendingLinkElement = null;
      return;
    }
    this._pendingLinkElement.url = url;
    this._broadcastElement(this._pendingLinkElement);
    this.showLinkModal.set(false);
    this._pendingLinkElement = null;
  }

  onFillModeChange(mode: 'solid' | 'stroke' | 'pattern' | 'gradient'): void {
    this._engine.setFillMode(mode);
  }

  onFontSizeChange(size: number): void {
    this._engine.setFontSize(size);
  }

  onFontFamilyChange(family: string): void {
    this._engine.setFontFamily(family);
  }

  onStrokeWidthChange(width: number): void {
    this._engine.setStrokeWidth(width);
  }

  onCommentAdded(text: string): void {
    this._whiteboardService.addComment(this._roomId, text).subscribe({
      next: (comment) => {
        this.comments.update(c => [...c, comment]);
      },
    });
  }

  onCommentDeleted(commentId: number): void {
    this._whiteboardService.deleteComment(this._roomId, commentId).subscribe({
      next: () => {
        this.comments.update(c => c.filter(x => x.id !== commentId));
      },
    });
  }

  toggleComments(): void {
    this.showComments.update(v => !v);
  }

  onZoomIn(): void {
    this._engine.zoomIn();
    this.zoomLevel.set(this._engine.getZoom());
  }

  onZoomOut(): void {
    this._engine.zoomOut();
    this.zoomLevel.set(this._engine.getZoom());
  }

  onZoomReset(): void {
    this._engine.resetZoom();
    this.zoomLevel.set(1);
  }

  goBack(): void {
    this._router.navigate(['/whiteboard']);
  }

  // Private methods

  private _broadcastElement(el: CanvasElement): void {
    el.id = this._tempIdCounter--;
    this._pendingTempIds.push(el.id);
    this._engine.addElement(el);

    const data = this._serializeElement(el);
    this._wsService.sendElementCreate(this._roomId, {
      type: el.type,
      data,
      color: el.color,
      strokeWidth: el.strokeWidth,
      zIndex: el.zIndex,
    });
  }

  private _loadRoom(): void {
    this._whiteboardService.getRoom(this._roomId).subscribe({
      next: (room) => this.room.set(room),
    });
  }

  private _loadElements(): void {
    this._whiteboardService.getElements(this._roomId).subscribe({
      next: (elements) => {
        const canvasElements = elements.map(e => this._deserializeElement(e));
        this._engine.loadElements(canvasElements);
      },
    });
  }

  private _loadComments(): void {
    this._whiteboardService.getComments(this._roomId).subscribe({
      next: (comments) => this.comments.set(comments),
    });
  }

  private _connectWebSocket(): void {
    this._wsService.connect(this._roomId).subscribe({
      next: () => {
        const cursorSub = this._wsService.subscribeToCursors(this._roomId).subscribe({
          next: (cursor) => this._handleRemoteCursor(cursor),
        });

        const elementSub = this._wsService.subscribeToElements(this._roomId).subscribe({
          next: (event) => {
            if (event.action === 'CREATE' && event.element) {
              const userId = this._auth.currentUser()?.id;
              if (event.element.createdById === userId && this._pendingTempIds.length > 0) {
                const tempId = this._pendingTempIds.shift()!;
                this._engine.removeElement(tempId);
              }
              const el = this._deserializeElement(event.element);
              this._engine.addElement(el);
            } else if (event.action === 'DELETE' && event.element) {
              this._engine.removeElement(event.element.id);
            }
          },
        });

        this._subscriptions.push(cursorSub, elementSub);
      },
    });
  }

  private _broadcastCursor(x: number, y: number): void {
    const now = Date.now();
    if (now - this._cursorThrottle < 30) return;
    this._cursorThrottle = now;

    const user = this._auth.currentUser();
    if (!user) return;

    this._wsService.sendCursorPosition(this._roomId, {
      userId: user.id,
      username: user.username,
      color: getDevColor(user.id),
      x,
      y,
    });
  }

  private _handleRemoteCursor(cursor: CursorPosition): void {
    const user = this._auth.currentUser();
    if (user && cursor.userId === user.id) return;

    this.remoteCursors.update(cursors => {
      const existing = cursors.findIndex(c => c.userId === cursor.userId);
      if (existing >= 0) {
        const updated = [...cursors];
        updated[existing] = cursor;
        return updated;
      }
      return [...cursors, cursor];
    });

    this.connectedUsers.update(users => {
      if (!users.find(u => u.userId === cursor.userId)) {
        return [...users, cursor];
      }
      return users;
    });
  }

  private _serializeElement(el: CanvasElement): string {
    if (el.type === 'PEN' || el.type === 'LINE') {
      return JSON.stringify({ points: el.points });
    }
    if (el.type === 'RECTANGLE' || el.type === 'ELLIPSE') {
      return JSON.stringify({ x: el.x, y: el.y, width: el.width, height: el.height });
    }
    if (el.type === 'TEXT') {
      return JSON.stringify({ x: el.x, y: el.y, text: el.text, fontSize: el.fontSize });
    }
    if (el.type === 'IMAGE') {
      return JSON.stringify({ x: el.x, y: el.y, width: el.width, height: el.height, imageData: el.imageData });
    }
    if (el.type === 'LINK') {
      return JSON.stringify({ x: el.x, y: el.y, url: el.url });
    }
    return '{}';
  }

  private _deserializeElement(el: WhiteboardElement): CanvasElement {
    const parsed = JSON.parse(el.data);
    return {
      id: el.id,
      type: el.type,
      points: parsed.points,
      x: parsed.x,
      y: parsed.y,
      width: parsed.width,
      height: parsed.height,
      text: parsed.text,
      fontSize: parsed.fontSize,
      imageData: parsed.imageData,
      url: parsed.url,
      color: el.color,
      strokeWidth: el.strokeWidth,
      zIndex: el.zIndex,
    };
  }
}
