import { beforeAll } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { getTestBed } from '@angular/core/testing';
import {
  BrowserDynamicTestingModule,
  platformBrowserDynamicTesting,
} from '@angular/platform-browser-dynamic/testing';
import { TranslatePipe } from './translate.pipe';
import { I18nService } from '../../core/services/i18n.service';

beforeAll(() => {
  getTestBed().initTestEnvironment(
    BrowserDynamicTestingModule,
    platformBrowserDynamicTesting()
  );
});

describe('TranslatePipe', () => {
  let pipe: TranslatePipe;
  let i18n: I18nService;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [I18nService],
    });
    i18n = TestBed.inject(I18nService);
    pipe = TestBed.runInInjectionContext(() => new TranslatePipe());
  });

  afterEach(() => {
    localStorage.clear();
    TestBed.resetTestingModule();
  });

  // Test 42
  it('should translate key using current language (French by default)', () => {
    i18n.setLanguage('fr');
    const result = pipe.transform('auth.email');
    expect(result).toBe('Adresse e-mail');
  });

  // Test 43
  it('should return the key itself when translation is not found', () => {
    const result = pipe.transform('this.key.does.not.exist');
    expect(result).toBe('this.key.does.not.exist');
  });

  // Test 44
  it('should translate in English after language switch', () => {
    i18n.setLanguage('en');
    const result = pipe.transform('auth.email');
    expect(result).toBe('Email address');
  });

  // Test 45
  it('should translate common.save in French', () => {
    i18n.setLanguage('fr');
    expect(pipe.transform('common.save')).toBe('Enregistrer');
  });

  // Test 46
  it('should translate common.save in English', () => {
    i18n.setLanguage('en');
    expect(pipe.transform('common.save')).toBe('Save');
  });
});
