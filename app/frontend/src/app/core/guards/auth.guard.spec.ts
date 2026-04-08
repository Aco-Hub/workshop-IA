import { beforeAll } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { getTestBed } from '@angular/core/testing';
import {
  BrowserDynamicTestingModule,
  platformBrowserDynamicTesting,
} from '@angular/platform-browser-dynamic/testing';
import { Router, UrlTree } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { authGuard } from './auth.guard';
import { AuthService } from '../services/auth.service';

beforeAll(() => {
  getTestBed().initTestEnvironment(
    BrowserDynamicTestingModule,
    platformBrowserDynamicTesting()
  );
});

describe('authGuard', () => {
  let router: Router;

  function runGuard(): boolean | UrlTree {
    return TestBed.runInInjectionContext(() => authGuard({} as never, {} as never));
  }

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
      ],
    });
    router = TestBed.inject(Router);
  });

  afterEach(() => {
    localStorage.clear();
    TestBed.resetTestingModule();
  });

  // Test 39
  it('should allow access (return true) when token is in localStorage', () => {
    localStorage.setItem('ts_jwt', 'valid-token');
    const result = runGuard();
    expect(result).toBe(true);
  });

  // Test 40
  it('should redirect to /login when no token in localStorage', () => {
    const result = runGuard();
    expect(result).toBeInstanceOf(UrlTree);
    const urlTree = result as UrlTree;
    expect(urlTree.toString()).toBe('/login');
  });

  // Test 41
  it('should check token via AuthService.getToken()', () => {
    localStorage.setItem('ts_jwt', 'my-jwt-token');
    const authService = TestBed.inject(AuthService);
    const token = authService.getToken();
    expect(token).toBe('my-jwt-token');
    const result = runGuard();
    expect(result).toBe(true);
  });

  // Test 42
  it('should return UrlTree (not true) when not authenticated', () => {
    localStorage.removeItem('ts_jwt');
    const result = runGuard();
    expect(result).not.toBe(true);
  });
});
