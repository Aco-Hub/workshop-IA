import { Component, input, output, signal } from '@angular/core';

import { ToolType } from '../../../core/services/canvas-engine.service';

@Component({
  selector: 'app-whiteboard-bottom-bar',
  standalone: true,
  templateUrl: './whiteboard-bottom-bar.html',
  styleUrl: './whiteboard-bottom-bar.scss',
})
export class WhiteboardBottomBarComponent {

  // Attributes

  readonly activeTool = input<ToolType>('pen');
  readonly activeColor = input<string>('#000000');

  readonly colorChanged = output<string>();
  readonly strokeWidthChanged = output<number>();
  readonly fillModeChanged = output<'solid' | 'stroke' | 'pattern' | 'gradient'>();
  readonly fontSizeChanged = output<number>();
  readonly fontFamilyChanged = output<string>();

  readonly fillMode = signal<'solid' | 'stroke' | 'pattern' | 'gradient'>('stroke');
  readonly fontSize = signal(16);
  readonly fontFamily = signal('Inter');
  readonly strokeWidth = signal(2);
  readonly bold = signal(false);
  readonly italic = signal(false);

  // Methods

  isShapeTool(): boolean {
    const tool = this.activeTool();
    return tool === 'rectangle' || tool === 'ellipse' || tool === 'line';
  }

  isPenTool(): boolean {
    return this.activeTool() === 'pen';
  }

  isTextTool(): boolean {
    return this.activeTool() === 'text';
  }

  setFillMode(mode: 'solid' | 'stroke' | 'pattern' | 'gradient'): void {
    this.fillMode.set(mode);
    this.fillModeChanged.emit(mode);
  }

  setFontSize(event: Event): void {
    const value = (event.target as HTMLSelectElement).value;
    const size = Number(value);
    this.fontSize.set(size);
    this.fontSizeChanged.emit(size);
  }

  setFontFamily(event: Event): void {
    const value = (event.target as HTMLSelectElement).value;
    this.fontFamily.set(value);
    this.fontFamilyChanged.emit(value);
  }

  setStrokeWidth(event: Event): void {
    const value = Number((event.target as HTMLInputElement).value);
    this.strokeWidth.set(value);
    this.strokeWidthChanged.emit(value);
  }

  toggleBold(): void {
    this.bold.update(v => !v);
  }

  toggleItalic(): void {
    this.italic.update(v => !v);
  }
}
