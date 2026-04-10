import { Component, input, output, signal, inject, computed } from '@angular/core';

import { TranslatePipe } from '../../../shared/pipes/translate.pipe';
import { WhiteboardComment } from '../../../core/models/whiteboard.model';
import { AuthService } from '../../../core/services/auth.service';
import { getDevColor } from '../../../core/utils/dev-colors';
import { I18nService } from '../../../core/services/i18n.service';

@Component({
  selector: 'app-whiteboard-comments',
  standalone: true,
  imports: [TranslatePipe],
  templateUrl: './whiteboard-comments.html',
  styleUrl: './whiteboard-comments.scss',
})
export class WhiteboardCommentsComponent {

  // Attributes

  readonly comments = input<WhiteboardComment[]>([]);
  readonly commentAdded = output<string>();
  readonly commentDeleted = output<number>();
  readonly panelClosed = output<void>();

  readonly commentText = signal('');
  readonly filterMode = signal<'all' | 'mine'>('all');
  readonly openMenuId = signal<number | null>(null);

  private readonly _i18n = inject(I18nService);
  private readonly _auth = inject(AuthService);

  readonly filteredComments = computed(() => {
    const all = this.comments();
    if (this.filterMode() === 'mine') {
      const userId = this._auth.currentUser()?.id;
      return all.filter(c => c.developer.id === userId);
    }
    return all;
  });

  // Methods

  getAvatarColor(id: number): string {
    return getDevColor(id);
  }

  getInitial(username: string): string {
    return username?.charAt(0).toUpperCase() ?? '?';
  }

  formatDate(dateStr: string): string {
    const date = new Date(dateStr);
    return date.toLocaleDateString(this._i18n.currentLanguage() === 'fr' ? 'fr-FR' : 'en-US', {
      month: 'long',
      day: 'numeric',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  }

  onInput(event: Event): void {
    this.commentText.set((event.target as HTMLInputElement).value);
  }

  sendComment(): void {
    const text = this.commentText().trim();
    if (!text) return;
    this.commentAdded.emit(text);
    this.commentText.set('');
  }

  onKeydown(event: KeyboardEvent): void {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      this.sendComment();
    }
  }

  setFilter(mode: 'all' | 'mine'): void {
    this.filterMode.set(mode);
  }

  toggleMenu(commentId: number): void {
    this.openMenuId.update(id => id === commentId ? null : commentId);
  }

  deleteComment(commentId: number): void {
    this.openMenuId.set(null);
    this.commentDeleted.emit(commentId);
  }

  isOwnComment(comment: WhiteboardComment): boolean {
    return comment.developer.id === this._auth.currentUser()?.id;
  }

  close(): void {
    this.panelClosed.emit();
  }
}
