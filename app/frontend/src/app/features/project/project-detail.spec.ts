import { beforeAll, describe, it, expect, beforeEach, afterEach } from 'vitest';
import { TestBed, getTestBed } from '@angular/core/testing';
import {
  BrowserDynamicTestingModule,
  platformBrowserDynamicTesting,
} from '@angular/platform-browser-dynamic/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { ProjectDetailComponent } from './project-detail';
import { AuthService } from '../../core/services/auth.service';
import { signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

beforeAll(() => {
  getTestBed().initTestEnvironment(
    BrowserDynamicTestingModule,
    platformBrowserDynamicTesting()
  );
});

const mockUser = {
  id: 1, email: 'test@test.com', username: 'TestDev', title: 'Dev',
  discordLink: '', discordAvatarUrl: '', role: 'ADMIN' as const,
  createdAt: '2025-01-01T00:00:00', updatedAt: '2025-01-01T00:00:00',
};

const mockProject = {
  id: 5, name: 'Test Project', type: 'INTERNAL',
  repoUrl: 'https://github.com/test', client: null,
  assignedDevelopers: [],
  createdAt: '2025-01-01T00:00:00', updatedAt: '2025-01-01T00:00:00',
};

const mockProjectWithDev = {
  ...mockProject,
  assignedDevelopers: [mockUser],
};

describe('ProjectDetailComponent - Join, Leave, Entries', () => {
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [ProjectDetailComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        {
          provide: AuthService,
          useValue: { currentUser: signal(mockUser) },
        },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: { get: () => '5' } } },
        },
      ],
    });
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    TestBed.resetTestingModule();
  });

  it('joinProject should send developerId in request body, not query param', () => {
    const fixture = TestBed.createComponent(ProjectDetailComponent);
    const comp = fixture.componentInstance;
    comp.project.set(mockProject);

    comp.joinProject();

    const req = httpMock.expectOne(r =>
      r.url === '/api/projects/5/assign' && r.method === 'POST'
    );
    // THE KEY ASSERTION: developerId must be in body, not query params
    expect(req.request.body).toEqual({ developerId: 1 });
    expect(req.request.urlWithParams).not.toContain('developerId=');
    req.flush(mockProjectWithDev);

    expect(comp.project()!.assignedDevelopers.length).toBe(1);
    fixture.destroy();
  });

  it('leaveProject should send developerId in request body', () => {
    const fixture = TestBed.createComponent(ProjectDetailComponent);
    const comp = fixture.componentInstance;
    comp.project.set(mockProjectWithDev);

    comp.leaveProject();

    const req = httpMock.expectOne(r =>
      r.url === '/api/projects/5/unassign' && r.method === 'POST'
    );
    expect(req.request.body).toEqual({ developerId: 1 });
    req.flush(mockProject);
    expect(comp.project()!.assignedDevelopers.length).toBe(0);
    fixture.destroy();
  });

  it('isAssigned should return true when current user is in assignedDevelopers', () => {
    const fixture = TestBed.createComponent(ProjectDetailComponent);
    const comp = fixture.componentInstance;
    comp.project.set(mockProjectWithDev);
    expect(comp.isAssigned()).toBe(true);
    fixture.destroy();
  });

  it('isAssigned should return false when current user is not in assignedDevelopers', () => {
    const fixture = TestBed.createComponent(ProjectDetailComponent);
    const comp = fixture.componentInstance;
    comp.project.set(mockProject);
    expect(comp.isAssigned()).toBe(false);
    fixture.destroy();
  });

  it('loadEntries should fetch all entries for the project without date range restriction', () => {
    const fixture = TestBed.createComponent(ProjectDetailComponent);
    const comp = fixture.componentInstance;
    comp.project.set(mockProject);

    comp.loadEntries();

    const req = httpMock.expectOne(r => r.url === '/api/time-entries');
    expect(req.request.params.get('projectId')).toBe('5');
    // Should NOT restrict by date range to get accurate totals
    expect(req.request.params.has('startDate')).toBe(false);
    expect(req.request.params.has('endDate')).toBe(false);
    req.flush([]);
    fixture.destroy();
  });

  it('totalHours should correctly sum entry durations', () => {
    const fixture = TestBed.createComponent(ProjectDetailComponent);
    const comp = fixture.componentInstance;
    comp.entries.set([
      { id: 1, developerId: 1, developerUsername: 'A', projectId: 5, projectName: 'P',
        type: 'WORK', description: '', startTime: '2026-03-13T09:00:00',
        endTime: '2026-03-13T11:00:00', createdAt: '', updatedAt: '' },
      { id: 2, developerId: 1, developerUsername: 'A', projectId: 5, projectName: 'P',
        type: 'WORK', description: '', startTime: '2026-03-13T14:00:00',
        endTime: '2026-03-13T15:30:00', createdAt: '', updatedAt: '' },
    ]);
    // 2h + 1.5h = 3.5h
    expect(comp.totalHours()).toBeCloseTo(3.5);
    fixture.destroy();
  });

  it('devHours should compute per-developer hour breakdown', () => {
    const fixture = TestBed.createComponent(ProjectDetailComponent);
    const comp = fixture.componentInstance;
    comp.project.set(mockProjectWithDev);
    comp.entries.set([
      { id: 1, developerId: 1, developerUsername: 'TestDev', projectId: 5, projectName: 'P',
        type: 'WORK', description: '', startTime: '2026-03-13T09:00:00',
        endTime: '2026-03-13T12:00:00', createdAt: '', updatedAt: '' },
    ]);
    const dh = comp.devHours();
    expect(dh.length).toBe(1);
    expect(dh[0].developer.username).toBe('TestDev');
    expect(dh[0].hours).toBeCloseTo(3);
    fixture.destroy();
  });
});

