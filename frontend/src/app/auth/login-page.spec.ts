import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Observable, of } from 'rxjs';

import { AuthApiService } from './auth-api.service';
import { LoginRequest, LoginResponse } from './auth.models';
import { LoginPage } from './login-page';

describe('LoginPage', () => {
  let fixture: ComponentFixture<LoginPage>;
  let page: LoginPage;
  let loginRequests: LoginRequest[];

  beforeEach(async () => {
    loginRequests = [];
    const authApiService = {
      login(request: LoginRequest): Observable<LoginResponse> {
        loginRequests.push(request);
        return of({
          accessToken: 'test-access-token',
          tokenType: 'Bearer',
          expiresIn: 900,
        });
      },
    };

    await TestBed.configureTestingModule({
      imports: [LoginPage],
      providers: [{ provide: AuthApiService, useValue: authApiService }],
    }).compileComponents();

    fixture = TestBed.createComponent(LoginPage);
    page = fixture.componentInstance;
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

    const submitButton = fixture.nativeElement.querySelector(
      '.submit-button',
    ) as HTMLButtonElement;

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
    expect(page.isSubmitting()).toBe(false);
  });
});
