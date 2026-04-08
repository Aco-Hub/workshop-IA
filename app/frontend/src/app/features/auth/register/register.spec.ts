import { beforeAll } from 'vitest';
import { TestBed, ComponentFixture } from '@angular/core/testing';
import { getTestBed } from '@angular/core/testing';
import {
  BrowserDynamicTestingModule,
  platformBrowserDynamicTesting,
} from '@angular/platform-browser-dynamic/testing';
import { provideRouter, ActivatedRoute, Router } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { of } from 'rxjs';
import { RegisterComponent } from './register';
import { AuthService } from '../../../core/services/auth.service';

beforeAll(() => {
  getTestBed().initTestEnvironment(
    BrowserDynamicTestingModule,
    platformBrowserDynamicTesting()
  );
});

describe('RegisterComponent', () => {
  let fixture: ComponentFixture<RegisterComponent>;
  let component: RegisterComponent;
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    localStorage.clear();
    await TestBed.configureTestingModule({
      imports: [RegisterComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([{ path: 'calendar', component: RegisterComponent }]),
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              queryParamMap: {
                get: (key: string) => (key === 'token' ? 'invite-abc-123' : null),
              },
            },
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(RegisterComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
    TestBed.resetTestingModule();
  });

  // Test 50
  it('should render register form', () => {
    const form = fixture.nativeElement.querySelector('form');
    expect(form).toBeTruthy();
  });

  // Test 51
  it('should have username input field', () => {
    const usernameInput = fixture.nativeElement.querySelector('[data-testid="register-username-input"]');
    expect(usernameInput).toBeTruthy();
  });

  // Test 52
  it('should have password input field', () => {
    const passwordInput = fixture.nativeElement.querySelector('[data-testid="register-password-input"]');
    expect(passwordInput).toBeTruthy();
    expect(passwordInput.type).toBe('password');
  });

  // Test 53
  it('should have title input field', () => {
    const titleInput = fixture.nativeElement.querySelector('[data-testid="register-title-input"]');
    expect(titleInput).toBeTruthy();
  });

  // Test 54
  it('should read invite token from URL query param on init', () => {
    expect(component.inviteToken()).toBe('invite-abc-123');
  });

  // Test 55
  it('should have a submit button', () => {
    const btn = fixture.nativeElement.querySelector('[data-testid="register-submit-btn"]');
    expect(btn).toBeTruthy();
    expect(btn.type).toBe('submit');
  });

  // Test 56
  it('should navigate to /calendar after successful registration', async () => {
    const authService = TestBed.inject(AuthService);
    const router = TestBed.inject(Router);
    const navigateSpy = vi.spyOn(router, 'navigate');

    vi.spyOn(authService, 'register').mockReturnValue(
      of({
        token: 'jwt',
        id: 1,
        email: 'newdev@example.com',
        username: 'newdev',
        title: 'Developer',
        discordLink: '',
        discordAvatarUrl: '',
        role: 'STANDARD' as const,
      })
    );

    component.username.set('newdev');
    component.password.set('securepass');
    component.onSubmit();

    await new Promise(resolve => setTimeout(resolve, 0));

    expect(navigateSpy).toHaveBeenCalledWith(['/calendar']);
  });

  // Test 57
  it('should show error when no invite token and submit is called', () => {
    component.inviteToken.set('');
    component.onSubmit();
    expect(component.error()).toBe('auth.invite_required');
  });
});
