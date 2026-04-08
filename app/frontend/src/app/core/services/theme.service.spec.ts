import { beforeAll } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { getTestBed } from '@angular/core/testing';
import {
  BrowserDynamicTestingModule,
  platformBrowserDynamicTesting,
} from '@angular/platform-browser-dynamic/testing';
import { ThemeService } from './theme.service';

beforeAll(() => {
  getTestBed().initTestEnvironment(
    BrowserDynamicTestingModule,
    platformBrowserDynamicTesting()
  );
});

describe('ThemeService', () => {
  let service: ThemeService;

  beforeEach(() => {
    localStorage.clear();
    document.documentElement.removeAttribute('data-theme');
    TestBed.configureTestingModule({});
    service = TestBed.inject(ThemeService);
  });

  afterEach(() => {
    localStorage.clear();
    TestBed.resetTestingModule();
  });

  // Test 16
  it('should default to light theme when nothing stored', () => {
    expect(service.currentTheme()).toBe('light');
  });

  // Test 17
  it('should toggle from light to dark theme', () => {
    service.setTheme('light');
    service.toggleTheme();
    expect(service.currentTheme()).toBe('dark');
  });

  // Test 18
  it('should persist theme to localStorage', () => {
    service.setTheme('dark');
    expect(localStorage.getItem('ts_theme')).toBe('dark');
  });

  // Test 19
  it('should load theme from localStorage on init', () => {
    localStorage.setItem('ts_theme', 'dark');
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({});
    const freshService = TestBed.inject(ThemeService);
    expect(freshService.currentTheme()).toBe('dark');
  });

  // Test 20
  it('should apply data-theme attribute to document on setTheme', () => {
    service.setTheme('dark');
    expect(document.documentElement.getAttribute('data-theme')).toBe('dark');
  });

  // Test 21
  it('should apply data-theme attribute for light theme', () => {
    service.setTheme('light');
    expect(document.documentElement.getAttribute('data-theme')).toBe('light');
  });

  // Test 22
  it('should toggle from dark to light theme', () => {
    service.setTheme('dark');
    service.toggleTheme();
    expect(service.currentTheme()).toBe('light');
  });
});
