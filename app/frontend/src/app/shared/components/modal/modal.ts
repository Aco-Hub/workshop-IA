import { Component, input, output, effect } from '@angular/core';

import { TranslatePipe } from '../../pipes/translate.pipe';

@Component({
  selector: 'app-modal',
  standalone: true,
  imports: [TranslatePipe],
  template: `
    @if (isOpen()) {
      <div
        class="modal-backdrop"
        (click)="onBackdropClick($event)"
        role="dialog"
        aria-modal="true"
        [attr.aria-label]="title()"
        data-testid="modal-backdrop"
      >
        <div class="modal-container" (click)="$event.stopPropagation()">
          <div class="modal-header">
            <h2 class="modal-title">{{ title() }}</h2>
            <button
              class="modal-close"
              (click)="closed.emit()"
              [attr.aria-label]="'common.close' | translate"
              data-testid="modal-close"
            >
              ✕
            </button>
          </div>
          <div class="modal-body">
            <ng-content></ng-content>
          </div>
        </div>
      </div>
    }
  `,
  styles: [`
    .modal-backdrop {
      position: fixed;
      inset: 0;
      background: rgba(0, 0, 0, 0.4);
      display: flex;
      align-items: center;
      justify-content: center;
      z-index: 1000;
      padding: 1rem;
      animation: fadeIn 0.15s ease;
    }
    .modal-container {
      background: var(--bg-primary);
      border-radius: 8px;
      box-shadow: 0 8px 32px var(--shadow), 0 0 0 1px var(--border-color);
      width: 100%;
      max-width: 480px;
      max-height: 90vh;
      overflow-y: auto;
      animation: slideUp 0.2s ease;
    }
    .modal-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 1.25rem 1.5rem 0;
    }
    .modal-title {
      font-size: 1rem;
      font-weight: 600;
      color: var(--text-primary);
      margin: 0;
    }
    .modal-close {
      background: none;
      border: none;
      cursor: pointer;
      color: var(--text-tertiary);
      font-size: 1rem;
      padding: 0.25rem 0.5rem;
      border-radius: 4px;
      transition: color 0.15s ease, background 0.15s ease;
      line-height: 1;
    }
    .modal-close:hover {
      color: var(--text-primary);
      background: var(--bg-hover);
    }
    .modal-close:focus {
      outline: 2px solid var(--accent-color);
      outline-offset: 2px;
    }
    .modal-body {
      padding: 1.25rem 1.5rem 1.5rem;
    }
    @keyframes fadeIn {
      from { opacity: 0; }
      to { opacity: 1; }
    }
    @keyframes slideUp {
      from { transform: translateY(8px); opacity: 0; }
      to { transform: translateY(0); opacity: 1; }
    }
  `]
})
export class ModalComponent {

  // Attributes

  readonly isOpen = input<boolean>(false);
  readonly title = input<string>('');
  readonly closeOnBackdrop = input<boolean>(true);
  readonly closed = output<void>();

  // Constructor

  constructor() {
    effect(() => {
      if (this.isOpen()) {
        document.body.style.overflow = 'hidden';
      } else {
        document.body.style.overflow = '';
      }
    });
  }

  // Methods

  onBackdropClick(event: MouseEvent): void {
    if (this.closeOnBackdrop()) {
      this.closed.emit();
    }
  }
}
