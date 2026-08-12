import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { Observable, of, throwError } from 'rxjs';

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

  beforeEach(async () => {
    loginRequests = [];
    loginResponse = of({
      accessToken: 'test-access-token',
      tokenType: 'Bearer',
      expiresIn: 900,
    });
    const authApiService = {
      login(request: LoginRequest): Observable<LoginResponse> {
        loginRequests.push(request);
        return loginResponse;
      },
    };

    await TestBed.configureTestingModule({
      imports: [LoginPage],
      providers: [{ provide: AuthApiService, useValue: authApiService }],
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

    expect(page.loginError()).toBe('Invalid email or password.');
    expect(
      (fixture.nativeElement.querySelector('#login-error') as HTMLElement).textContent,
    ).toContain('Invalid email or password.');
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
});
