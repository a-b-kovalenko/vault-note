import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { Observable, of, throwError } from 'rxjs';

import { AuthApiService } from './auth-api.service';
import { RegisterUserRequest, RegisterUserResponse } from './auth.models';
import { RegisterPage } from './register-page';

describe('RegisterPage', () => {
  let fixture: ComponentFixture<RegisterPage>;
  let page: RegisterPage;
  let registrationRequests: RegisterUserRequest[];
  let registrationResponse: Observable<RegisterUserResponse>;
  let navigate: ReturnType<typeof vi.fn>;

  beforeEach(async () => {
    registrationRequests = [];
    registrationResponse = of({ userId: 42 });
    navigate = vi.fn().mockResolvedValue(true);

    const authApiService = {
      register(request: RegisterUserRequest): Observable<RegisterUserResponse> {
        registrationRequests.push(request);
        return registrationResponse;
      },
    };

    await TestBed.configureTestingModule({
      imports: [RegisterPage],
      providers: [
        { provide: AuthApiService, useValue: authApiService },
        { provide: Router, useValue: { navigate } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(RegisterPage);
    page = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should require a valid profile and password', () => {
    expect(page.registerForm.invalid).toBe(true);

    page.registerForm.setValue({
      displayName: 'New User',
      email: 'new-user@example.com',
      password: 'Password1234',
      confirmPassword: 'Password1234',
    });

    expect(page.registerForm.valid).toBe(true);

    page.registerForm.controls.password.setValue('short');
    expect(page.registerForm.controls.password.hasError('minlength')).toBe(true);
    expect(page.registerForm.controls.password.hasError('passwordPolicy')).toBe(true);
  });

  it('should show a mismatch error for different passwords', () => {
    page.registerForm.setValue({
      displayName: 'New User',
      email: 'new-user@example.com',
      password: 'Password1234',
      confirmPassword: 'Password4321',
    });
    page.registerForm.controls.confirmPassword.markAsTouched();
    fixture.detectChanges();

    expect(page.registerForm.hasError('passwordMismatch')).toBe(true);
    expect(
      (fixture.nativeElement.querySelector('#confirm-password-error') as HTMLElement).textContent,
    ).toContain('Passwords do not match.');
  });

  it('should submit the registration and show email verification guidance', () => {
    page.registerForm.setValue({
      displayName: 'New User',
      email: 'new-user@example.com',
      password: 'Password1234',
      confirmPassword: 'Password1234',
    });

    const form = fixture.nativeElement.querySelector('form') as HTMLFormElement;
    form.dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));
    fixture.detectChanges();

    expect(registrationRequests).toEqual([
      {
        displayName: 'New User',
        email: 'new-user@example.com',
        password: 'Password1234',
      },
    ]);
    expect(page.registrationSuccess()).toBe(true);
    expect(fixture.nativeElement.textContent).toContain('Check your inbox');
    expect(fixture.nativeElement.textContent).toContain('new-user@example.com');
  });

  it('should show the duplicate-email message from the backend', () => {
    registrationResponse = throwError(
      () =>
        new HttpErrorResponse({
          status: 409,
          error: {
            code: 'ENTITY_ALREADY_EXISTS',
            message: 'An account with this email already exists.',
            violations: [],
          },
        }),
    );
    page.registerForm.setValue({
      displayName: 'New User',
      email: 'existing@example.com',
      password: 'Password1234',
      confirmPassword: 'Password1234',
    });

    const form = fixture.nativeElement.querySelector('form') as HTMLFormElement;
    form.dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));
    fixture.detectChanges();

    expect(page.registrationError()).toBe('An account with this email already exists.');
    expect(
      (fixture.nativeElement.querySelector('#registration-error') as HTMLElement).textContent,
    ).toContain('An account with this email already exists.');
    expect(page.isSubmitting()).toBe(false);
  });

  it('should map backend display-name validation to the field', () => {
    registrationResponse = throwError(
      () =>
        new HttpErrorResponse({
          status: 400,
          error: {
            code: 'VALIDATION_FAILED',
            message: 'Request validation failed',
            violations: [
              {
                field: 'display_name',
                code: 'REQUIRED',
                message: 'Display name is required.',
              },
            ],
          },
        }),
    );
    page.registerForm.setValue({
      displayName: 'New User',
      email: 'new-user@example.com',
      password: 'Password1234',
      confirmPassword: 'Password1234',
    });

    const form = fixture.nativeElement.querySelector('form') as HTMLFormElement;
    form.dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));
    fixture.detectChanges();

    expect(page.registerForm.controls.displayName.getError('server')).toBe(
      'Display name is required.',
    );
    expect(page.registrationError()).toBe(null);
  });

  it('should navigate back to login', () => {
    const signInButton = fixture.nativeElement.querySelector(
      '.login-prompt .text-button',
    ) as HTMLButtonElement | null;

    signInButton?.click();

    expect(navigate).toHaveBeenCalledWith(['/login']);
  });
});
