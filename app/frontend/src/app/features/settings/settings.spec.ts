import { beforeAll } from 'vitest';
import { TestBed, ComponentFixture } from '@angular/core/testing';
import { getTestBed } from '@angular/core/testing';
import {
  BrowserDynamicTestingModule,
  platformBrowserDynamicTesting,
} from '@angular/platform-browser-dynamic/testing';
import { SettingsComponent } from './settings';
import { I18nService } from '../../core/services/i18n.service';
import { ThemeService } from '../../core/services/theme.service';

beforeAll(() => {
  getTestBed().initTestEnvironment(
    BrowserDynamicTestingModule,
    platformBrowserDynamicTesting()
  );
});

describe('SettingsComponent', () => {
  let fixture: ComponentFixture<SettingsComponent>;
  let component: SettingsComponent;
  let i18nService: I18nService;
  let themeService: ThemeService;

  beforeEach(async () => {
    localStorage.clear();
    await TestBed.configureTestingModule({
      imports: [SettingsComponent],
      providers: [I18nService, ThemeService],
    }).compileComponents();

    i18nService = TestBed.inject(I18nService);
    themeService = TestBed.inject(ThemeService);
    fixture = TestBed.createComponent(SettingsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => {
    localStorage.clear();
    TestBed.resetTestingModule();
  });

  // Test 53
  it('should render French language option', () => {
    const frOption = fixture.nativeElement.querySelector('[data-testid="lang-fr-option"]');
    expect(frOption).toBeTruthy();
  });

  // Test 54
  it('should render English language option', () => {
    const enOption = fixture.nativeElement.querySelector('[data-testid="lang-en-option"]');
    expect(enOption).toBeTruthy();
  });

  // Test 55
  it('should render light theme option', () => {
    const lightOption = fixture.nativeElement.querySelector('[data-testid="theme-light-option"]');
    expect(lightOption).toBeTruthy();
  });

  // Test 56
  it('should render dark theme option', () => {
    const darkOption = fixture.nativeElement.querySelector('[data-testid="theme-dark-option"]');
    expect(darkOption).toBeTruthy();
  });

  // Test 57
  it('should call i18n.setLanguage when setLanguage is invoked', () => {
    const setSpy = vi.spyOn(i18nService, 'setLanguage');
    component.setLanguage('en');
    expect(setSpy).toHaveBeenCalledWith('en');
  });

  // Test 58
  it('should call theme.setTheme when setTheme is invoked', () => {
    const setSpy = vi.spyOn(themeService, 'setTheme');
    component.setTheme('dark');
    expect(setSpy).toHaveBeenCalledWith('dark');
  });

  // Test 59
  it('should have two radio inputs for language selection', () => {
    const radios = fixture.nativeElement.querySelectorAll('input[name="language"]');
    expect(radios.length).toBe(2);
  });

  // Test 60
  it('should have two radio inputs for theme selection', () => {
    const radios = fixture.nativeElement.querySelectorAll('input[name="theme"]');
    expect(radios.length).toBe(2);
  });

  // Test 61
  it('should reflect current language in radio state (fr checked by default)', () => {
    i18nService.setLanguage('fr');
    fixture.detectChanges();
    const frRadio = fixture.nativeElement.querySelector('[data-testid="lang-fr-radio"]');
    expect(frRadio.checked).toBe(true);
  });

  // Test 62
  it('should update language signal when setLanguage is called', () => {
    component.setLanguage('en');
    expect(i18nService.currentLanguage()).toBe('en');
  });
});
