import { ElementType } from '../models/whiteboard.model';

export interface CanvasElement {
  id: number;
  type: ElementType;
  points?: number[][];
  x?: number;
  y?: number;
  width?: number;
  height?: number;
  text?: string;
  fontSize?: number;
  color: string;
  strokeWidth: number;
  zIndex: number;
  imageData?: string;
  url?: string;
}

export type ToolType = 'pen' | 'select' | 'rectangle' | 'ellipse' | 'line' | 'text' | 'eraser' | 'image' | 'link';

export class CanvasEngine {

  // Attributes

  private _canvas: HTMLCanvasElement | null = null;
  private _ctx: CanvasRenderingContext2D | null = null;
  private _elements: CanvasElement[] = [];
  private _currentTool: ToolType = 'pen';
  private _currentColor = '#000000';
  private _currentStrokeWidth = 2;
  private _currentFillMode: 'solid' | 'stroke' | 'pattern' | 'gradient' = 'stroke';
  private _currentFontSize = 16;
  private _currentFontFamily = 'Inter';
  private _panX = 0;
  private _panY = 0;
  private _scale = 1;
  private _drawing = false;
  private _panning = false;
  private _panStartX = 0;
  private _panStartY = 0;
  private _currentPoints: number[][] = [];
  private _startX = 0;
  private _startY = 0;
  private _selectedElementId: number | null = null;
  private readonly _imageCache = new Map<string, HTMLImageElement>();
  private _onWheelBound: ((e: WheelEvent) => void) | null = null;

  // Methods

  init(canvas: HTMLCanvasElement): void {
    this._canvas = canvas;
    this._ctx = canvas.getContext('2d')!;
    this._resize();

    this._onWheelBound = (e: WheelEvent) => this._onWheel(e);
    canvas.addEventListener('wheel', this._onWheelBound, { passive: false });
  }

  destroy(): void {
    if (this._canvas && this._onWheelBound) {
      this._canvas.removeEventListener('wheel', this._onWheelBound);
    }
    this._canvas = null;
    this._ctx = null;
    this._elements = [];
    this._imageCache.clear();
  }

  resize(): void {
    this._resize();
    this.render();
  }

  setTool(tool: ToolType): void {
    this._currentTool = tool;
    this._selectedElementId = null;
  }

  getTool(): ToolType {
    return this._currentTool;
  }

  setColor(color: string): void {
    this._currentColor = color;
  }

  setStrokeWidth(width: number): void {
    this._currentStrokeWidth = width;
  }

  setFillMode(mode: 'solid' | 'stroke' | 'pattern' | 'gradient'): void {
    this._currentFillMode = mode;
  }

  setFontSize(size: number): void {
    this._currentFontSize = size;
  }

  setFontFamily(family: string): void {
    this._currentFontFamily = family;
  }

  getZoom(): number {
    return this._scale;
  }

  setZoom(level: number): void {
    this._scale = Math.max(0.1, Math.min(5, level));
    this.render();
  }

  zoomIn(): void {
    this.setZoom(this._scale * 1.2);
  }

  zoomOut(): void {
    this.setZoom(this._scale / 1.2);
  }

  resetZoom(): void {
    this._scale = 1;
    this._panX = 0;
    this._panY = 0;
    this.render();
  }

  addElement(element: CanvasElement): void {
    if (element.type === 'IMAGE' && element.imageData) {
      this._loadImage(element.imageData);
    }
    this._elements.push(element);
    this._elements.sort((a, b) => a.zIndex - b.zIndex);
    this.render();
  }

  removeElement(id: number): void {
    this._elements = this._elements.filter(e => e.id !== id);
    this.render();
  }

  clearAll(): void {
    this._elements = [];
    this.render();
  }

  loadElements(elements: CanvasElement[]): void {
    this._elements = elements.sort((a, b) => a.zIndex - b.zIndex);
    for (const el of this._elements) {
      if (el.type === 'IMAGE' && el.imageData) {
        this._loadImage(el.imageData);
      }
    }
    this.render();
  }

