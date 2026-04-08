import { beforeAll, describe, it, expect, beforeEach, afterEach } from 'vitest';
import { TestBed, getTestBed } from '@angular/core/testing';
import {
  BrowserDynamicTestingModule,
  platformBrowserDynamicTesting,
} from '@angular/platform-browser-dynamic/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { CalendarComponent } from './calendar';
import { CalendarStateService } from '../../core/services/calendar-state.service';
import { AuthService } from '../../core/services/auth.service';
import { signal } from '@angular/core';

beforeAll(() => {
  getTestBed().initTestEnvironment(
    BrowserDynamicTestingModule,
    platformBrowserDynamicTesting()
  );
});

const mockDeveloper = {
  id: 1, email: 'test@test.com', username: 'TestDev', title: 'Dev',
  discordLink: '', discordAvatarUrl: '', role: 'STANDARD' as const,
  createdAt: '2025-01-01T00:00:00', updatedAt: '2025-01-01T00:00:00',
};

const mockEntry = {
  id: 1, developerId: 1, developerUsername: 'TestDev',
  projectId: 1, projectName: 'Project A', type: 'WORK',
  description: 'Did work', startTime: '2026-03-13T09:00:00',
  endTime: '2026-03-13T11:00:00', createdAt: '2026-03-13T09:00:00',
  updatedAt: '2026-03-13T09:00:00',
};

