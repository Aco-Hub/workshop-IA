import { beforeAll, describe, it, expect, beforeEach } from 'vitest';
import { TestBed, getTestBed } from '@angular/core/testing';
import {
  BrowserDynamicTestingModule,
  platformBrowserDynamicTesting,
} from '@angular/platform-browser-dynamic/testing';
import { CalendarStateService, CalendarView } from './calendar-state.service';

beforeAll(() => {
  getTestBed().initTestEnvironment(
    BrowserDynamicTestingModule,
    platformBrowserDynamicTesting()
  );
});

describe('CalendarStateService - Calendar State Preservation', () => {
  let service: CalendarStateService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(CalendarStateService);
  });

  afterEach(() => {
    // Reset state for next test
    service.view.set('week');
    service.currentDate.set(new Date());
    service.filterProjectId.set(null);
    service.filterClientId.set(null);
    TestBed.resetTestingModule();
  });

  // User story: As a developer, I want my calendar view (day/week/month) to persist when I navigate away and come back
  it('should preserve calendar view selection across component instances', () => {
    service.view.set('month');
    expect(service.view()).toBe('month');

    // Simulate navigating away and back — the service is a singleton
    const sameService = TestBed.inject(CalendarStateService);
    expect(sameService.view()).toBe('month');
  });

  // User story: As a developer, I want the current date I was viewing to still be there when I return to my calendar
  it('should preserve the current date when navigating away', () => {
    const targetDate = new Date(2026, 5, 15);
    service.currentDate.set(targetDate);

    const sameService = TestBed.inject(CalendarStateService);
    expect(sameService.currentDate().getTime()).toBe(targetDate.getTime());
  });

  // User story: As a developer, I want my project filter to persist across navigation
  it('should preserve project filter across navigation', () => {
    service.filterProjectId.set(42);
    const sameService = TestBed.inject(CalendarStateService);
    expect(sameService.filterProjectId()).toBe(42);
  });

  // User story: As a developer, I want my client filter to persist across navigation
  it('should preserve client filter across navigation', () => {
    service.filterClientId.set(7);
    const sameService = TestBed.inject(CalendarStateService);
    expect(sameService.filterClientId()).toBe(7);
  });

  // User story: defaults should be sensible
  it('should default to week view', () => {
    expect(service.view()).toBe('week');
  });

  it('should default to today for current date', () => {
    const today = new Date();
    expect(service.currentDate().toDateString()).toBe(today.toDateString());
  });

  it('should default to no project filter', () => {
    expect(service.filterProjectId()).toBeNull();
  });

  it('should default to no client filter', () => {
    expect(service.filterClientId()).toBeNull();
  });

  // User story: I can change view to day
  it('should allow setting view to day', () => {
    service.view.set('day');
    expect(service.view()).toBe('day');
  });

  // User story: I can change view to week
  it('should allow setting view to week', () => {
    service.view.set('day');
    service.view.set('week');
    expect(service.view()).toBe('week');
  });

  // User story: Multiple state changes should all persist
  it('should preserve all state simultaneously', () => {
    const date = new Date(2026, 0, 1);
    service.view.set('day');
    service.currentDate.set(date);
    service.filterProjectId.set(10);
    service.filterClientId.set(3);

    expect(service.view()).toBe('day');
    expect(service.currentDate().getTime()).toBe(date.getTime());
    expect(service.filterProjectId()).toBe(10);
    expect(service.filterClientId()).toBe(3);
  });

  // User story: Clearing filters should work
  it('should allow clearing filters', () => {
    service.filterProjectId.set(5);
    service.filterClientId.set(2);
    service.filterProjectId.set(null);
    service.filterClientId.set(null);
    expect(service.filterProjectId()).toBeNull();
    expect(service.filterClientId()).toBeNull();
  });
});
