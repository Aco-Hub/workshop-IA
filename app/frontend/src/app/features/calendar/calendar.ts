import {
  Component, OnInit, OnDestroy, HostListener, signal, computed, inject
} from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { CdkDragDrop, CdkDrag, CdkDropList } from '@angular/cdk/drag-drop';

import { AuthService } from '../../core/services/auth.service';
import { DeveloperService } from '../../core/services/developer.service';
import { ProjectService } from '../../core/services/project.service';
import { ClientService } from '../../core/services/client.service';
import { TimeEntryService, TimeEntryRequest, RecurringTimeEntryRequest, RecurrenceFrequency } from '../../core/services/time-entry.service';
import { HolidayService } from '../../core/services/holiday.service';
import { ClipboardService } from '../../core/services/clipboard.service';
import { Developer } from '../../core/models/developer.model';
import { Project } from '../../core/models/project.model';
import { Client } from '../../core/models/client.model';
import { TimeEntry } from '../../core/models/time-entry.model';
import { Holiday } from '../../core/models/holiday.model';
import { TranslatePipe } from '../../shared/pipes/translate.pipe';
import { ModalComponent } from '../../shared/components/modal/modal';
import { LoadingSpinnerComponent } from '../../shared/components/loading-spinner/loading-spinner';
import { I18nService } from '../../core/services/i18n.service';
import { ExportService } from '../../core/services/export.service';
import { CalendarStateService, CalendarView } from '../../core/services/calendar-state.service';
import { getDevColor } from '../../core/utils/dev-colors';

export type { CalendarView };

export interface CalendarDay {
  date: Date;
  isToday: boolean;
  isWeekend: boolean;
  isCurrentMonth: boolean;
  holiday: Holiday | null;
  entries: TimeEntry[];
}

const PROJECT_COLORS = [
  '#2383e2', '#0f7b6c', '#d9730d', '#9065b0',
  '#c94a4a', '#337ea9', '#448361', '#cb912f',
  '#3b5f8c', '#7f5f4a', '#527a52', '#8c6e4a',
];

// Hours options for the entry form (0.5h increments)
export const HOURS_OPTIONS = [0.5, 1, 1.5, 2, 2.5, 3, 3.5, 4, 4.5, 5, 5.5, 6, 6.5, 7, 7.5, 8];

@Component({
  selector: 'app-calendar',
  standalone: true,
  imports: [CommonModule, FormsModule, TranslatePipe, ModalComponent, LoadingSpinnerComponent, CdkDrag, CdkDropList],
  templateUrl: './calendar.html',
  styleUrl: './calendar.scss',
})
export class CalendarComponent implements OnInit, OnDestroy {

  // Attributes

  private readonly _route = inject(ActivatedRoute);
  private readonly _router = inject(Router);
  private readonly _auth = inject(AuthService);
  private readonly _developerService = inject(DeveloperService);
  private readonly _projectService = inject(ProjectService);
  private readonly _clientService = inject(ClientService);
  private readonly _timeEntryService = inject(TimeEntryService);
  private readonly _holidayService = inject(HolidayService);
  private readonly _i18n = inject(I18nService);
  private readonly _exportService = inject(ExportService);
  private readonly _calendarState = inject(CalendarStateService);
  private readonly _clipboard = inject(ClipboardService);

  readonly hoursOptions = HOURS_OPTIONS;

  developer = signal<Developer | null>(null);
  currentUser = this._auth.currentUser;
  isOwnProfile = signal(false);
  editMode = signal(false);
  profileLoading = signal(false);

  editUsername = signal('');
  editTitle = signal('');
  editDiscordLink = signal('');
  profileError = signal('');

  view = this._calendarState.view;
  currentDate = this._calendarState.currentDate;
  entries = signal<TimeEntry[]>([]);
  holidays = signal<Holiday[]>([]);
  projects = signal<Project[]>([]);
  clients = signal<Client[]>([]);
  readonly assignedProjects = computed(() => {
    const dev = this.developer();
    if (!dev) {
      return this.projects();
    }
    return this.projects().filter(p =>
      p.assignedDevelopers.some(d => d.id === dev.id)
    );
  });
  loading = signal(false);
  filterProjectId = this._calendarState.filterProjectId;
  filterClientId = this._calendarState.filterClientId;