/**
 * US-001: Accurate developer time totals on project view
 *
 * As a developer viewing a project detail page,
 * I want the total hours and per-developer hours to reflect the full project
 * history (or the explicitly selected date range),
 * so that I can trust the reported time data for planning and billing.
 *
 * Bug: loadEntries() scopes the API call to getSwimlaneRange() (current
 * zoom window), so totalHours and devHours only sum entries within that
 * visible range — making it look like developers worked far fewer hours
 * than they actually have.
 *
 * Fix: the profile KPIs (totalHours, devHours) must query ALL entries for
 * the project independently of the swimlane zoom range.
 */
describe('US-001: Project view should show accurate total dev times', () => {
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [ProjectDetailComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        {
          provide: AuthService,
          useValue: { currentUser: signal(mockUser) },
        },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: { get: () => '5' } } },
        },
      ],
    });
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    TestBed.resetTestingModule();
  });

  it('loadEntries should fetch ALL project entries for KPI totals, not just the current zoom range', () => {
    const fixture = TestBed.createComponent(ProjectDetailComponent);
    const comp = fixture.componentInstance;
    comp.project.set(mockProject);

    comp.loadEntries();

    // There should be a request that fetches entries without date range restrictions
    // (or with the full project date range) for the KPI section
    const allReqs = httpMock.match(r => r.url === '/api/time-entries');
    const hasUnboundedRequest = allReqs.some(r =>
      !r.request.params.has('startDate') && !r.request.params.has('endDate')
    );
    const hasBroadRangeRequest = allReqs.some(r => {
      const start = r.request.params.get('startDate');
      const end = r.request.params.get('endDate');
      if (!start || !end) return false;
      const rangeMs = new Date(end).getTime() - new Date(start).getTime();
      const oneYearMs = 365 * 24 * 3600000;
      return rangeMs >= oneYearMs;
    });

    expect(
      hasUnboundedRequest || hasBroadRangeRequest
    ).toBe(true);

    allReqs.forEach(r => r.flush([]));
    fixture.destroy();
  });

  it('totalHours should include entries outside the current swimlane zoom range', () => {
    const fixture = TestBed.createComponent(ProjectDetailComponent);
    const comp = fixture.componentInstance;
    comp.project.set(mockProjectWithDev);

    // Entry from 3 months ago — outside any weekly zoom range centered on today
    const oldEntry = {
      id: 10, developerId: 1, developerUsername: 'TestDev', projectId: 5,
      projectName: 'P', type: 'WORK' as const, description: 'Old work',
      startTime: '2025-12-01T09:00:00', endTime: '2025-12-01T17:00:00',
      createdAt: '', updatedAt: '',
    };
    // Entry from today
    const recentEntry = {
      id: 11, developerId: 1, developerUsername: 'TestDev', projectId: 5,
      projectName: 'P', type: 'WORK' as const, description: 'Recent work',
      startTime: '2026-03-23T09:00:00', endTime: '2026-03-23T12:00:00',
      createdAt: '', updatedAt: '',
    };

    comp.entries.set([oldEntry, recentEntry]);

    // totalHours must include BOTH entries: 8h + 3h = 11h
    expect(comp.totalHours()).toBeCloseTo(11);
    fixture.destroy();
  });

  it('devHours should aggregate across the full project history, not just the visible range', () => {
    const fixture = TestBed.createComponent(ProjectDetailComponent);
    const comp = fixture.componentInstance;
    comp.project.set(mockProjectWithDev);

    comp.entries.set([
      { id: 20, developerId: 1, developerUsername: 'TestDev', projectId: 5,
        projectName: 'P', type: 'WORK', description: '',
        startTime: '2025-06-01T09:00:00', endTime: '2025-06-01T17:00:00',
        createdAt: '', updatedAt: '' },
      { id: 21, developerId: 1, developerUsername: 'TestDev', projectId: 5,
        projectName: 'P', type: 'WORK', description: '',
        startTime: '2026-03-20T10:00:00', endTime: '2026-03-20T12:00:00',
        createdAt: '', updatedAt: '' },
    ]);

    const dh = comp.devHours();
    expect(dh.length).toBe(1);
    // 8h + 2h = 10h total for TestDev
    expect(dh[0].hours).toBeCloseTo(10);
    fixture.destroy();
  });
});
