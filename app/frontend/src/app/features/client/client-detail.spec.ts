import { beforeAll, describe, it, expect, beforeEach, afterEach } from 'vitest';
import { TestBed, getTestBed } from '@angular/core/testing';
import {
  BrowserDynamicTestingModule,
  platformBrowserDynamicTesting,
} from '@angular/platform-browser-dynamic/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { ClientDetailComponent } from './client-detail';
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
  id: 1, email: 'admin@test.com', username: 'Admin', title: 'Lead',
  discordLink: '', discordAvatarUrl: '', role: 'ADMIN' as const,
  createdAt: '2025-01-01T00:00:00', updatedAt: '2025-01-01T00:00:00',
};

const mockDev1 = {
  id: 1, email: 'albin@test.com', username: 'Albin', title: 'Dev',
  discordLink: '', discordAvatarUrl: '', role: 'STANDARD' as const,
  createdAt: '2025-01-01T00:00:00', updatedAt: '2025-01-01T00:00:00',
};

const mockDev2 = {
  id: 2, email: 'totof@test.com', username: 'totof', title: 'Dev',
  discordLink: '', discordAvatarUrl: '', role: 'STANDARD' as const,
  createdAt: '2025-01-01T00:00:00', updatedAt: '2025-01-01T00:00:00',
};

const mockClient = { id: 10, name: 'Firmenish', createdAt: '2025-01-01', updatedAt: '2025-01-01' };

const mockProject = {
  id: 5, name: 'Firm test', type: 'EXTERNAL' as const,
  repoUrl: '', client: { id: 10, name: 'Firmenish', createdAt: '', updatedAt: '' },
  assignedDevelopers: [mockDev1, mockDev2],
  createdAt: '2025-01-01', updatedAt: '2025-01-01',
};

const mkEntry = (id: number, devId: number, devName: string, start: string, end: string, desc = '') => ({
  id, developerId: devId, developerUsername: devName, projectId: 5,
  projectName: 'Firm test', type: 'WORK' as const, description: desc,
  startTime: start, endTime: end, createdAt: '', updatedAt: '',
});

function createComponent() {
  TestBed.configureTestingModule({
    imports: [ClientDetailComponent],
    providers: [
      provideHttpClient(),
      provideHttpClientTesting(),
      provideRouter([]),
      { provide: AuthService, useValue: { currentUser: signal(mockUser) } },
      { provide: ActivatedRoute, useValue: { snapshot: { paramMap: { get: () => '10' } } } },
    ],
  });
  const fixture = TestBed.createComponent(ClientDetailComponent);
  const comp = fixture.componentInstance;
  const httpMock = TestBed.inject(HttpTestingController);
  return { fixture, comp, httpMock };
}

/**
 * US-001: All assigned developers should appear in "Hours per developer"
 *
 * As a project manager viewing a client detail page,
 * I want to see ALL developers assigned to the client's projects
 * in the "hours per developer" section, even those with 0 hours,
 * so that I know who is available but hasn't logged time yet.
 */
describe('US-001: All assigned devs should appear in hours breakdown, even with 0h', () => {
  let httpMock: HttpTestingController;

  afterEach(() => { httpMock.verify(); TestBed.resetTestingModule(); });

  it('should list all assigned devs including those with zero hours', () => {
    const { fixture, comp, httpMock: hm } = createComponent();
    httpMock = hm;

    comp.client.set(mockClient);
    comp.projects.set([mockProject]);
    // Only dev1 has entries, dev2 has none
    comp.entries.set([
      mkEntry(1, 1, 'Albin', '2026-03-20T09:00:00', '2026-03-20T10:30:00'),
    ]);

    const dh = comp.devHoursMap();
    expect(dh.length).toBe(2);
    const dev2hours = dh.find(d => d.developer.id === 2);
    expect(dev2hours).toBeDefined();
    expect(dev2hours!.hours).toBe(0);
    fixture.destroy();
  });
});

/**
 * US-002: Total hours in stats bar must match sum of all dev hours
 *
 * As a user viewing the client detail,
 * I want the total hours in the stats bar to equal the sum of
 * individual developer hours, so the numbers are always consistent.
 */