  showExportPanel = signal(false);
  exportStartDate = signal('');
  exportEndDate = signal('');

  showEntryModal = signal(false);
  editingEntry = signal<TimeEntry | null>(null);
  entryForm = signal<{
    projectId: number | null;
    type: 'WORK' | 'LEAVE';
    description: string;
    date: string;
    hours: number;
    recurring: boolean;
    frequency: RecurrenceFrequency;
    endDate: string;
    count: number | null;
  }>({ projectId: null, type: 'WORK', description: '', date: '', hours: 1, recurring: false, frequency: 'WEEKLY', endDate: '', count: null });
  entryLoading = signal(false);
  entryError = signal('');
  showDeleteConfirm = signal(false);
  deleteEntryId = signal<number | null>(null);
  showCopiedToast = signal(false);
  readonly copiedEntry = this._clipboard.copiedEntry;
  private _dragging = false;

  readonly weekDays = computed<CalendarDay[]>(() => {
    const date = this.currentDate();
    const startOfWeek = this._getMonday(date);
    const days: CalendarDay[] = [];
    const today = new Date();
    today.setHours(0, 0, 0, 0);

    for (let i = 0; i < 7; i++) {
      const d = new Date(startOfWeek);
      d.setDate(d.getDate() + i);
      days.push({
        date: d,
        isToday: d.getTime() === today.getTime(),
        isWeekend: d.getDay() === 0 || d.getDay() === 6,
        isCurrentMonth: d.getMonth() === this.currentDate().getMonth(),
        holiday: this._getHolidayForDate(d),
        entries: this.getEntriesForDay(d),
      });
    }
    return days;
  });

  readonly monthDays = computed<CalendarDay[]>(() => {
    const date = this.currentDate();
    const year = date.getFullYear();
    const month = date.getMonth();
    const firstDay = new Date(year, month, 1);
    const lastDay = new Date(year, month + 1, 0);
    const today = new Date();
    today.setHours(0, 0, 0, 0);

    const start = this._getMonday(firstDay);
    const days: CalendarDay[] = [];
    const d = new Date(start);
    while (d <= lastDay || days.length % 7 !== 0) {
      const dc = new Date(d);
      dc.setHours(0, 0, 0, 0);
      days.push({
        date: new Date(d),
        isToday: dc.getTime() === today.getTime(),
        isWeekend: d.getDay() === 0 || d.getDay() === 6,
        isCurrentMonth: d.getMonth() === month,
        holiday: this._getHolidayForDate(d),
        entries: this.getEntriesForDay(d),
      });
      d.setDate(d.getDate() + 1);
      if (days.length > 42) {
        break;
      }
    }
    return days;
  });

  readonly dayView = computed<CalendarDay>(() => {
    const d = new Date(this.currentDate());
    d.setHours(0, 0, 0, 0);
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    return {
      date: d,
      isToday: d.getTime() === today.getTime(),
      isWeekend: d.getDay() === 0 || d.getDay() === 6,
      isCurrentMonth: true,
      holiday: this._getHolidayForDate(d),
      entries: this.getEntriesForDay(d),
    };
  });

  readonly totalHours = computed<number>(() => {
    return this.filteredEntries().reduce((sum, e) => sum + this.getEntryDurationHours(e), 0);
  });

  readonly filteredEntries = computed<TimeEntry[]>(() => {
    let entries = this.entries();
    if (this.filterProjectId()) {
      entries = entries.filter(e => e.projectId === this.filterProjectId());
    }
    if (this.filterClientId()) {
      const projectIds = this.projects()
        .filter(p => p.client?.id === this.filterClientId())
        .map(p => p.id);
      entries = entries.filter(e => e.projectId !== null && projectIds.includes(e.projectId!));
    }
    return entries;
  });

