import { Component, input } from '@angular/core';

import { TranslatePipe } from '../../pipes/translate.pipe';

@Component({
  selector: 'app-empty-state',
  standalone: true,
  imports: [TranslatePipe],
  template: `
    <div class="empty-state" role="status">
      <div class="empty-icon">{{ icon() }}</div>
      <p class="empty-message">{{ message() | translate }}</p>
      @if (subMessage()) {
        <p class="empty-sub">{{ subMessage() | translate }}</p>
      }
    </div>
  `,
  styles: [`
    .empty-state {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      padding: 3rem 1.5rem;
      text-align: center;
      color: var(--text-tertiary);
    }
    .empty-icon {
      font-size: 2.5rem;
      margin-bottom: 1rem;
      opacity: 0.5;
    }
    .empty-message {
      font-size: 0.9375rem;
      color: var(--text-secondary);
      margin: 0 0 0.5rem;
    }
    .empty-sub {
      font-size: 0.8125rem;
      color: var(--text-tertiary);
      margin: 0;
    }
  `]
})
export class EmptyStateComponent {

  // Attributes

  readonly icon = input<string>('📭');
  readonly message = input<string>('common.empty');
  readonly subMessage = input<string>('');
}
