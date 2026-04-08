import { beforeAll } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { getTestBed } from '@angular/core/testing';
import {
  BrowserDynamicTestingModule,
  platformBrowserDynamicTesting,
} from '@angular/platform-browser-dynamic/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ProjectService, ProjectRequest } from './project.service';
import { Project } from '../models/project.model';

beforeAll(() => {
  getTestBed().initTestEnvironment(
    BrowserDynamicTestingModule,
    platformBrowserDynamicTesting()
  );
});

const mockProject: Project = {
  id: 1,
  name: 'Test Project',
  type: 'INTERNAL',
  repoUrl: 'https://github.com/org/repo',
  client: null,
  assignedDevelopers: [],
  createdAt: '2025-01-01T00:00:00Z',
  updatedAt: '2025-01-01T00:00:00Z',
};

const mockRequest: ProjectRequest = {
  name: 'Test Project',
  type: 'INTERNAL',
  repoUrl: 'https://github.com/org/repo',
  clientId: null,
};

describe('ProjectService', () => {
  let service: ProjectService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(ProjectService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    TestBed.resetTestingModule();
  });

  // Test 25
  it('should fetch all projects via GET /api/projects', () => {
    let result: Project[] | undefined;
    service.getAll().subscribe(projects => (result = projects));
    const http = httpMock.expectOne('/api/projects');
    expect(http.request.method).toBe('GET');
    http.flush([mockProject]);
    expect(result).toHaveLength(1);
    expect(result![0].name).toBe('Test Project');
  });

  // Test 26
  it('should create project with creatorId as query param', () => {
    let result: Project | undefined;
    service.create(mockRequest, 42).subscribe(p => (result = p));
    const http = httpMock.expectOne('/api/projects?creatorId=42');
    expect(http.request.method).toBe('POST');
    expect(http.request.body).toEqual(mockRequest);
    http.flush(mockProject);
    expect(result?.name).toBe('Test Project');
  });

  // Test 27
  it('should assign developer to project via POST /api/projects/:id/assign with body', () => {
    let result: Project | undefined;
    service.assign(1, 5).subscribe(p => (result = p));
    const http = httpMock.expectOne('/api/projects/1/assign');
    expect(http.request.method).toBe('POST');
    expect(http.request.body).toEqual({ developerId: 5 });
    http.flush(mockProject);
    expect(result?.id).toBe(1);
  });

  // Test 28
  it('should unassign developer from project via POST /api/projects/:id/unassign with body', () => {
    let result: Project | undefined;
    service.unassign(1, 5).subscribe(p => (result = p));
    const http = httpMock.expectOne('/api/projects/1/unassign');
    expect(http.request.method).toBe('POST');
    expect(http.request.body).toEqual({ developerId: 5 });
    http.flush(mockProject);
    expect(result?.id).toBe(1);
  });

  // Test 29
  it('should delete project via DELETE /api/projects/:id', () => {
    let completed = false;
    service.delete(1).subscribe(() => (completed = true));
    const http = httpMock.expectOne('/api/projects/1');
    expect(http.request.method).toBe('DELETE');
    http.flush(null);
    expect(completed).toBe(true);
  });

  // Test 30
  it('should update project via PUT /api/projects/:id', () => {
    let result: Project | undefined;
    service.update(1, mockRequest).subscribe(p => (result = p));
    const http = httpMock.expectOne('/api/projects/1');
    expect(http.request.method).toBe('PUT');
    http.flush({ ...mockProject, name: 'Updated Project' });
    expect(result?.name).toBe('Updated Project');
  });
});