  onPointerDown(x: number, y: number, spaceHeld: boolean): void {
    if (spaceHeld) {
      this._panning = true;
      this._panStartX = x - this._panX;
      this._panStartY = y - this._panY;
      return;
    }

    const [cx, cy] = this._screenToCanvas(x, y);
    this._drawing = true;
    this._startX = cx;
    this._startY = cy;

    if (this._currentTool === 'pen' || this._currentTool === 'eraser') {
      this._currentPoints = [[cx, cy]];
    } else if (this._currentTool === 'select') {
      this._selectedElementId = this._hitTest(cx, cy);
      this.render();
    }
  }

  onPointerMove(x: number, y: number): void {
    if (this._panning) {
      this._panX = x - this._panStartX;
      this._panY = y - this._panStartY;
      this.render();
      return;
    }

    if (!this._drawing) return;
    const [cx, cy] = this._screenToCanvas(x, y);

    if (this._currentTool === 'pen' || this._currentTool === 'eraser') {
      this._currentPoints.push([cx, cy]);
      this.render();
      this._drawLiveStroke();
    } else if (this._currentTool === 'rectangle' || this._currentTool === 'ellipse' || this._currentTool === 'line') {
      this.render();
      this._drawLiveShape(cx, cy);
    }
  }

  onPointerUp(x: number, y: number): CanvasElement | null {
    if (this._panning) {
      this._panning = false;
      return null;
    }

    if (!this._drawing) return null;
    this._drawing = false;
    const [cx, cy] = this._screenToCanvas(x, y);

    if (this._currentTool === 'pen') {
      if (this._currentPoints.length < 2) return null;
      const element: CanvasElement = {
        id: -1,
        type: 'PEN',
        points: [...this._currentPoints],
        color: this._currentColor,
        strokeWidth: this._currentStrokeWidth,
        zIndex: this._elements.length,
      };
      this._currentPoints = [];
      return element;
    }

    if (this._currentTool === 'eraser') {
      const erasedId = this._hitTest(cx, cy);
      this._currentPoints = [];
      if (erasedId !== null) {
        this.removeElement(erasedId);
        return { id: erasedId, type: 'PEN', color: '', strokeWidth: 0, zIndex: -1 };
      }
      return null;
    }

    if (this._currentTool === 'rectangle') {
      const w = cx - this._startX;
      const h = cy - this._startY;
      if (Math.abs(w) < 2 && Math.abs(h) < 2) return null;
      return {
        id: -1,
        type: 'RECTANGLE',
        x: Math.min(this._startX, cx),
        y: Math.min(this._startY, cy),
        width: Math.abs(w),
        height: Math.abs(h),
        color: this._currentColor,
        strokeWidth: this._currentStrokeWidth,
        zIndex: this._elements.length,
      };
    }

    if (this._currentTool === 'ellipse') {
      const w = cx - this._startX;
      const h = cy - this._startY;
      if (Math.abs(w) < 2 && Math.abs(h) < 2) return null;
      return {
        id: -1,
        type: 'ELLIPSE',
        x: Math.min(this._startX, cx),
        y: Math.min(this._startY, cy),
        width: Math.abs(w),
        height: Math.abs(h),
        color: this._currentColor,
        strokeWidth: this._currentStrokeWidth,
        zIndex: this._elements.length,
      };
    }

    if (this._currentTool === 'line') {
      const dist = Math.hypot(cx - this._startX, cy - this._startY);
      if (dist < 2) return null;
      return {
        id: -1,
        type: 'LINE',
        points: [[this._startX, this._startY], [cx, cy]],
        color: this._currentColor,
        strokeWidth: this._currentStrokeWidth,
        zIndex: this._elements.length,
      };
    }

    if (this._currentTool === 'text') {
      return {
        id: -1,
        type: 'TEXT',
        x: cx,
        y: cy,
        text: '',
        fontSize: this._currentFontSize,
        color: this._currentColor,
        strokeWidth: this._currentStrokeWidth,
        zIndex: this._elements.length,
      };
    }

    if (this._currentTool === 'image') {
      return {
        id: -1,
        type: 'IMAGE',
        x: cx,
        y: cy,
        width: 200,
        height: 150,
        imageData: '',
        color: this._currentColor,
        strokeWidth: this._currentStrokeWidth,
        zIndex: this._elements.length,
      };
    }

    if (this._currentTool === 'link') {
      return {
        id: -1,
        type: 'LINK',
        x: cx,
        y: cy,
        url: '',
        color: this._currentColor,
        strokeWidth: this._currentStrokeWidth,
        zIndex: this._elements.length,
      };
    }

    return null;
  }

