import { Component, inject, OnInit, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';

import { TranslatePipe } from '../../shared/pipes/translate.pipe';
import { ModalComponent } from '../../shared/components/modal/modal';
import { WhiteboardService } from '../../core/services/whiteboard.service';
import { AuthService } from '../../core/services/auth.service';
import { WhiteboardRoom } from '../../core/models/whiteboard.model';
import { getDevColor } from '../../core/utils/dev-colors';

@Component({
  selector: 'app-whiteboard',
  standalone: true,
  imports: [TranslatePipe, RouterLink, ModalComponent, FormsModule],
  templateUrl: './whiteboard.html',
  styleUrl: './whiteboard.scss',
})
export class WhiteboardComponent implements OnInit {

  // Attributes

  private readonly _whiteboardService = inject(WhiteboardService);
  private readonly _auth = inject(AuthService);
  private readonly _router = inject(Router);

  readonly rooms = signal<WhiteboardRoom[]>([]);
  readonly showCreateModal = signal(false);
  readonly newRoomName = signal('');
  readonly loading = signal(false);

  // Methods

  ngOnInit(): void {
    this._loadRooms();
  }

  openCreateModal(): void {
    this.newRoomName.set('');
    this.showCreateModal.set(true);
  }

  submitCreate(): void {
    const name = this.newRoomName().trim();
    if (!name) return;

    this.loading.set(true);
    this._whiteboardService.createRoom(name).subscribe({
      next: (room) => {
        this.loading.set(false);
        this.showCreateModal.set(false);
        this._router.navigate(['/whiteboard', room.id]);
      },
      error: () => this.loading.set(false),
    });
  }

  getCreatorColor(id: number): string {
    return getDevColor(id);
  }

  getCreatorInitial(room: WhiteboardRoom): string {
    return room.createdBy.username?.charAt(0).toUpperCase() ?? '?';
  }

  // Private methods

  private _loadRooms(): void {
    this._whiteboardService.getRooms().subscribe({
      next: (rooms) => this.rooms.set(rooms),
    });
  }
}
