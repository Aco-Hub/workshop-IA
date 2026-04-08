import { beforeAll } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { getTestBed } from '@angular/core/testing';
import {
  BrowserDynamicTestingModule,
  platformBrowserDynamicTesting,
} from '@angular/platform-browser-dynamic/testing';
import { I18nService } from './i18n.service';

beforeAll(() => {
  getTestBed().initTestEnvironment(
    BrowserDynamicTestingModule,
    platformBrowserDynamicTesting()
  );
});

describe('I18nService', () => {
  let service: I18nService;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({});
    service = TestBed.inject(I18nService);
  });

  afterEach(() => {
    localStorage.clear();
    TestBed.resetTestingModule();
  });

  // Test 11
  it('should default to French when no language stored', () => {
    expect(service.currentLanguage()).toBe('fr');
  });

  // Test 12
  it('should translate known key in French', () => {
    service.setLanguage('fr');
    expect(service.translate('auth.email')).toBe('Adresse e-mail');
  });

  // Test 13
  it('should switch to English', () => {
    service.setLanguage('en');
    expect(service.currentLanguage()).toBe('en');
  });

  // Test 14
  it('should translate known key in English', () => {
    service.setLanguage('en');
    expect(service.translate('auth.email')).toBe('Email address');
  });

  // Test 15
  it('should return key when translation not found', () => {
    service.setLanguage('fr');
    expect(service.translate('nonexistent.key')).toBe('nonexistent.key');
  });

  // Test 16
  it('should persist language to localStorage', () => {
    service.setLanguage('en');
    expect(localStorage.getItem('ts_language')).toBe('en');
  });

  // Test 17
  it('should load language from localStorage on init', () => {
    localStorage.setItem('ts_language', 'en');
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({});
    const freshService = TestBed.inject(I18nService);
    expect(freshService.currentLanguage()).toBe('en');
  });

  // Test 18
  it('should translate common.loading key in French', () => {
    service.setLanguage('fr');
    expect(service.translate('common.loading')).toBe('Chargement...');
  });

  // Test 19
  it('should translate common.loading key in English', () => {
    service.setLanguage('en');
    expect(service.translate('common.loading')).toBe('Loading...');
  });
});
