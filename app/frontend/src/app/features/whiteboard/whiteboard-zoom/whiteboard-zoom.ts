import { Component, input, output } from '@angular/core';

import { TranslatePipe } from '../../../shared/pipes/translate.pipe';

@Component({
  selector: 'app-whiteboard-zoom',
  standalone: true,
  imports: [TranslatePipe],
  templateUrl: './whiteboard-zoom.html',
  styleUrl: './whiteboard-zoom.scss',
})
export class WhiteboardZoomComponent {

  // Attributes

  readonly zoomLevel = input<number>(1);

  readonly zoomIn = output<void>();
  readonly zoomOut = output<void>();
  readonly zoomReset = output<void>();

  // Methods

  getZoomPercent(): number {
    return Math.round(this.zoomLevel() * 100);
  }

  onZoomIn(): void {
    this.zoomIn.emit();
  }

  onZoomOut(): void {
    this.zoomOut.emit();
  }

  onReset(): void {
    this.zoomReset.emit();
  }
}