describe('US-002: Stats bar total must equal sum of individual dev hours', () => {
  let httpMock: HttpTestingController;

  afterEach(() => { httpMock.verify(); TestBed.resetTestingModule(); });

  it('totalHours should equal the sum of all devHoursMap entries', () => {
    const { fixture, comp, httpMock: hm } = createComponent();
    httpMock = hm;

    comp.client.set(mockClient);
    comp.projects.set([mockProject]);
    comp.entries.set([
      mkEntry(1, 1, 'Albin', '2026-03-20T09:00:00', '2026-03-20T11:00:00'),
      mkEntry(2, 2, 'totof', '2026-03-20T14:00:00', '2026-03-20T16:30:00'),
    ]);

    const total = comp.totalHours();
    const devSum = comp.devHoursMap().reduce((s, d) => s + d.hours, 0);
    expect(total).toBeCloseTo(devSum);
    // Albin: 2h, totof: 2.5h => 4.5h
    expect(total).toBeCloseTo(4.5);
    fixture.destroy();
  });
});

/**
 * US-003: Developer count in stats bar must match heatmap and hours section
 *
 * As a user viewing the client detail,
 * I want the "Developers" count to match the number of rows
 * in both the heatmap and the hours-per-developer section.
 */
describe('US-003: Developer count must be consistent across all sections', () => {
  let httpMock: HttpTestingController;

  afterEach(() => { httpMock.verify(); TestBed.resetTestingModule(); });

  it('uniqueDevs count should match devHoursMap length', () => {
    const { fixture, comp, httpMock: hm } = createComponent();
    httpMock = hm;

    comp.client.set(mockClient);
    comp.projects.set([mockProject]);
    comp.entries.set([
      mkEntry(1, 1, 'Albin', '2026-03-20T09:00:00', '2026-03-20T10:00:00'),
    ]);

    expect(comp.uniqueDevs().length).toBe(2);
    expect(comp.devHoursMap().length).toBe(2);
    fixture.destroy();
  });

  it('should include devs from entries who are not currently assigned', () => {
    const { fixture, comp, httpMock: hm } = createComponent();
    httpMock = hm;

    comp.client.set(mockClient);
    // Project only has dev1 assigned
    comp.projects.set([{ ...mockProject, assignedDevelopers: [mockDev1] }]);
    // But dev2 (id=2) logged time in the past
    comp.entries.set([
      mkEntry(1, 1, 'Albin', '2026-03-20T09:00:00', '2026-03-20T10:00:00'),
      mkEntry(2, 2, 'totof', '2026-03-19T09:00:00', '2026-03-19T11:00:00'),
    ]);

    expect(comp.uniqueDevs().length).toBe(2);
    expect(comp.devHoursMap().length).toBe(2);
    const totof = comp.devHoursMap().find(d => d.developer.id === 2);
    expect(totof).toBeDefined();
    expect(totof!.hours).toBeCloseTo(2);
    fixture.destroy();
  });
});

/**
 * US-005: Swimlane must display actual time entry blocks, not placeholders
 *
 * As a user switching to the swimlane view on a client page,
 * I want to see positioned time entry blocks for each developer,
 * not just empty dashes.
 */
describe('US-005: Swimlane view must show positioned entry blocks', () => {
  let httpMock: HttpTestingController;

  afterEach(() => { httpMock.verify(); TestBed.resetTestingModule(); });

  it('swimlaneData should produce blocks with left/width for entries', () => {
    const { fixture, comp, httpMock: hm } = createComponent();
    httpMock = hm;

    comp.client.set(mockClient);
    comp.projects.set([mockProject]);
    comp.entries.set([
      mkEntry(1, 1, 'Albin', '2026-03-20T09:00:00', '2026-03-20T11:00:00', 'Code review'),
      mkEntry(2, 2, 'totof', '2026-03-20T10:00:00', '2026-03-20T12:00:00', 'Feature dev'),
    ]);

    const data = comp.swimlaneData();
    expect(data.length).toBe(1);
    expect(data[0].project.name).toBe('Firm test');
    expect(data[0].rows.length).toBe(2);

    const albinRow = data[0].rows.find(r => r.developer.id === 1);
    expect(albinRow).toBeDefined();
    expect(albinRow!.blocks.length).toBe(1);
    expect(albinRow!.blocks[0].left).toBeGreaterThanOrEqual(0);
    expect(albinRow!.blocks[0].width).toBeGreaterThan(0);
    expect(albinRow!.blocks[0].dateLabel).toBe('20/03');
    expect(albinRow!.blocks[0].hours).toBeCloseTo(2);

    const totofRow = data[0].rows.find(r => r.developer.id === 2);
    expect(totofRow!.blocks.length).toBe(1);
    expect(totofRow!.blocks[0].dateLabel).toBe('20/03');
    expect(totofRow!.blocks[0].hours).toBeCloseTo(2);
    fixture.destroy();
  });

  it('swimlaneData should show empty blocks for devs without entries', () => {
    const { fixture, comp, httpMock: hm } = createComponent();
    httpMock = hm;

    comp.client.set(mockClient);
    comp.projects.set([mockProject]);
    comp.entries.set([
      mkEntry(1, 1, 'Albin', '2026-03-20T09:00:00', '2026-03-20T11:00:00'),
    ]);

    const data = comp.swimlaneData();
    const totofRow = data[0].rows.find(r => r.developer.id === 2);
    expect(totofRow).toBeDefined();
    expect(totofRow!.blocks.length).toBe(0);
    fixture.destroy();
  });
});

