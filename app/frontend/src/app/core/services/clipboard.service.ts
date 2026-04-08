import { Injectable, signal } from '@angular/core';

import { TimeEntry } from '../models/time-entry.model';

@Injectable({ providedIn: 'root' })
export class ClipboardService {

  // Attributes

  readonly copiedEntry = signal<TimeEntry | null>(null);

  // Methods

  copy(entry: TimeEntry): void {
    this.copiedEntry.set(entry);
  }

  clear(): void {
    this.copiedEntry.set(null);
  }
}
