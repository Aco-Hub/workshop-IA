import { beforeAll } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { getTestBed } from '@angular/core/testing';
import {
  BrowserDynamicTestingModule,
  platformBrowserDynamicTesting,
} from '@angular/platform-browser-dynamic/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ClientService } from './client.service';

beforeAll(() => {
  getTestBed().initTestEnvironment(
    BrowserDynamicTestingModule,
    platformBrowserDynamicTesting()
  );
});

interface Client {
  id: number;
  name: string;
}

const mockClient: Client = { id: 1, name: 'Acme Corp' };

describe('ClientService', () => {
  let service: ClientService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(ClientService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    TestBed.resetTestingModule();
  });

  // Test 31
  it('should fetch all clients via GET /api/clients', () => {
    let result: Client[] | undefined;
    service.getAll().subscribe(clients => (result = clients as Client[]));
    const http = httpMock.expectOne('/api/clients');
    expect(http.request.method).toBe('GET');
    http.flush([mockClient]);
    expect(result).toHaveLength(1);
    expect(result![0].name).toBe('Acme Corp');
  });

  // Test 32
  it('should create client via POST /api/clients', () => {
    let result: Client | undefined;
    service.create({ name: 'New Client' }).subscribe(c => (result = c as Client));
    const http = httpMock.expectOne('/api/clients');
    expect(http.request.method).toBe('POST');
    expect(http.request.body).toEqual({ name: 'New Client' });
    http.flush({ id: 2, name: 'New Client' });
    expect(result?.name).toBe('New Client');
  });

  // Test 33
  it('should update client via PUT /api/clients/:id', () => {
    let result: Client | undefined;
    service.update(1, { name: 'Updated Corp' }).subscribe(c => (result = c as Client));
    const http = httpMock.expectOne('/api/clients/1');
    expect(http.request.method).toBe('PUT');
    http.flush({ id: 1, name: 'Updated Corp' });
    expect(result?.name).toBe('Updated Corp');
  });

  // Test 34
  it('should delete client via DELETE /api/clients/:id', () => {
    let completed = false;
    service.delete(1).subscribe(() => (completed = true));
    const http = httpMock.expectOne('/api/clients/1');
    expect(http.request.method).toBe('DELETE');
    http.flush(null);
    expect(completed).toBe(true);
  });

  // Test 35
  it('should fetch client by ID via GET /api/clients/:id', () => {
    let result: Client | undefined;
    service.getById(1).subscribe(c => (result = c as Client));
    const http = httpMock.expectOne('/api/clients/1');
    expect(http.request.method).toBe('GET');
    http.flush(mockClient);
    expect(result?.id).toBe(1);
  });
});
