import { Component, input, output, inject } from '@angular/core';

import { TranslatePipe } from '../../../shared/pipes/translate.pipe';
import { WhiteboardRoom, CursorPosition } from '../../../core/models/whiteboard.model';
import { I18nService } from '../../../core/services/i18n.service';
import { getDevColor } from '../../../core/utils/dev-colors';

@Component({
  selector: 'app-whiteboard-topbar',
  standalone: true,
  imports: [TranslatePipe],
  templateUrl: './whiteboard-topbar.html',
  styleUrl: './whiteboard-topbar.scss',
})
export class WhiteboardTopbarComponent {

  // Attributes

  readonly room = input<WhiteboardRoom | null>(null);
  readonly connectedUsers = input<CursorPosition[]>([]);

  readonly backClicked = output<void>();

  private readonly _i18n = inject(I18nService);

  // Methods

  getUserColor(userId: number): string {
    return getDevColor(userId);
  }

  getInitial(username: string): string {
    return username?.charAt(0).toUpperCase() ?? '?';
  }

  formatLastEdited(): string {
    const r = this.room();
    if (!r?.updatedAt) return '';
    const date = new Date(r.updatedAt);
    return date.toLocaleDateString(this._i18n.currentLanguage() === 'fr' ? 'fr-FR' : 'en-US', {
      month: 'long',
      day: 'numeric',
    });
  }

  goBack(): void {
    this.backClicked.emit();
  }
}
