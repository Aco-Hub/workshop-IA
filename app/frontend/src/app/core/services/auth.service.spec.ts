import { beforeAll } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { getTestBed } from '@angular/core/testing';
import {
  BrowserDynamicTestingModule,
  platformBrowserDynamicTesting,
} from '@angular/platform-browser-dynamic/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { AuthService, AuthResponse, LoginRequest, RegisterRequest } from './auth.service';

beforeAll(() => {
  getTestBed().initTestEnvironment(
    BrowserDynamicTestingModule,
    platformBrowserDynamicTesting()
  );
});

const mockAuthResponse: AuthResponse = {
  token: 'test-jwt-token',
  id: 1,
  email: 'dev@example.com',
  username: 'devuser',
  title: 'Frontend Developer',
  discordLink: 'https://discord.com/users/123',
  discordAvatarUrl: 'https://cdn.discordapp.com/avatars/123/abc.png',
  role: 'STANDARD',
};

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
    TestBed.resetTestingModule();
  });

  // Test 1
  it('should store token on login', () => {
    const req: LoginRequest = { email: 'dev@example.com', password: 'secret' };
    service.login(req).subscribe();
    const http = httpMock.expectOne('/api/auth/login');
    http.flush(mockAuthResponse);
    expect(localStorage.getItem('ts_jwt')).toBe('test-jwt-token');
  });

  // Test 2
  it('should store user on login', () => {
    const req: LoginRequest = { email: 'dev@example.com', password: 'secret' };
    service.login(req).subscribe();
    const http = httpMock.expectOne('/api/auth/login');
    http.flush(mockAuthResponse);
    const stored = JSON.parse(localStorage.getItem('ts_user')!);
    expect(stored.username).toBe('devuser');
  });

  // Test 3
  it('should clear token on logout', () => {
    localStorage.setItem('ts_jwt', 'test-jwt-token');
    localStorage.setItem('ts_user', JSON.stringify({ id: 1, username: 'devuser' }));
    service.logout();
    expect(localStorage.getItem('ts_jwt')).toBeNull();
  });

  // Test 4
  it('should return null user when not logged in', () => {
    localStorage.clear();
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    const freshService = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
    expect(freshService.currentUser()).toBeNull();
  });

  // Test 5
  it('should load user from localStorage on init', () => {
    const storedUser = { id: 1, username: 'devuser', email: 'dev@example.com', role: 'STANDARD' };
    localStorage.setItem('ts_user', JSON.stringify(storedUser));
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    const freshService = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
    expect(freshService.currentUser()?.username).toBe('devuser');
  });

  // Test 6
  it('should call POST /api/auth/login', () => {
    const req: LoginRequest = { email: 'dev@example.com', password: 'secret' };
    service.login(req).subscribe();
    const http = httpMock.expectOne('/api/auth/login');
    expect(http.request.method).toBe('POST');
    http.flush(mockAuthResponse);
  });

  // Test 7
  it('should call POST /api/auth/register', () => {
    const req: RegisterRequest = { token: 'invite-abc', username: 'newdev', password: 'pass123' };
    service.register(req).subscribe();
    const http = httpMock.expectOne('/api/auth/register');
    expect(http.request.method).toBe('POST');
    http.flush(mockAuthResponse);
  });

  // Test 8
  it('should call GET /api/auth/me', () => {
    service.getMe().subscribe();
    const http = httpMock.expectOne('/api/auth/me');
    expect(http.request.method).toBe('GET');
    http.flush({ id: 1, username: 'devuser', email: 'dev@example.com', role: 'STANDARD' });
  });

  // Test 9
  it('should call PUT /api/auth/profile', () => {
    service.updateProfile({ username: 'updated', title: 'Senior Dev', discordLink: '' }).subscribe();
    const http = httpMock.expectOne('/api/auth/profile');
    expect(http.request.method).toBe('PUT');
    http.flush({ id: 1, username: 'updated' });
  });

  // Test 10
  it('should add token to localStorage after login', () => {
    const req: LoginRequest = { email: 'dev@example.com', password: 'secret' };
    service.login(req).subscribe();
    const http = httpMock.expectOne('/api/auth/login');
    http.flush(mockAuthResponse);
    expect(localStorage.getItem('ts_jwt')).toBeTruthy();
  });

  // Test 11
  it('should set isAuthenticated to true after login', () => {
    const req: LoginRequest = { email: 'dev@example.com', password: 'secret' };
    service.login(req).subscribe();
    const http = httpMock.expectOne('/api/auth/login');
    http.flush(mockAuthResponse);
    expect(service.isAuthenticated()).toBe(true);
  });

  // Test 12
  it('should clear currentUser signal on logout', () => {
    localStorage.setItem('ts_jwt', 'test-jwt-token');
    service.logout();
    expect(service.currentUser()).toBeNull();
  });
});