describe('CalendarComponent - Data Persistence & Loading', () => {
  let httpMock: HttpTestingController;
  let calendarState: CalendarStateService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [CalendarComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([
          { path: 'calendar', component: CalendarComponent },
          { path: 'calendar/:developerId', component: CalendarComponent },
        ]),
        {
          provide: AuthService,
          useValue: {
            currentUser: signal(mockDeveloper),
            updateProfile: () => {},
          },
        },
      ],
    });
    httpMock = TestBed.inject(HttpTestingController);
    calendarState = TestBed.inject(CalendarStateService);
  });

  afterEach(() => {
    httpMock.verify();
    calendarState.view.set('week');
    calendarState.currentDate.set(new Date());
    calendarState.filterProjectId.set(null);
    calendarState.filterClientId.set(null);
    TestBed.resetTestingModule();
  });

  // Core contract: getDateRange must produce ISO datetime strings
  it('getDateRange should return ISO datetime strings with T separator for week view', () => {
    const fixture = TestBed.createComponent(CalendarComponent);
    const comp = fixture.componentInstance;
    calendarState.view.set('week');
    calendarState.currentDate.set(new Date(2026, 2, 13)); // March 13, 2026

    const range = comp.getDateRange();
    expect(range.startDate).toContain('T');
    expect(range.endDate).toContain('T');
    expect(range.startDate).toMatch(/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}$/);
    expect(range.endDate).toMatch(/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}$/);
    fixture.destroy();
  });

  it('getDateRange should return ISO datetime strings for day view', () => {
    const fixture = TestBed.createComponent(CalendarComponent);
    const comp = fixture.componentInstance;
    calendarState.view.set('day');
    calendarState.currentDate.set(new Date(2026, 2, 13));

    const range = comp.getDateRange();
    expect(range.startDate).toBe('2026-03-13T00:00:00');
    expect(range.endDate).toBe('2026-03-13T23:59:59');
    fixture.destroy();
  });

  it('getDateRange should return ISO datetime strings for month view', () => {
    const fixture = TestBed.createComponent(CalendarComponent);
    const comp = fixture.componentInstance;
    calendarState.view.set('month');
    calendarState.currentDate.set(new Date(2026, 2, 13)); // March 2026

    const range = comp.getDateRange();
    expect(range.startDate).toBe('2026-03-01T00:00:00');
    expect(range.endDate).toBe('2026-03-31T23:59:59');
    fixture.destroy();
  });

  it('getDateRange week view should span monday to sunday', () => {
    const fixture = TestBed.createComponent(CalendarComponent);
    const comp = fixture.componentInstance;
    calendarState.view.set('week');
    // March 13, 2026 is a Friday — week should be Mon Mar 9 to Sun Mar 15
    calendarState.currentDate.set(new Date(2026, 2, 13));

    const range = comp.getDateRange();
    expect(range.startDate).toContain('2026-03-09');
    expect(range.endDate).toContain('2026-03-15');
    fixture.destroy();
  });

  // loadEntries sends correct HTTP params (would have caught the 500 bug)
  it('loadEntries should send ISO datetime params that the backend can parse', () => {
    const fixture = TestBed.createComponent(CalendarComponent);
    const comp = fixture.componentInstance;
    calendarState.view.set('week');
    calendarState.currentDate.set(new Date(2026, 2, 13));

    comp.developer.set(mockDeveloper);
    comp.loadEntries();

    const req = httpMock.expectOne(r =>
      r.url === '/api/time-entries' &&
      r.params.has('startDate') &&
      r.params.has('endDate')
    );

    // THE KEY ASSERTION: params must have T separator, not plain date
    const startDate = req.request.params.get('startDate')!;
    const endDate = req.request.params.get('endDate')!;
    expect(startDate).toContain('T');
    expect(endDate).toContain('T');
    expect(startDate).toMatch(/T00:00:00$/);
    expect(endDate).toMatch(/T23:59:59$/);

    req.flush([mockEntry]);
    expect(comp.entries().length).toBe(1);
    fixture.destroy();
  });

  // After loadEntries, entries should be populated
  it('should populate entries signal after successful API response', () => {
    const fixture = TestBed.createComponent(CalendarComponent);
    const comp = fixture.componentInstance;
    calendarState.view.set('week');
    calendarState.currentDate.set(new Date(2026, 2, 13));

    comp.developer.set(mockDeveloper);
    comp.loadEntries();

    const req = httpMock.expectOne(r => r.url === '/api/time-entries');
    req.flush([mockEntry, { ...mockEntry, id: 2, description: 'Second entry' }]);

    expect(comp.entries().length).toBe(2);
    fixture.destroy();
  });

  // Calendar state persists (singleton service)
  it('should use CalendarStateService view, not local state', () => {
    calendarState.view.set('month');
    const fixture = TestBed.createComponent(CalendarComponent);
    const comp = fixture.componentInstance;
    expect(comp.view()).toBe('month');
    fixture.destroy();
  });

  it('should use CalendarStateService currentDate, not local state', () => {
    const date = new Date(2026, 5, 15);
    calendarState.currentDate.set(date);
    const fixture = TestBed.createComponent(CalendarComponent);
    const comp = fixture.componentInstance;
    expect(comp.currentDate().getTime()).toBe(date.getTime());
    fixture.destroy();
  });

  it('setView should update CalendarStateService and reload entries', () => {
    const fixture = TestBed.createComponent(CalendarComponent);
    const comp = fixture.componentInstance;
    comp.developer.set(mockDeveloper);

    comp.setView('day');

    expect(calendarState.view()).toBe('day');
    // Should have triggered loadEntries
    const req = httpMock.expectOne(r => r.url === '/api/time-entries');
    req.flush([]);
    fixture.destroy();
  });

  it('previous() should update date and reload entries', () => {
    const fixture = TestBed.createComponent(CalendarComponent);
    const comp = fixture.componentInstance;
    calendarState.view.set('week');
    calendarState.currentDate.set(new Date(2026, 2, 13));
    comp.developer.set(mockDeveloper);

    comp.previous();

    // Should have moved back 7 days
    expect(calendarState.currentDate().getDate()).toBe(6);
    const req = httpMock.expectOne(r => r.url === '/api/time-entries');
    req.flush([]);
    fixture.destroy();
  });

  it('next() should update date and reload entries', () => {
    const fixture = TestBed.createComponent(CalendarComponent);
    const comp = fixture.componentInstance;
    calendarState.view.set('week');
    calendarState.currentDate.set(new Date(2026, 2, 13));
    comp.developer.set(mockDeveloper);

    comp.next();

    expect(calendarState.currentDate().getDate()).toBe(20);
    const req = httpMock.expectOne(r => r.url === '/api/time-entries');
    req.flush([]);
    fixture.destroy();
  });

  // Entry duration calculations
  it('should calculate entry duration in hours correctly', () => {
    const fixture = TestBed.createComponent(CalendarComponent);
    const comp = fixture.componentInstance;
    // mockEntry: 09:00 to 11:00 = 2 hours
    expect(comp.getEntryDurationHours(mockEntry)).toBe(2);
    fixture.destroy();
  });

  it('should calculate total hours from filtered entries', () => {
    const fixture = TestBed.createComponent(CalendarComponent);
    const comp = fixture.componentInstance;
    comp.entries.set([
      mockEntry,
      { ...mockEntry, id: 2, startTime: '2026-03-13T13:00:00', endTime: '2026-03-13T14:30:00' },
    ]);
    // 2h + 1.5h = 3.5h
    expect(comp.totalHours()).toBe(3.5);
    fixture.destroy();
  });

  // Filter persistence
  it('should filter entries by project when filterProjectId is set', () => {
    const fixture = TestBed.createComponent(CalendarComponent);
    const comp = fixture.componentInstance;
    comp.entries.set([
      mockEntry,
      { ...mockEntry, id: 2, projectId: 99, projectName: 'Other' },
    ]);
    calendarState.filterProjectId.set(1);
    expect(comp.filteredEntries().length).toBe(1);
    expect(comp.filteredEntries()[0].projectId).toBe(1);
    fixture.destroy();
  });

  it('should show all entries when no filter is set', () => {
    const fixture = TestBed.createComponent(CalendarComponent);
    const comp = fixture.componentInstance;
    comp.entries.set([
      mockEntry,
      { ...mockEntry, id: 2, projectId: 99 },
    ]);
    calendarState.filterProjectId.set(null);
    calendarState.filterClientId.set(null);
    expect(comp.filteredEntries().length).toBe(2);
    fixture.destroy();
  });

  // Entry creation flow
  it('openCreateEntryModal should set form with date and default hours', () => {
    const fixture = TestBed.createComponent(CalendarComponent);
    const comp = fixture.componentInstance;
    comp.openCreateEntryModal('2026-03-13');
    expect(comp.showEntryModal()).toBe(true);
    expect(comp.editingEntry()).toBeNull();
    expect(comp.entryForm().date).toBe('2026-03-13');
    expect(comp.entryForm().hours).toBe(1);
    expect(comp.entryForm().type).toBe('WORK');
    fixture.destroy();
  });

  it('openEditEntryModal should populate form with existing entry data', () => {
    const fixture = TestBed.createComponent(CalendarComponent);
    const comp = fixture.componentInstance;
    comp.isOwnProfile.set(true);
    const event = new Event('click');
    comp.openEditEntryModal(mockEntry, event);
    expect(comp.showEntryModal()).toBe(true);
    expect(comp.editingEntry()).toBe(mockEntry);
    expect(comp.entryForm().projectId).toBe(1);
    expect(comp.entryForm().description).toBe('Did work');
    fixture.destroy();
  });

  it('saveEntry should POST new entry and add to entries signal', () => {
    const fixture = TestBed.createComponent(CalendarComponent);
    const comp = fixture.componentInstance;
    comp.isOwnProfile.set(true);
    comp.developer.set(mockDeveloper);
    comp.openCreateEntryModal('2026-03-13');
    comp.entryForm.update(f => ({ ...f, projectId: 1, description: 'New work', hours: 1.5 }));

    comp.saveEntry();

    const req = httpMock.expectOne(r => r.url === '/api/time-entries' && r.method === 'POST');
    expect(req.request.body.developerId).toBe(1);
    expect(req.request.body.description).toBe('New work');
    req.flush({ ...mockEntry, id: 99, description: 'New work' });

    expect(comp.entries().some(e => e.id === 99)).toBe(true);
    expect(comp.showEntryModal()).toBe(false);
    fixture.destroy();
  });

  it('saveEntry should PUT existing entry and update entries signal', () => {
    const fixture = TestBed.createComponent(CalendarComponent);
    const comp = fixture.componentInstance;
    comp.isOwnProfile.set(true);
    comp.developer.set(mockDeveloper);
    comp.entries.set([mockEntry]);
    const event = new Event('click');
    comp.openEditEntryModal(mockEntry, event);
    comp.entryForm.update(f => ({ ...f, description: 'Updated' }));

    comp.saveEntry();

    const req = httpMock.expectOne(r => r.url === '/api/time-entries/1' && r.method === 'PUT');
    req.flush({ ...mockEntry, description: 'Updated' });

    expect(comp.entries()[0].description).toBe('Updated');
    fixture.destroy();
  });

  it('deleteEntry should remove entry from entries signal', () => {
    const fixture = TestBed.createComponent(CalendarComponent);
    const comp = fixture.componentInstance;
    comp.entries.set([mockEntry, { ...mockEntry, id: 2 }]);
    comp.deleteEntryId.set(1);
    comp.showDeleteConfirm.set(true);

    comp.deleteEntry();

    const req = httpMock.expectOne(r => r.url === '/api/time-entries/1' && r.method === 'DELETE');
    req.flush(null);

    expect(comp.entries().length).toBe(1);
    expect(comp.entries()[0].id).toBe(2);
    expect(comp.showDeleteConfirm()).toBe(false);
    fixture.destroy();
  });

  // Entry display helpers
  it('getEntriesForDay should match entries by date string', () => {
    const fixture = TestBed.createComponent(CalendarComponent);
    const comp = fixture.componentInstance;
    comp.entries.set([
      mockEntry, // 2026-03-13
      { ...mockEntry, id: 2, startTime: '2026-03-14T10:00:00', endTime: '2026-03-14T11:00:00' },
    ]);
    const march13 = new Date(2026, 2, 13);
    const march14 = new Date(2026, 2, 14);
    expect(comp.getEntriesForDay(march13).length).toBe(1);
    expect(comp.getEntriesForDay(march14).length).toBe(1);
    expect(comp.getEntriesForDay(new Date(2026, 2, 15)).length).toBe(0);
    fixture.destroy();
  });

  it('formatEntryDuration should show human-readable duration', () => {
    const fixture = TestBed.createComponent(CalendarComponent);
    const comp = fixture.componentInstance;
    expect(comp.formatEntryDuration(mockEntry)).toBe('2h');
    // 0.5h entry
    const shortEntry = { ...mockEntry, startTime: '2026-03-13T09:00:00', endTime: '2026-03-13T09:30:00' };
    expect(comp.formatEntryDuration(shortEntry)).toBe('0.5h');
    // 1.5h entry
    const medEntry = { ...mockEntry, startTime: '2026-03-13T09:00:00', endTime: '2026-03-13T10:30:00' };
    expect(comp.formatEntryDuration(medEntry)).toBe('1.5h');
    fixture.destroy();
  });
});