  readonly currentPeriodLabel = computed<string>(() => {
    const date = this.currentDate();
    const monthNames = Array.from({ length: 12 }, (_, i) => this._i18n.translate(`month.${i}`));
    if (this.view() === 'day') {
      const dayNames = Array.from({ length: 7 }, (_, i) => this._i18n.translate(`day.${i}`));
      return `${dayNames[date.getDay()]} ${date.getDate()} ${monthNames[date.getMonth()]} ${date.getFullYear()}`;
    }
    if (this.view() === 'week') {
      const monday = this._getMonday(date);
      const sunday = new Date(monday);
      sunday.setDate(sunday.getDate() + 6);
      if (monday.getMonth() === sunday.getMonth()) {
        return `${monday.getDate()} - ${sunday.getDate()} ${monthNames[monday.getMonth()]} ${monday.getFullYear()}`;
      }
      return `${monday.getDate()} ${monthNames[monday.getMonth()]} - ${sunday.getDate()} ${monthNames[sunday.getMonth()]} ${sunday.getFullYear()}`;
    }
    return `${monthNames[date.getMonth()]} ${date.getFullYear()}`;
  });

  private _projectColorMap = new Map<number, string>();
  private _routeSub: ReturnType<typeof setInterval> | null = null;

  // Lifecycle

  ngOnInit(): void {
    this._route.paramMap.subscribe(params => {
      const devId = params.get('developerId');
      const currentUserId = this.currentUser()?.id;
      if (devId) {
        const id = Number(devId);
        this.isOwnProfile.set(id === currentUserId);
        this.loadDeveloper(id);
      } else {
        this.isOwnProfile.set(true);
        if (currentUserId) {
          this.loadDeveloper(currentUserId);
        }
      }
    });
  }

  ngOnDestroy(): void {
    if (this._routeSub) {
      clearInterval(this._routeSub);
    }
  }

  // Methods

  loadDeveloper(id: number): void {
    this.loading.set(true);
    forkJoin({
      developer: this._developerService.getById(id),
      projects: this._projectService.getAll(),
      clients: this._clientService.getAll(),
      holidays: this._holidayService.getHolidays(new Date().getFullYear()),
    }).subscribe({
      next: ({ developer, projects, clients, holidays }) => {
        this.developer.set(developer);
        this.projects.set(projects);
        this.clients.set(clients);
        this.holidays.set(holidays);
        this._buildProjectColorMap(projects);
        this.loading.set(false);
        this.loadEntries();
      },
      error: () => this.loading.set(false),
    });
  }

  loadEntries(): void {
    const dev = this.developer();
    if (!dev) {
      return;
    }
    const { startDate, endDate } = this.getDateRange();
    this._timeEntryService.getAll({
      developerId: dev.id,
      startDate,
      endDate,
    }).subscribe({
      next: entries => this.entries.set(entries),
    });
  }

  getDateRange(): { startDate: string; endDate: string } {
    const date = this.currentDate();
    if (this.view() === 'day') {
      return {
        startDate: this.formatDate(date) + 'T00:00:00',
        endDate: this.formatDate(date) + 'T23:59:59',
      };
    }
    if (this.view() === 'week') {
      const monday = this._getMonday(date);
      const sunday = new Date(monday);
      sunday.setDate(sunday.getDate() + 6);
      return {
        startDate: this.formatDate(monday) + 'T00:00:00',
        endDate: this.formatDate(sunday) + 'T23:59:59',
      };
    }
    const first = new Date(date.getFullYear(), date.getMonth(), 1);
    const last = new Date(date.getFullYear(), date.getMonth() + 1, 0);
    return {
      startDate: this.formatDate(first) + 'T00:00:00',
      endDate: this.formatDate(last) + 'T23:59:59',
    };
  }

  getProjectColor(projectId: number | null): string {
    if (!projectId) {
      return '#9B9A97';
    }
    return this._projectColorMap.get(projectId) || '#9B9A97';
  }

  getEntryColor(entry: TimeEntry): string {
    if (entry.type === 'LEAVE') {
      return '#0f7b6c';
    }
    return this.getProjectColor(entry.projectId);
  }

  getDevColor(devId: number): string {
    return getDevColor(devId);
  }

  setView(view: CalendarView): void {
    this.view.set(view);
    this.loadEntries();
  }

  previous(): void {
    const d = new Date(this.currentDate());
    if (this.view() === 'day') {
      d.setDate(d.getDate() - 1);
    } else if (this.view() === 'week') {
      d.setDate(d.getDate() - 7);
    } else {
      d.setMonth(d.getMonth() - 1);
    }
    this.currentDate.set(d);
    this.loadEntries();
  }

