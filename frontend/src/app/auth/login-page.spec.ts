import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpErrorResponse, HttpHeaders } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, of, throwError } from 'rxjs';

import { API_BASE_URL } from '../api/api-config';
import { AuthApiService } from './auth-api.service';
import { AuthStateService } from './auth-state.service';
import { LoginRequest, LoginResponse } from './auth.models';
import { LoginPage } from './login-page';

describe('LoginPage', () => {
  let fixture: ComponentFixture<LoginPage>;
  let page: LoginPage;
  let loginRequests: LoginRequest[];
  let loginResponse: Observable<LoginResponse>;
  let authState: AuthStateService;
  let navigate: ReturnType<typeof vi.fn>;

  beforeEach(async () => {
    loginRequests = [];
    loginResponse = of({
      accessToken: 'test-access-token',
      tokenType: 'Bearer',
      expiresIn: 900,
    });
    navigate = vi.fn().mockResolvedValue(true);
    const authApiService = {
      login(request: LoginRequest): Observable<LoginResponse> {
        loginRequests.push(request);
        return loginResponse;
      },
    };

    await TestBed.configureTestingModule({
      imports: [LoginPage],
      providers: [
        { provide: AuthApiService, useValue: authApiService },
        { provide: Router, useValue: { navigate } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(LoginPage);
    page = fixture.componentInstance;
    authState = TestBed.inject(AuthStateService);
    fixture.detectChanges();
  });

  it('should require a valid email and a non-empty password', () => {
    expect(page.loginForm.invalid).toBe(true);
    expect(page.loginForm.controls.email.hasError('required')).toBe(true);
    expect(page.loginForm.controls.password.hasError('required')).toBe(true);

    page.loginForm.controls.email.setValue('invalid-email');
    page.loginForm.controls.password.setValue('password');

    expect(page.loginForm.controls.email.hasError('email')).toBe(true);
    expect(page.loginForm.valid).toBe(false);
  });

  it('should enable submit for valid credentials within API limits', () => {
    page.loginForm.setValue({
      email: 'user@example.com',
      password: 'password',
    });
    fixture.detectChanges();

    const submitButton = fixture.nativeElement.querySelector('.submit-button') as HTMLButtonElement;

    expect(page.loginForm.valid).toBe(true);
    expect(submitButton.disabled).toBe(false);
  });

  it('should toggle password visibility', () => {
    const passwordInput = fixture.nativeElement.querySelector('#password') as HTMLInputElement;
    const toggleButton = fixture.nativeElement.querySelector(
      '.password-toggle',
    ) as HTMLButtonElement;

    expect(passwordInput.type).toBe('password');
    expect(toggleButton.getAttribute('aria-label')).toBe('Show password');
    expect(toggleButton.getAttribute('aria-pressed')).toBe('false');

    toggleButton.click();
    fixture.detectChanges();

    expect(passwordInput.type).toBe('text');
    expect(toggleButton.getAttribute('aria-label')).toBe('Hide password');
    expect(toggleButton.getAttribute('aria-pressed')).toBe('true');

    toggleButton.click();
    fixture.detectChanges();

    expect(passwordInput.type).toBe('password');
  });

  it('should navigate to registration from the account prompt', () => {
    const registrationButton = fixture.nativeElement.querySelector(
      '.registration-button',
    ) as HTMLButtonElement;

    registrationButton.click();

    expect(navigate).toHaveBeenCalledWith(['/register']);
  });

  it('should navigate to password recovery from the forgot-password action', () => {
    const forgotPasswordButton = fixture.nativeElement.querySelector(
      '.account-actions .text-button',
    ) as HTMLButtonElement;

    forgotPasswordButton.click();

    expect(navigate).toHaveBeenCalledWith(['/forgot-password']);
  });

  it('should link Google sign-in to the backend OAuth authorization endpoint', () => {
    const googleButton = fixture.nativeElement.querySelector(
      '.oauth-button-google',
    ) as HTMLAnchorElement;

    expect(googleButton.getAttribute('href')).toBe(`${API_BASE_URL}/oauth2/authorization/google`);
  });

  it('should show an error after an invalid control is touched', () => {
    page.loginForm.controls.email.markAsTouched();
    fixture.detectChanges();

    const error = fixture.nativeElement.querySelector('#email-error') as HTMLElement;

    expect(error.textContent).toContain('Email is required.');
  });

  it('should submit valid credentials through AuthApiService', () => {
    page.loginForm.setValue({
      email: 'user@example.com',
      password: 'password',
    });
    fixture.detectChanges();

    const form = fixture.nativeElement.querySelector('form') as HTMLFormElement;
    form.dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));

    expect(loginRequests).toEqual([
      {
        email: 'user@example.com',
        password: 'password',
      },
    ]);
    expect(authState.accessToken()).toBe('test-access-token');
    expect(navigate).toHaveBeenCalledWith(['/me']);
    expect(page.isSubmitting()).toBe(false);
  });

  it('should show the backend authentication message for an unauthorized response', () => {
    loginResponse = throwError(
      () =>
        new HttpErrorResponse({
          status: 401,
          error: {
            code: 'AUTHENTICATION_FAILED',
            message: 'Invalid email or password.',
            violations: [],
          },
        }),
    );
    page.loginForm.setValue({
      email: 'user@example.com',
      password: 'wrong-password',
    });

    const form = fixture.nativeElement.querySelector('form') as HTMLFormElement;
    form.dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));
    fixture.detectChanges();

    expect(page.loginError()).toBe(
      'Invalid email or password. If you registered recently, check your inbox and verify your email before signing in.',
    );
    expect(
      (fixture.nativeElement.querySelector('#login-error') as HTMLElement).textContent,
    ).toContain('check your inbox and verify your email');
    expect(page.isSubmitting()).toBe(false);
  });

  it('should apply backend validation messages to the matching controls', () => {
    loginResponse = throwError(
      () =>
        new HttpErrorResponse({
          status: 400,
          error: {
            code: 'VALIDATION_FAILED',
            message: 'Request validation failed',
            violations: [
              {
                field: 'email',
                code: 'INVALID_FORMAT',
                message: 'Email format is not accepted.',
              },
            ],
          },
        }),
    );
    page.loginForm.setValue({
      email: 'user@example.com',
      password: 'password',
    });

    const form = fixture.nativeElement.querySelector('form') as HTMLFormElement;
    form.dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));
    fixture.detectChanges();

    expect(page.loginForm.controls.email.getError('server')).toBe('Email format is not accepted.');
    expect(
      (fixture.nativeElement.querySelector('#email-error') as HTMLElement).textContent,
    ).toContain('Email format is not accepted.');
    expect(page.loginError()).toBe(null);
  });

  it('should show a generic message for an unexpected login failure', () => {
    loginResponse = throwError(() => new HttpErrorResponse({ status: 500 }));
    page.loginForm.setValue({
      email: 'user@example.com',
      password: 'password',
    });

    const form = fixture.nativeElement.querySelector('form') as HTMLFormElement;
    form.dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));
    fixture.detectChanges();

    expect(page.loginError()).toBe('Unable to sign in right now. Please try again.');
    expect(page.isSubmitting()).toBe(false);
  });

  it('should show a countdown and preserve the form after a rate-limit response', () => {
    loginResponse = throwError(
      () =>
        new HttpErrorResponse({
          status: 429,
          headers: new HttpHeaders({ 'Retry-After': '5' }),
          error: {
            code: 'RATE_LIMIT_EXCEEDED',
            message: 'Too many requests. Please try again later.',
            violations: [],
          },
        }),
    );
    page.loginForm.setValue({ email: 'user@example.com', password: 'password' });

    const form = fixture.nativeElement.querySelector('form') as HTMLFormElement;
    form.dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));
    fixture.detectChanges();

    expect(page.rateLimit.remainingSeconds()).toBe(5);
    expect(page.loginError()).toBe(null);
    expect(page.loginForm.getRawValue()).toEqual({
      email: 'user@example.com',
      password: 'password',
    });
    expect(
      (fixture.nativeElement.querySelector('.submit-button') as HTMLButtonElement).disabled,
    ).toBe(true);
    expect(fixture.nativeElement.textContent).toContain('5 seconds');
  });
});