  render(): void {
    if (!this._ctx || !this._canvas) return;
    const ctx = this._ctx;
    const w = this._canvas.width;
    const h = this._canvas.height;

    ctx.clearRect(0, 0, w, h);
    ctx.fillStyle = '#FDFCFF';
    ctx.fillRect(0, 0, w, h);

    this._drawDotGrid();

    ctx.save();
    ctx.translate(this._panX, this._panY);
    ctx.scale(this._scale, this._scale);
    this._drawElements();
    ctx.restore();
  }

  screenToCanvas(x: number, y: number): [number, number] {
    return this._screenToCanvas(x, y);
  }

  // Private methods

  private _resize(): void {
    if (!this._canvas) return;
    const rect = this._canvas.parentElement!.getBoundingClientRect();
    this._canvas.width = rect.width;
    this._canvas.height = rect.height;
  }

  private _screenToCanvas(sx: number, sy: number): [number, number] {
    return [
      (sx - this._panX) / this._scale,
      (sy - this._panY) / this._scale,
    ];
  }

  private _onWheel(e: WheelEvent): void {
    e.preventDefault();
    if (e.ctrlKey || e.metaKey) {
      const delta = e.deltaY > 0 ? 0.9 : 1.1;
      const rect = this._canvas!.getBoundingClientRect();
      const mx = e.clientX - rect.left;
      const my = e.clientY - rect.top;

      const [wx, wy] = this._screenToCanvas(mx, my);
      this._scale = Math.max(0.1, Math.min(5, this._scale * delta));
      this._panX = mx - wx * this._scale;
      this._panY = my - wy * this._scale;
      this.render();
    } else {
      this._panX -= e.deltaX;
      this._panY -= e.deltaY;
      this.render();
    }
  }

  private _drawDotGrid(): void {
    if (!this._ctx || !this._canvas) return;
    const ctx = this._ctx;
    const spacing = 23;
    const dotSize = 1.5;

    ctx.fillStyle = '#D2D6DB';
    for (let x = spacing; x < this._canvas.width; x += spacing) {
      for (let y = spacing; y < this._canvas.height; y += spacing) {
        ctx.beginPath();
        ctx.arc(x, y, dotSize, 0, Math.PI * 2);
        ctx.fill();
      }
    }
  }

  private _drawElements(): void {
    if (!this._ctx) return;
    const ctx = this._ctx;

    for (const el of this._elements) {
      ctx.save();
      ctx.strokeStyle = el.color;
      ctx.fillStyle = el.color;
      ctx.lineWidth = el.strokeWidth;
      ctx.lineCap = 'round';
      ctx.lineJoin = 'round';

      if (el.id === this._selectedElementId) {
        ctx.shadowColor = '#4774D5';
        ctx.shadowBlur = 8;
      }

      switch (el.type) {
        case 'PEN':
          this._drawPenStroke(ctx, el.points ?? []);
          break;
        case 'RECTANGLE':
          this._drawRect(ctx, el);
          break;
        case 'ELLIPSE':
          this._drawEllipseElement(ctx, el);
          break;
        case 'LINE':
          if (el.points && el.points.length === 2) {
            ctx.beginPath();
            ctx.moveTo(el.points[0][0], el.points[0][1]);
            ctx.lineTo(el.points[1][0], el.points[1][1]);
            ctx.stroke();
          }
          break;
        case 'TEXT':
          ctx.font = `${el.fontSize ?? this._currentFontSize}px ${this._currentFontFamily}, sans-serif`;
          ctx.fillText(el.text ?? '', el.x ?? 0, el.y ?? 0);
          break;
        case 'IMAGE':
          this._drawImage(ctx, el);
          break;
        case 'LINK':
          this._drawLink(ctx, el);
          break;
      }

      ctx.restore();
    }
  }

  private _drawPenStroke(ctx: CanvasRenderingContext2D, points: number[][]): void {
    if (points.length < 2) return;
    ctx.beginPath();
    ctx.moveTo(points[0][0], points[0][1]);
    for (let i = 1; i < points.length; i++) {
      ctx.lineTo(points[i][0], points[i][1]);
    }
    ctx.stroke();
  }