/**
 * US-008: Loading entries should fetch ALL entries without date restriction
 *
 * As a user viewing a client page,
 * I want all historical entries to be loaded so totals match
 * the project view and no data is missing.
 */
describe('US-008: loadEntries should fetch all entries without date restriction', () => {
  let httpMock: HttpTestingController;

  afterEach(() => { httpMock.verify(); TestBed.resetTestingModule(); });

  it('should request entries without startDate/endDate params', () => {
    const { fixture, comp, httpMock: hm } = createComponent();
    httpMock = hm;

    comp.client.set(mockClient);
    comp.projects.set([mockProject]);
    comp.loadEntries();

    const req = hm.expectOne(r => r.url === '/api/time-entries' && r.params.get('projectId') === '5');
    expect(req.request.params.has('startDate')).toBe(false);
    expect(req.request.params.has('endDate')).toBe(false);
    req.flush([]);
    fixture.destroy();
  });
});

/**
 * US-010: Swimlane must group entries by project and show all devs per project
 *
 * As a user viewing the swimlane on a client with multiple projects,
 * I want each project to appear as a separate section with its own
 * developer rows and entry blocks grouped correctly.
 */
describe('US-010: Swimlane must group entries correctly by project', () => {
  let httpMock: HttpTestingController;

  afterEach(() => { httpMock.verify(); TestBed.resetTestingModule(); });

  it('should produce one swimlane section per client project', () => {
    const { fixture, comp, httpMock: hm } = createComponent();
    httpMock = hm;

    const project2 = {
      ...mockProject, id: 6, name: 'Project B',
      assignedDevelopers: [mockDev1],
    };
    comp.client.set(mockClient);
    comp.projects.set([mockProject, project2]);
    comp.entries.set([
      mkEntry(1, 1, 'Albin', '2026-03-20T09:00:00', '2026-03-20T11:00:00'),
      { ...mkEntry(2, 1, 'Albin', '2026-03-20T14:00:00', '2026-03-20T16:00:00'), projectId: 6, projectName: 'Project B' },
    ]);

    const data = comp.swimlaneData();
    expect(data.length).toBe(2);
    expect(data[0].project.name).toBe('Firm test');
    expect(data[1].project.name).toBe('Project B');

    // Firm test: both devs, only Albin has a block
    expect(data[0].rows.length).toBe(2);
    const albinFirm = data[0].rows.find(r => r.developer.id === 1);
    expect(albinFirm!.blocks.length).toBe(1);
    const totofFirm = data[0].rows.find(r => r.developer.id === 2);
    expect(totofFirm!.blocks.length).toBe(0);

    // Project B: only Albin assigned, 1 block
    expect(data[1].rows.length).toBe(1);
    expect(data[1].rows[0].blocks.length).toBe(1);
    fixture.destroy();
  });
});

/**
 * US-011: Client hours must correctly aggregate across multiple projects and devs
 *
 * As a project manager viewing a client detail page,
 * I want to see accurate total and per-developer hours when Albin worked 11h
 * and totof also worked for the client, so I can track billing correctly.
 */