  next(): void {
    const d = new Date(this.currentDate());
    if (this.view() === 'day') {
      d.setDate(d.getDate() + 1);
    } else if (this.view() === 'week') {
      d.setDate(d.getDate() + 7);
    } else {
      d.setMonth(d.getMonth() + 1);
    }
    this.currentDate.set(d);
    this.loadEntries();
  }

  goToday(): void {
    this.currentDate.set(new Date());
    this.loadEntries();
  }

  getEntryDurationHours(entry: TimeEntry): number {
    const start = new Date(entry.startTime).getTime();
    const end = new Date(entry.endTime).getTime();
    return (end - start) / 3600000;
  }

  formatEntryDuration(entry: TimeEntry): string {
    const hours = this.getEntryDurationHours(entry);
    if (hours === Math.floor(hours)) {
      return `${hours}h`;
    }
    return `${hours.toFixed(1)}h`;
  }

  getEntriesForDay(date: Date): TimeEntry[] {
    const dateStr = this.formatDate(date);
    return this.filteredEntries().filter(e => {
      const entryDate = e.startTime.split('T')[0];
      return entryDate === dateStr;
    });
  }

  getDayTotalHours(entries: TimeEntry[]): number {
    return entries.reduce((sum, e) => sum + this.getEntryDurationHours(e), 0);
  }

  openCreateEntryModal(date: string): void {
    this.editingEntry.set(null);
    this.entryForm.set({
      projectId: null,
      type: 'WORK',
      description: '',
      date,
      hours: 1,
      recurring: false,
      frequency: 'WEEKLY',
      endDate: '',
      count: null,
    });
    this.entryError.set('');
    this.showEntryModal.set(true);
  }

  openCreateEntryForDay(day: Date): void {
    this.openCreateEntryModal(this.formatDate(day));
  }

  openEditEntryModal(entry: TimeEntry, event: Event): void {
    event.stopPropagation();
    if (!this.isOwnProfile() || this._dragging) {
      return;
    }
    this.editingEntry.set(entry);
    const date = entry.startTime.split('T')[0];
    const hours = this.getEntryDurationHours(entry);
    this.entryForm.set({
      projectId: entry.projectId,
      type: entry.type,
      description: entry.description,
      date,
      hours,
      recurring: false,
      frequency: 'WEEKLY',
      endDate: '',
      count: null,
    });
    this.entryError.set('');
    this.showEntryModal.set(true);
  }

  saveEntry(): void {
    if (!this.isOwnProfile()) {
      return;
    }
    const form = this.entryForm();
    const dev = this.developer();
    if (!dev) {
      return;
    }

    const { startTime, endTime } = this.computeTimesFromDateAndHours(form.date, form.hours);

    this.entryLoading.set(true);
    this.entryError.set('');

    const editing = this.editingEntry();

    if (!editing && form.recurring) {
      const recurringRequest: RecurringTimeEntryRequest = {
        developerId: dev.id,
        projectId: form.projectId,
        type: form.type,
        description: form.description,
        startTime,
        endTime,
        frequency: form.frequency,
        endDate: form.endDate || null,
        count: form.count,
      };

      this._timeEntryService.createRecurring(recurringRequest).subscribe({
        next: entries => {
          this.entries.update(list => [...list, ...entries]);
          this.entryLoading.set(false);
          this.showEntryModal.set(false);
        },
        error: () => {
          this.entryError.set('time_entry.error');
          this.entryLoading.set(false);
        },
      });
      return;
    }

    const request: TimeEntryRequest = {
      developerId: dev.id,
      projectId: form.projectId,
      type: form.type,
      description: form.description,
      startTime,
      endTime,
    };

    const obs = editing
      ? this._timeEntryService.update(editing.id, request)
      : this._timeEntryService.create(request);

    obs.subscribe({
      next: entry => {
        if (editing) {
          this.entries.update(list => list.map(e => e.id === editing.id ? entry : e));
        } else {
          this.entries.update(list => [...list, entry]);
        }
        this.entryLoading.set(false);
        this.showEntryModal.set(false);
      },
      error: () => {
        this.entryError.set('time_entry.error');
        this.entryLoading.set(false);
      }
    });
  }