  private _drawRect(ctx: CanvasRenderingContext2D, el: CanvasElement): void {
    const x = el.x ?? 0;
    const y = el.y ?? 0;
    const w = el.width ?? 0;
    const h = el.height ?? 0;
    const fillMode = (el as CanvasElement & { fillMode?: string }).fillMode ?? this._currentFillMode;

    if (fillMode === 'solid') {
      ctx.fillRect(x, y, w, h);
    } else if (fillMode === 'gradient') {
      const grad = ctx.createLinearGradient(x, y, x + w, y + h);
      grad.addColorStop(0, el.color);
      grad.addColorStop(1, 'transparent');
      ctx.fillStyle = grad;
      ctx.fillRect(x, y, w, h);
    } else if (fillMode === 'pattern') {
      ctx.strokeRect(x, y, w, h);
      ctx.save();
      ctx.lineWidth = 1;
      const step = 8;
      ctx.beginPath();
      for (let i = -h; i < w; i += step) {
        ctx.moveTo(x + i, y);
        ctx.lineTo(x + i + h, y + h);
      }
      ctx.clip();
      ctx.beginPath();
      ctx.rect(x, y, w, h);
      ctx.stroke();
      ctx.restore();
      ctx.save();
      ctx.lineWidth = 1;
      ctx.beginPath();
      for (let i = -h; i < w; i += step) {
        ctx.moveTo(x + i, y);
        ctx.lineTo(x + i + h, y + h);
      }
      ctx.stroke();
      ctx.restore();
    } else {
      ctx.strokeRect(x, y, w, h);
    }
  }

  private _drawEllipseElement(ctx: CanvasRenderingContext2D, el: CanvasElement): void {
    const x = el.x ?? 0;
    const y = el.y ?? 0;
    const w = el.width ?? 0;
    const h = el.height ?? 0;
    const fillMode = (el as CanvasElement & { fillMode?: string }).fillMode ?? this._currentFillMode;

    ctx.beginPath();
    ctx.ellipse(x + w / 2, y + h / 2, w / 2, h / 2, 0, 0, Math.PI * 2);

    if (fillMode === 'solid') {
      ctx.fill();
    } else if (fillMode === 'gradient') {
      const grad = ctx.createLinearGradient(x, y, x + w, y + h);
      grad.addColorStop(0, el.color);
      grad.addColorStop(1, 'transparent');
      ctx.fillStyle = grad;
      ctx.fill();
    } else {
      ctx.stroke();
    }
  }

  private _drawEllipse(ctx: CanvasRenderingContext2D, x: number, y: number, w: number, h: number): void {
    ctx.beginPath();
    ctx.ellipse(x + w / 2, y + h / 2, w / 2, h / 2, 0, 0, Math.PI * 2);
    ctx.stroke();
  }

  private _drawImage(ctx: CanvasRenderingContext2D, el: CanvasElement): void {
    const x = el.x ?? 0;
    const y = el.y ?? 0;
    const w = el.width ?? 200;
    const h = el.height ?? 150;

    if (el.imageData) {
      const img = this._imageCache.get(el.imageData);
      if (img?.complete) {
        ctx.drawImage(img, x, y, w, h);
        return;
      }
    }

    ctx.fillStyle = '#f0f0f0';
    ctx.fillRect(x, y, w, h);
    ctx.strokeStyle = '#ccc';
    ctx.strokeRect(x, y, w, h);
    ctx.fillStyle = '#999';
    ctx.font = '14px Inter, sans-serif';
    ctx.textAlign = 'center';
    ctx.fillText('Image', x + w / 2, y + h / 2);
    ctx.textAlign = 'start';
  }

  private _drawLink(ctx: CanvasRenderingContext2D, el: CanvasElement): void {
    const x = el.x ?? 0;
    const y = el.y ?? 0;
    const w = 180;
    const h = 40;

    ctx.fillStyle = '#FFFFFF';
    ctx.strokeStyle = '#EAEAEA';
    ctx.lineWidth = 1;

    ctx.beginPath();
    ctx.roundRect(x, y, w, h, 8);
    ctx.fill();
    ctx.stroke();

    ctx.fillStyle = '#4774D5';
    ctx.font = '13px Inter, sans-serif';
    const displayUrl = (el.url ?? 'Link').substring(0, 25);
    ctx.fillText(displayUrl, x + 12, y + 25);
  }