describe('US-011: Client hours must display correctly for multiple devs (Albin 11h, totof)', () => {
  let httpMock: HttpTestingController;

  afterEach(() => { httpMock.verify(); TestBed.resetTestingModule(); });

  const project2 = {
    ...mockProject, id: 6, name: 'Project B',
    assignedDevelopers: [mockDev1, mockDev2],
  };

  // Albin: 3h + 2.5h + 3h + 2.5h = 11h across two projects
  // totof: 4h + 3.5h = 7.5h across two projects
  const albinEntries = [
    mkEntry(1, 1, 'Albin', '2026-03-16T09:00:00', '2026-03-16T12:00:00', 'Backend dev'),
    mkEntry(2, 1, 'Albin', '2026-03-17T14:00:00', '2026-03-17T16:30:00', 'Code review'),
    mkEntry(3, 1, 'Albin', '2026-03-18T09:00:00', '2026-03-18T12:00:00', 'API work'),
    { ...mkEntry(4, 1, 'Albin', '2026-03-19T10:00:00', '2026-03-19T12:30:00', 'Testing'), projectId: 6, projectName: 'Project B' },
  ];
  const totofEntries = [
    mkEntry(5, 2, 'totof', '2026-03-16T10:00:00', '2026-03-16T14:00:00', 'Frontend dev'),
    { ...mkEntry(6, 2, 'totof', '2026-03-18T09:00:00', '2026-03-18T12:30:00', 'UI fixes'), projectId: 6, projectName: 'Project B' },
  ];

  it('should show Albin at 11h and totof at 7.5h with correct total', () => {
    const { fixture, comp, httpMock: hm } = createComponent();
    httpMock = hm;

    comp.client.set(mockClient);
    comp.projects.set([mockProject, project2]);
    comp.entries.set([...albinEntries, ...totofEntries]);

    // Total hours: 11 + 7.5 = 18.5
    expect(comp.totalHours()).toBeCloseTo(18.5);

    const dh = comp.devHoursMap();
    expect(dh.length).toBe(2);

    // Sorted by hours desc: Albin first
    const albin = dh.find(d => d.developer.id === 1);
    expect(albin).toBeDefined();
    expect(albin!.hours).toBeCloseTo(11);

    const totof = dh.find(d => d.developer.id === 2);
    expect(totof).toBeDefined();
    expect(totof!.hours).toBeCloseTo(7.5);

    // Sum of individual hours must equal totalHours
    const devSum = dh.reduce((s, d) => s + d.hours, 0);
    expect(devSum).toBeCloseTo(comp.totalHours());

    fixture.destroy();
  });

  it('should show correct bar widths proportional to total hours', () => {
    const { fixture, comp, httpMock: hm } = createComponent();
    httpMock = hm;

    comp.client.set(mockClient);
    comp.projects.set([mockProject, project2]);
    comp.entries.set([...albinEntries, ...totofEntries]);

    const total = comp.totalHours();
    const dh = comp.devHoursMap();

    const albin = dh.find(d => d.developer.id === 1)!;
    const totof = dh.find(d => d.developer.id === 2)!;

    // Bar widths as percentages
    const albinPct = (albin.hours / total) * 100;
    const totofPct = (totof.hours / total) * 100;

    expect(albinPct).toBeCloseTo(59.46, 1); // 11/18.5 * 100
    expect(totofPct).toBeCloseTo(40.54, 1); // 7.5/18.5 * 100
    expect(albinPct + totofPct).toBeCloseTo(100);

    fixture.destroy();
  });

  it('should count exactly 2 unique developers', () => {
    const { fixture, comp, httpMock: hm } = createComponent();
    httpMock = hm;

    comp.client.set(mockClient);
    comp.projects.set([mockProject, project2]);
    comp.entries.set([...albinEntries, ...totofEntries]);

    expect(comp.uniqueDevs().length).toBe(2);
    expect(comp.clientProjects().length).toBe(2);

    fixture.destroy();
  });

  it('should still show dev hours when dev has entries but is not assigned to any project', () => {
    const { fixture, comp, httpMock: hm } = createComponent();
    httpMock = hm;

    // Only mockDev1 (Albin) is assigned, but totof has entries
    const projectOnlyAlbin = {
      ...mockProject,
      assignedDevelopers: [mockDev1],
    };

    comp.client.set(mockClient);
    comp.projects.set([projectOnlyAlbin]);
    comp.entries.set([...albinEntries.filter(e => e.projectId === 5), ...totofEntries.filter(e => e.projectId === 5)]);

    const dh = comp.devHoursMap();
    // totof must still appear even though not assigned
    const totof = dh.find(d => d.developer.id === 2);
    expect(totof).toBeDefined();
    expect(totof!.hours).toBeCloseTo(4); // Only project 5 entry: 4h

    const albin = dh.find(d => d.developer.id === 1);
    expect(albin).toBeDefined();
    expect(albin!.hours).toBeCloseTo(8.5); // 3h + 2.5h + 3h from project 5

    fixture.destroy();
  });

});
