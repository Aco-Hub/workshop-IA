import { Component, input, output, signal } from '@angular/core';

import { TranslatePipe } from '../../../shared/pipes/translate.pipe';
import { ToolType } from '../../../core/services/canvas-engine.service';

@Component({
  selector: 'app-whiteboard-toolbar',
  standalone: true,
  imports: [TranslatePipe],
  templateUrl: './whiteboard-toolbar.html',
  styleUrl: './whiteboard-toolbar.scss',
})
export class WhiteboardToolbarComponent {

  // Attributes

  readonly activeTool = input<ToolType>('pen');
  readonly activeColor = input<string>('#000000');

  readonly toolChanged = output<ToolType>();
  readonly colorChanged = output<string>();
  readonly commentsToggled = output<void>();
  readonly imageRequested = output<void>();
  readonly linkRequested = output<void>();

  readonly showShapeMenu = signal(false);
  readonly showMoreMenu = signal(false);
  readonly showColorPicker = signal(false);

  readonly colors = [
    '#000000', '#434343', '#666666', '#999999',
    '#E03E3E', '#D9730D', '#DFAB01', '#0F7B6C',
    '#0B6E99', '#2383E2', '#6940A5', '#AD1A72',
  ];

  // Methods

  selectTool(tool: ToolType): void {
    this._closeMenus();
    this.toolChanged.emit(tool);
  }

  toggleShapeMenu(): void {
    this.showShapeMenu.update(v => !v);
    this.showMoreMenu.set(false);
  }

  selectShape(shape: 'rectangle' | 'ellipse' | 'line'): void {
    this.showShapeMenu.set(false);
    this.toolChanged.emit(shape);
  }

  onImageClick(): void {
    this._closeMenus();
    this.imageRequested.emit();
  }

  onLinkClick(): void {
    this._closeMenus();
    this.linkRequested.emit();
  }

  onCommentClick(): void {
    this._closeMenus();
    this.commentsToggled.emit();
  }

  toggleMoreMenu(): void {
    this.showMoreMenu.update(v => !v);
    this.showShapeMenu.set(false);
  }

  toggleColorPicker(): void {
    this.showColorPicker.update(v => !v);
  }

  selectColor(color: string): void {
    this.colorChanged.emit(color);
    this.showColorPicker.set(false);
  }

  onCustomColor(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.colorChanged.emit(input.value);
  }

  selectEraser(): void {
    this.showMoreMenu.set(false);
    this.toolChanged.emit('eraser');
  }

  isShapeActive(): boolean {
    const tool = this.activeTool();
    return tool === 'rectangle' || tool === 'ellipse' || tool === 'line';
  }

  // Private methods

  private _closeMenus(): void {
    this.showShapeMenu.set(false);
    this.showMoreMenu.set(false);
    this.showColorPicker.set(false);
  }
}
