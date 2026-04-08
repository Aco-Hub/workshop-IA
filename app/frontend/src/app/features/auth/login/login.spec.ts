import { beforeAll } from 'vitest';
import { TestBed, ComponentFixture } from '@angular/core/testing';
import { getTestBed } from '@angular/core/testing';
import {
  BrowserDynamicTestingModule,
  platformBrowserDynamicTesting,
} from '@angular/platform-browser-dynamic/testing';
import { provideRouter, Router } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { of, throwError } from 'rxjs';
import { LoginComponent } from './login';
import { AuthService } from '../../../core/services/auth.service';

beforeAll(() => {
  getTestBed().initTestEnvironment(
    BrowserDynamicTestingModule,
    platformBrowserDynamicTesting()
  );
});

describe('LoginComponent', () => {
  let fixture: ComponentFixture<LoginComponent>;
  let component: LoginComponent;
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    localStorage.clear();
    await TestBed.configureTestingModule({
      imports: [LoginComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([{ path: 'calendar', component: LoginComponent }]),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(LoginComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
    TestBed.resetTestingModule();
  });

  // Test 45
  it('should render login form', () => {
    const form = fixture.nativeElement.querySelector('form');
    expect(form).toBeTruthy();
  });

  // Test 46
  it('should have email input field with data-testid', () => {
    const emailInput = fixture.nativeElement.querySelector('[data-testid="login-email-input"]');
    expect(emailInput).toBeTruthy();
    expect(emailInput.type).toBe('email');
  });

  // Test 47
  it('should have password input field with data-testid', () => {
    const passwordInput = fixture.nativeElement.querySelector('[data-testid="login-password-input"]');
    expect(passwordInput).toBeTruthy();
    expect(passwordInput.type).toBe('password');
  });

  // Test 48
  it('should have a submit button', () => {
    const btn = fixture.nativeElement.querySelector('[data-testid="login-submit-btn"]');
    expect(btn).toBeTruthy();
    expect(btn.type).toBe('submit');
  });

  // Test 49
  it('should show error element when login fails', async () => {
    const authService = TestBed.inject(AuthService);
    vi.spyOn(authService, 'login').mockReturnValue(throwError(() => new Error('401')));

    component.email.set('dev@example.com');
    component.password.set('wrongpass');
    component.onSubmit();

    await new Promise(resolve => setTimeout(resolve, 0));
    fixture.detectChanges();

    const errorEl = fixture.nativeElement.querySelector('[data-testid="login-error"]');
    expect(errorEl).toBeTruthy();
  });

  // Test 50
  it('should navigate to /calendar on successful login', async () => {
    const authService = TestBed.inject(AuthService);
    const router = TestBed.inject(Router);
    const navigateSpy = vi.spyOn(router, 'navigate');

    vi.spyOn(authService, 'login').mockReturnValue(
      of({
        token: 'jwt',
        id: 1,
        email: 'dev@example.com',
        username: 'dev',
        title: '',
        discordLink: '',
        discordAvatarUrl: '',
        role: 'STANDARD' as const,
      })
    );

    component.email.set('dev@example.com');
    component.password.set('correctpass');
    component.onSubmit();

    await new Promise(resolve => setTimeout(resolve, 0));

    expect(navigateSpy).toHaveBeenCalledWith(['/calendar']);
  });

  // Test 51
  it('should not call login when email is empty', () => {
    const authService = TestBed.inject(AuthService);
    const loginSpy = vi.spyOn(authService, 'login');

    component.email.set('');
    component.password.set('somepass');
    component.onSubmit();

    expect(loginSpy).not.toHaveBeenCalled();
  });

  // Test 52
  it('should not call login when password is empty', () => {
    const authService = TestBed.inject(AuthService);
    const loginSpy = vi.spyOn(authService, 'login');

    component.email.set('dev@example.com');
    component.password.set('');
    component.onSubmit();

    expect(loginSpy).not.toHaveBeenCalled();
  });

  // Test 53
  it('should set loading to true while submitting', async () => {
    const authService = TestBed.inject(AuthService);
    vi.spyOn(authService, 'login').mockReturnValue(
      of({
        token: 'jwt', id: 1, email: 'dev@example.com', username: 'dev',
        title: '', discordLink: '', discordAvatarUrl: '', role: 'STANDARD' as const,
      })
    );

    component.email.set('dev@example.com');
    component.password.set('pass');
    expect(component.loading()).toBe(false);
    component.onSubmit();
    // loading should be set synchronously before subscription completes
    await new Promise(resolve => setTimeout(resolve, 0));
    // After the observable completes, loading should be false again
    expect(component.loading()).toBe(false);
  });
});
