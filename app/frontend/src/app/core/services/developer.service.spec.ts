import { beforeAll } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { getTestBed } from '@angular/core/testing';
import {
  BrowserDynamicTestingModule,
  platformBrowserDynamicTesting,
} from '@angular/platform-browser-dynamic/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { DeveloperService } from './developer.service';
import { Developer } from '../models/developer.model';

beforeAll(() => {
  getTestBed().initTestEnvironment(
    BrowserDynamicTestingModule,
    platformBrowserDynamicTesting()
  );
});

const mockDeveloper: Developer = {
  id: 1,
  email: 'dev@example.com',
  username: 'devuser',
  title: 'Frontend Developer',
  discordLink: '',
  discordAvatarUrl: '',
  role: 'STANDARD',
  createdAt: '2025-01-01T00:00:00Z',
  updatedAt: '2025-01-01T00:00:00Z',
};

describe('DeveloperService', () => {
  let service: DeveloperService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(DeveloperService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    TestBed.resetTestingModule();
  });

  // Test 21
  it('should fetch all developers via GET /api/developers', () => {
    let result: Developer[] | undefined;
    service.getAll().subscribe(devs => (result = devs));
    const http = httpMock.expectOne('/api/developers');
    expect(http.request.method).toBe('GET');
    http.flush([mockDeveloper]);
    expect(result).toHaveLength(1);
    expect(result![0].username).toBe('devuser');
  });

  // Test 22
  it('should fetch developer by ID via GET /api/developers/:id', () => {
    let result: Developer | undefined;
    service.getById(1).subscribe(dev => (result = dev));
    const http = httpMock.expectOne('/api/developers/1');
    expect(http.request.method).toBe('GET');
    http.flush(mockDeveloper);
    expect(result?.id).toBe(1);
  });

  // Test 23
  it('should invite developer via POST /api/developers/invite', () => {
    let result: { inviteLink: string; message: string } | undefined;
    service.invite({ email: 'newdev@example.com' }).subscribe(r => (result = r));
    const http = httpMock.expectOne('/api/developers/invite');
    expect(http.request.method).toBe('POST');
    expect(http.request.body).toEqual({ email: 'newdev@example.com' });
    http.flush({ inviteLink: 'http://app/register?token=abc', message: 'Invitation sent' });
    expect(result?.inviteLink).toContain('token=abc');
  });

  // Test 24
  it('should delete developer via DELETE /api/developers/:id', () => {
    let completed = false;
    service.delete(1).subscribe(() => (completed = true));
    const http = httpMock.expectOne('/api/developers/1');
    expect(http.request.method).toBe('DELETE');
    http.flush(null);
    expect(completed).toBe(true);
  });
});
