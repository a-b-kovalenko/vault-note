import { HttpErrorResponse, HttpStatusCode } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import {
  AbstractControl,
  NonNullableFormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { Router } from '@angular/router';

import { AuthApiService } from './auth-api.service';
import { ApiErrorResponse, RegisterUserRequest, ValidationViolation } from './auth.models';
import { RateLimitNotice } from './rate-limit-notice';
import { RateLimitState } from './rate-limit-state';

type RegisterUserField = keyof RegisterUserRequest | 'confirmPassword';

const GENERIC_REGISTRATION_ERROR = 'Unable to create your account right now. Please try again.';
const PASSWORD_POLICY_MESSAGE = 'Use at least 12 characters, two digits, and one letter.';

@Component({
  selector: 'app-register-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, RateLimitNotice],
  providers: [RateLimitState],
  templateUrl: './register-page.html',
  styleUrl: './register-page.scss',
})
export class RegisterPage {
  private readonly authApiService = inject(AuthApiService);
  private readonly formBuilder = inject(NonNullableFormBuilder);
  private readonly router = inject(Router);

  readonly rateLimit = inject(RateLimitState);

  readonly registerForm = this.formBuilder.group(
    {
      displayName: ['', [Validators.required, Validators.maxLength(100)]],
      email: ['', [Validators.required, Validators.email, Validators.maxLength(320)]],
      password: [
        '',
        [
          Validators.required,
          Validators.minLength(12),
          Validators.maxLength(256),
          passwordPolicyValidator,
        ],
      ],
      confirmPassword: ['', [Validators.required]],
    },
    { validators: passwordsMatchValidator },
  );

  readonly isSubmitting = signal(false);
  readonly registrationError = signal<string | null>(null);
  readonly registrationSuccess = signal(false);
  readonly registeredEmail = signal('');
  readonly isPasswordVisible = signal(false);
  readonly isConfirmPasswordVisible = signal(false);

  protected isInvalid(control: AbstractControl): boolean {
    return control.invalid && (control.dirty || control.touched);
  }

  protected isConfirmPasswordInvalid(): boolean {
    const confirmPassword = this.registerForm.controls.confirmPassword;
    return (
      this.isInvalid(confirmPassword) ||
      (confirmPassword.touched && this.registerForm.hasError('passwordMismatch'))
    );
  }

  protected togglePasswordVisibility(): void {
    this.isPasswordVisible.update((isVisible) => !isVisible);
  }

  protected toggleConfirmPasswordVisibility(): void {
    this.isConfirmPasswordVisible.update((isVisible) => !isVisible);
  }

  protected navigateToLogin(): void {
    void this.router.navigate(['/login']);
  }

  protected onSubmit(): void {
    this.registerForm.markAllAsTouched();
    this.registrationError.set(null);
    this.clearServerErrors();

    if (this.registerForm.invalid || this.isSubmitting() || this.rateLimit.isActive()) {
      return;
    }

    const { confirmPassword: _confirmPassword, ...request } = this.registerForm.getRawValue();
    this.isSubmitting.set(true);

    this.authApiService.register(request).subscribe({
      next: () => {
        this.registeredEmail.set(request.email);
        this.registrationSuccess.set(true);
      },
      complete: () => this.isSubmitting.set(false),
      error: (error: unknown) => {
        this.handleRegistrationError(error);
        this.isSubmitting.set(false);
      },
    });
  }

  private handleRegistrationError(error: unknown): void {
    if (this.rateLimit.start(error)) {
      this.registrationError.set(null);
      return;
    }

    const httpError = error instanceof HttpErrorResponse ? error : null;
    const apiError = this.toApiErrorResponse(httpError);

    if (httpError?.status === HttpStatusCode.BadRequest) {
      const hasFieldErrors = apiError && this.applyValidationViolations(apiError.violations);

      if (hasFieldErrors) {
        return;
      }

      this.registrationError.set(apiError?.message ?? GENERIC_REGISTRATION_ERROR);
      return;
    }

    if (httpError?.status === HttpStatusCode.Conflict) {
      this.registrationError.set(apiError?.message ?? 'An account with this email already exists.');
      return;
    }

    this.registrationError.set(GENERIC_REGISTRATION_ERROR);
  }

  private applyValidationViolations(violations: ValidationViolation[]): boolean {
    let hasFieldErrors = false;

    for (const violation of violations) {
      const control = this.controlForApiField(violation.field);
      if (!control) {
        continue;
      }

      control.setErrors({ ...(control.errors ?? {}), server: violation.message });
      hasFieldErrors = true;
    }

    return hasFieldErrors;
  }

  private controlForApiField(field: string): AbstractControl | null {
    if (field === 'display_name') {
      return this.registerForm.controls.displayName;
    }

    if (this.isRegisterUserField(field)) {
      return this.registerForm.controls[field];
    }

    return null;
  }

  private clearServerErrors(): void {
    for (const control of Object.values(this.registerForm.controls)) {
      if (!control.hasError('server')) {
        continue;
      }

      const errors = { ...(control.errors ?? {}) };
      delete errors['server'];
      control.setErrors(Object.keys(errors).length > 0 ? errors : null);
    }
  }

  private toApiErrorResponse(error: HttpErrorResponse | null): ApiErrorResponse | null {
    if (!error || typeof error.error !== 'object' || error.error === null) {
      return null;
    }

    const response = error.error as Partial<ApiErrorResponse>;

    if (typeof response.code !== 'string' || typeof response.message !== 'string') {
      return null;
    }

    return {
      code: response.code,
      message: response.message,
      violations: Array.isArray(response.violations) ? response.violations : [],
    };
  }

  private isRegisterUserField(field: string): field is RegisterUserField {
    return (
      field === 'email' ||
      field === 'displayName' ||
      field === 'password' ||
      field === 'confirmPassword'
    );
  }
}

function passwordPolicyValidator(control: AbstractControl): ValidationErrors | null {
  const value = control.value;
  if (typeof value !== 'string' || value.length === 0) {
    return null;
  }

  const digitCount = Array.from(value).filter((character) =>
    /\p{Decimal_Number}/u.test(character),
  ).length;
  const hasLetter = Array.from(value).some((character) => /\p{Letter}/u.test(character));

  return digitCount >= 2 && hasLetter ? null : { passwordPolicy: PASSWORD_POLICY_MESSAGE };
}

function passwordsMatchValidator(control: AbstractControl): ValidationErrors | null {
  const password = control.get('password')?.value;
  const confirmPassword = control.get('confirmPassword')?.value;

  if (!password || !confirmPassword || password === confirmPassword) {
    return null;
  }

  return { passwordMismatch: true };
}