  private _loadImage(src: string): void {
    if (this._imageCache.has(src)) return;
    const img = new Image();
    img.onload = () => this.render();
    img.src = src;
    this._imageCache.set(src, img);
  }

  private _drawLiveStroke(): void {
    if (!this._ctx) return;
    const ctx = this._ctx;
    const color = this._currentTool === 'eraser' ? '#FF0000' : this._currentColor;

    ctx.save();
    ctx.translate(this._panX, this._panY);
    ctx.scale(this._scale, this._scale);
    ctx.strokeStyle = color;
    ctx.lineWidth = this._currentStrokeWidth;
    ctx.lineCap = 'round';
    ctx.lineJoin = 'round';
    this._drawPenStroke(ctx, this._currentPoints);
    ctx.restore();
  }

  private _drawLiveShape(cx: number, cy: number): void {
    if (!this._ctx) return;
    const ctx = this._ctx;

    ctx.save();
    ctx.translate(this._panX, this._panY);
    ctx.scale(this._scale, this._scale);
    ctx.strokeStyle = this._currentColor;
    ctx.lineWidth = this._currentStrokeWidth;
    ctx.lineCap = 'round';
    ctx.setLineDash([5, 5]);

    if (this._currentTool === 'rectangle') {
      const x = Math.min(this._startX, cx);
      const y = Math.min(this._startY, cy);
      ctx.strokeRect(x, y, Math.abs(cx - this._startX), Math.abs(cy - this._startY));
    } else if (this._currentTool === 'ellipse') {
      this._drawEllipse(ctx, Math.min(this._startX, cx), Math.min(this._startY, cy),
        Math.abs(cx - this._startX), Math.abs(cy - this._startY));
    } else if (this._currentTool === 'line') {
      ctx.beginPath();
      ctx.moveTo(this._startX, this._startY);
      ctx.lineTo(cx, cy);
      ctx.stroke();
    }

    ctx.restore();
  }

  private _hitTest(x: number, y: number): number | null {
    for (let i = this._elements.length - 1; i >= 0; i--) {
      const el = this._elements[i];
      if (el.type === 'PEN' && el.points) {
        for (const [px, py] of el.points) {
          if (Math.hypot(px - x, py - y) < 10) return el.id;
        }
      } else if (el.type === 'RECTANGLE' || el.type === 'ELLIPSE' || el.type === 'IMAGE') {
        const ex = el.x ?? 0;
        const ey = el.y ?? 0;
        if (x >= ex && x <= ex + (el.width ?? 0) && y >= ey && y <= ey + (el.height ?? 0)) {
          return el.id;
        }
      } else if (el.type === 'LINE' && el.points && el.points.length === 2) {
        const [p1, p2] = el.points;
        const dist = this._pointToLineDistance(x, y, p1[0], p1[1], p2[0], p2[1]);
        if (dist < 10) return el.id;
      } else if (el.type === 'TEXT') {
        const ex = el.x ?? 0;
        const ey = el.y ?? 0;
        if (x >= ex && x <= ex + 100 && y >= ey - 20 && y <= ey) {
          return el.id;
        }
      } else if (el.type === 'LINK') {
        const ex = el.x ?? 0;
        const ey = el.y ?? 0;
        if (x >= ex && x <= ex + 180 && y >= ey && y <= ey + 40) {
          return el.id;
        }
      }
    }
    return null;
  }

  private _pointToLineDistance(px: number, py: number, x1: number, y1: number, x2: number, y2: number): number {
    const A = px - x1;
    const B = py - y1;
    const C = x2 - x1;
    const D = y2 - y1;
    const dot = A * C + B * D;
    const lenSq = C * C + D * D;
    let t = lenSq !== 0 ? dot / lenSq : -1;
    t = Math.max(0, Math.min(1, t));
    const nearX = x1 + t * C;
    const nearY = y1 + t * D;
    return Math.hypot(px - nearX, py - nearY);
  }
}
