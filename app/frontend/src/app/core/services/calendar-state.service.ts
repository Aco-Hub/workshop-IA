import { Injectable, signal } from '@angular/core';

export type CalendarView = 'day' | 'week' | 'month';

@Injectable({ providedIn: 'root' })
export class CalendarStateService {

  // Attributes

  readonly view = signal<CalendarView>('week');
  readonly currentDate = signal<Date>(new Date());
  readonly filterProjectId = signal<number | null>(null);
  readonly filterClientId = signal<number | null>(null);
}