  computeTimesFromDateAndHours(date: string, hours: number): { startTime: string; endTime: string } {
    const startTime = new Date(`${date}T09:00:00`).toISOString();
    const endMs = new Date(`${date}T09:00:00`).getTime() + hours * 3600000;
    const endTime = new Date(endMs).toISOString();
    return { startTime, endTime };
  }

  confirmDeleteEntry(entry: TimeEntry, event: Event): void {
    event.stopPropagation();
    if (!this.isOwnProfile()) {
      return;
    }
    this.deleteEntryId.set(entry.id);
    this.showDeleteConfirm.set(true);
  }

  deleteEntry(): void {
    const id = this.deleteEntryId();
    if (!id) {
      return;
    }
    this._timeEntryService.delete(id).subscribe({
      next: () => {
        this.entries.update(list => list.filter(e => e.id !== id));
        this.showDeleteConfirm.set(false);
        this.deleteEntryId.set(null);
        if (this.showEntryModal()) {
          this.showEntryModal.set(false);
        }
      }
    });
  }

  startEdit(): void {
    const dev = this.developer();
    if (!dev) {
      return;
    }
    this.editUsername.set(dev.username);
    this.editTitle.set(dev.title);
    this.editDiscordLink.set(dev.discordLink);
    this.profileError.set('');
    this.editMode.set(true);
  }

  cancelEdit(): void {
    this.editMode.set(false);
  }

  saveProfile(): void {
    this.profileLoading.set(true);
    this.profileError.set('');
    this._auth.updateProfile({
      username: this.editUsername(),
      title: this.editTitle(),
      discordLink: this.editDiscordLink(),
    }).subscribe({
      next: dev => {
        this.developer.set(dev);
        this.editMode.set(false);
        this.profileLoading.set(false);
      },
      error: () => {
        this.profileError.set('profile.update_error');
        this.profileLoading.set(false);
      }
    });
  }

  formatDate(date: Date): string {
    const y = date.getFullYear();
    const m = this._pad(date.getMonth() + 1);
    const d = this._pad(date.getDate());
    return `${y}-${m}-${d}`;
  }

  formatDayLabel(date: Date): string {
    const dayNames = Array.from({ length: 7 }, (_, i) => this._i18n.translate(`day.${i}`));
    return `${dayNames[date.getDay()]} ${date.getDate()}`;
  }

  formatMonthDayEntries(entries: TimeEntry[]): string {
    if (entries.length === 0) {
      return '';
    }
    const totalH = entries.reduce((s, e) => s + this.getEntryDurationHours(e), 0);
    return `${totalH.toFixed(1)}h`;
  }

  getMonthWeekRows(): CalendarDay[][] {
    const days = this.monthDays();
    const rows: CalendarDay[][] = [];
    for (let i = 0; i < days.length; i += 7) {
      rows.push(days.slice(i, i + 7));
    }
    return rows;
  }

  updateEntryFormField(field: string, value: string | number | null): void {
    this.entryForm.update(f => ({ ...f, [field]: value }));
  }

  getDayInitial(date: Date): string {
    return this._i18n.translate(`day.${date.getDay()}`);
  }

  exportReport(format: 'pdf' | 'excel'): void {
    const dev = this.developer();
    if (!dev) {
      return;
    }
    const lang = this._i18n.currentLanguage();
    this._exportService.exportDeveloper(
      dev.id,
      format,
      this.exportStartDate() || undefined,
      this.exportEndDate() || undefined,
      lang
    );
    this.showExportPanel.set(false);
  }

  // Recurrence methods

  deleteRecurrenceGroup(): void {
    const entry = this.editingEntry();
    if (!entry?.recurrenceGroupId) {
      return;
    }
    this._timeEntryService.deleteRecurrenceGroup(entry.recurrenceGroupId).subscribe({
      next: () => {
        const groupId = entry.recurrenceGroupId;
        this.entries.update(list => list.filter(e => e.recurrenceGroupId !== groupId));
        this.showDeleteConfirm.set(false);
        this.deleteEntryId.set(null);
        this.showEntryModal.set(false);
      },
    });
  }

  // Copy-paste methods

