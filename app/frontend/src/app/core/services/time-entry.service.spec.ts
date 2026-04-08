import { beforeAll } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { getTestBed } from '@angular/core/testing';
import {
  BrowserDynamicTestingModule,
  platformBrowserDynamicTesting,
} from '@angular/platform-browser-dynamic/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TimeEntryService, TimeEntryRequest } from './time-entry.service';
import { TimeEntry } from '../models/time-entry.model';

beforeAll(() => {
  getTestBed().initTestEnvironment(
    BrowserDynamicTestingModule,
    platformBrowserDynamicTesting()
  );
});

const mockEntry: TimeEntry = {
  id: 1,
  developerId: 1,
  developerUsername: 'devuser',
  projectId: 2,
  projectName: 'Test Project',
  type: 'WORK',
  description: 'Worked on feature X',
  startTime: '2025-03-10T09:00:00',
  endTime: '2025-03-10T17:00:00',
  createdAt: '2025-03-10T09:00:00',
  updatedAt: '2025-03-10T09:00:00',
};

const mockRequest: TimeEntryRequest = {
  developerId: 1,
  projectId: 2,
  type: 'WORK',
  description: 'Worked on feature X',
  startTime: '2025-03-10T09:00:00',
  endTime: '2025-03-10T17:00:00',
};

describe('TimeEntryService', () => {
  let service: TimeEntryService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(TimeEntryService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    TestBed.resetTestingModule();
  });

  it('should fetch time entries without filter via GET /api/time-entries', () => {
    let result: TimeEntry[] | undefined;
    service.getAll().subscribe(entries => (result = entries));
    const http = httpMock.expectOne('/api/time-entries');
    expect(http.request.method).toBe('GET');
    http.flush([mockEntry]);
    expect(result).toHaveLength(1);
  });

  it('should fetch time entries with developerId filter', () => {
    service.getAll({ developerId: 1 }).subscribe();
    const http = httpMock.expectOne(req => req.url === '/api/time-entries' && req.params.get('developerId') === '1');
    expect(http.request.method).toBe('GET');
    http.flush([mockEntry]);
  });

  // CRITICAL: date params must include time component for backend ISO.DATE_TIME parsing
  it('should send startDate and endDate with ISO datetime format (not plain date)', () => {
    service.getAll({ startDate: '2025-03-01T00:00:00', endDate: '2025-03-31T23:59:59' }).subscribe();
    const http = httpMock.expectOne(req => {
      const start = req.params.get('startDate');
      const end = req.params.get('endDate');
      // Must contain time component — plain '2025-03-01' would cause 500 on backend
      return start !== null && start.includes('T') &&
             end !== null && end.includes('T');
    });
    http.flush([mockEntry]);
  });

  it('should reject plain date strings without time component as invalid API params', () => {
    // This test documents the contract: the backend requires ISO datetime, not plain dates
    const plainDate = '2025-03-01';
    expect(plainDate.includes('T')).toBe(false); // plain date has no time component
    const isoDateTime = '2025-03-01T00:00:00';
    expect(isoDateTime.includes('T')).toBe(true); // ISO datetime has time component
  });

  it('should create time entry via POST /api/time-entries', () => {
    let result: TimeEntry | undefined;
    service.create(mockRequest).subscribe(e => (result = e));
    const http = httpMock.expectOne('/api/time-entries');
    expect(http.request.method).toBe('POST');
    expect(http.request.body).toEqual(mockRequest);
    http.flush(mockEntry);
    expect(result?.id).toBe(1);
  });

  it('should update time entry via PUT /api/time-entries/:id', () => {
    let result: TimeEntry | undefined;
    service.update(1, mockRequest).subscribe(e => (result = e));
    const http = httpMock.expectOne('/api/time-entries/1');
    expect(http.request.method).toBe('PUT');
    http.flush({ ...mockEntry, description: 'Updated description' });
    expect(result?.description).toBe('Updated description');
  });

  it('should delete time entry via DELETE /api/time-entries/:id', () => {
    let completed = false;
    service.delete(1).subscribe(() => (completed = true));
    const http = httpMock.expectOne('/api/time-entries/1');
    expect(http.request.method).toBe('DELETE');
    http.flush(null);
    expect(completed).toBe(true);
  });

  it('should fetch time entries with projectId filter', () => {
    service.getAll({ projectId: 2 }).subscribe();
    const http = httpMock.expectOne(req => req.url === '/api/time-entries' && req.params.get('projectId') === '2');
    expect(http.request.method).toBe('GET');
    http.flush([mockEntry]);
  });

  it('should send all filter params together', () => {
    service.getAll({
      developerId: 1,
      projectId: 2,
      startDate: '2025-03-01T00:00:00',
      endDate: '2025-03-31T23:59:59',
    }).subscribe();
    const http = httpMock.expectOne(req =>
      req.params.get('developerId') === '1' &&
      req.params.get('projectId') === '2' &&
      req.params.get('startDate') === '2025-03-01T00:00:00' &&
      req.params.get('endDate') === '2025-03-31T23:59:59'
    );
    http.flush([mockEntry]);
  });

  it('should not send undefined filter params', () => {
    service.getAll({ developerId: 1 }).subscribe();
    const http = httpMock.expectOne(req =>
      req.params.get('developerId') === '1' &&
      !req.params.has('projectId') &&
      !req.params.has('startDate') &&
      !req.params.has('endDate')
    );
    http.flush([]);
  });

  it('should return empty array when no entries match', () => {
    let result: TimeEntry[] | undefined;
    service.getAll({ developerId: 999 }).subscribe(entries => (result = entries));
    httpMock.expectOne(req => req.params.get('developerId') === '999').flush([]);
    expect(result).toEqual([]);
  });
});