  copyEntry(entry: TimeEntry, event: Event): void {
    event.stopPropagation();
    this._clipboard.copy(entry);
    this.showCopiedToast.set(true);
    setTimeout(() => this.showCopiedToast.set(false), 1500);
  }

  pasteEntry(targetDate: Date): void {
    const copied = this._clipboard.copiedEntry();
    if (!copied || !this.isOwnProfile()) {
      return;
    }
    const dev = this.developer();
    if (!dev) {
      return;
    }
    const duration = this.getEntryDurationHours(copied);
    const { startTime, endTime } = this.computeTimesFromDateAndHours(this.formatDate(targetDate), duration);
    const request: TimeEntryRequest = {
      developerId: dev.id,
      projectId: copied.projectId,
      type: copied.type,
      description: copied.description,
      startTime,
      endTime,
    };
    this._timeEntryService.create(request).subscribe({
      next: entry => this.entries.update(list => [...list, entry]),
    });
  }

  @HostListener('document:keydown', ['$event'])
  onKeydown(event: KeyboardEvent): void {
    if (!this.isOwnProfile()) {
      return;
    }
    const ctrl = event.ctrlKey || event.metaKey;
    if (!ctrl) {
      return;
    }
    if (event.key === 'c') {
      const el = document.activeElement as HTMLElement;
      const entryId = el?.getAttribute('data-entry-id');
      if (entryId) {
        const entry = this.entries().find(e => e.id === +entryId);
        if (entry) {
          this._clipboard.copy(entry);
          this.showCopiedToast.set(true);
          setTimeout(() => this.showCopiedToast.set(false), 1500);
        }
      }
    }
    if (event.key === 'v' && this._clipboard.copiedEntry()) {
      const el = document.activeElement as HTMLElement;
      const pasteTarget = el?.getAttribute('data-paste-date');
      if (pasteTarget) {
        event.preventDefault();
        this.pasteEntry(new Date(pasteTarget));
      }
    }
  }

  // Drag-and-drop methods

  readonly weekDropListIds = computed(() =>
    this.weekDays().map((_, i) => `week-drop-${i}`)
  );

  readonly monthDropListIds = computed(() => {
    const days = this.monthDays();
    return days.map((_, i) => `month-drop-${i}`);
  });

  onEntryDragStarted(): void {
    this._dragging = true;
  }

  onEntryDrop(event: CdkDragDrop<CalendarDay>, targetDate: Date): void {
    setTimeout(() => this._dragging = false, 50);
    if (!this.isOwnProfile()) {
      return;
    }
    const entry: TimeEntry = event.item.data;
    if (event.previousContainer === event.container) {
      return;
    }
    const dev = this.developer();
    if (!dev) {
      return;
    }
    const duration = this.getEntryDurationHours(entry);
    const dateStr = this.formatDate(targetDate);
    const { startTime, endTime } = this.computeTimesFromDateAndHours(dateStr, duration);
    const request: TimeEntryRequest = {
      developerId: dev.id,
      projectId: entry.projectId,
      type: entry.type,
      description: entry.description,
      startTime,
      endTime,
    };

    // Optimistic update
    const updatedEntry = { ...entry, startTime, endTime };
    this.entries.update(list => list.map(e => e.id === entry.id ? updatedEntry : e));

    this._timeEntryService.update(entry.id, request).subscribe({
      next: saved => this.entries.update(list => list.map(e => e.id === saved.id ? saved : e)),
      error: () => this.entries.update(list => list.map(e => e.id === entry.id ? entry : e)),
    });
  }

  // Private methods

  private _buildProjectColorMap(projects: Project[]): void {
    this._projectColorMap.clear();
    projects.forEach((p, i) => {
      this._projectColorMap.set(p.id, PROJECT_COLORS[i % PROJECT_COLORS.length]);
    });
  }

  private _getMonday(date: Date): Date {
    const d = new Date(date);
    const day = d.getDay();
    const diff = day === 0 ? -6 : 1 - day;
    d.setDate(d.getDate() + diff);
    d.setHours(0, 0, 0, 0);
    return d;
  }

  private _pad(n: number): string {
    return n.toString().padStart(2, '0');
  }

  private _getHolidayForDate(date: Date): Holiday | null {
    const dateStr = this.formatDate(date);
    return this.holidays().find(h => h.date === dateStr) || null;
  }
}
