import { HttpErrorResponse, HttpStatusCode } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import {
  AbstractControl,
  NonNullableFormBuilder,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';

import { AuthApiService } from './auth-api.service';
import { AuthStateService } from './auth-state.service';
import { ApiErrorResponse, LoginRequest, ValidationViolation } from './auth.models';

type LoginField = keyof LoginRequest;

const GENERIC_LOGIN_ERROR = 'Unable to sign in right now. Please try again.';

@Component({
  selector: 'app-login-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule],
  templateUrl: './login-page.html',
  styleUrl: './login-page.scss',
})
export class LoginPage {
  private readonly authApiService = inject(AuthApiService);
  private readonly authState = inject(AuthStateService);

  readonly loginForm = inject(NonNullableFormBuilder).group({
    email: ['', [Validators.required, Validators.email, Validators.maxLength(320)]],
    password: ['', [Validators.required, Validators.maxLength(256)]],
  });

  readonly isSubmitting = signal(false);
  readonly loginError = signal<string | null>(null);

  protected isInvalid(control: AbstractControl): boolean {
    return control.invalid && (control.dirty || control.touched);
  }

  protected onSubmit(): void {
    this.loginForm.markAllAsTouched();
    this.loginError.set(null);
    this.clearServerErrors();

    if (this.loginForm.invalid || this.isSubmitting()) {
      return;
    }

    this.isSubmitting.set(true);
    this.authApiService.login(this.loginForm.getRawValue()).subscribe({
      next: (session) => this.authState.setSession(session),
      complete: () => this.isSubmitting.set(false),
      error: (error: unknown) => {
        this.handleLoginError(error);
        this.isSubmitting.set(false);
      },
    });
  }

  private handleLoginError(error: unknown): void {
    const httpError = error instanceof HttpErrorResponse ? error : null;
    const apiError = this.toApiErrorResponse(httpError);

    if (httpError?.status === HttpStatusCode.BadRequest) {
      const hasFieldErrors = apiError && this.applyValidationViolations(apiError.violations);

      if (hasFieldErrors) {
        return;
      }

      this.loginError.set(apiError?.message ?? GENERIC_LOGIN_ERROR);
      return;
    }

    if (httpError?.status === HttpStatusCode.Unauthorized) {
      this.loginError.set(apiError?.message ?? 'Invalid email or password.');
      return;
    }

    this.loginError.set(GENERIC_LOGIN_ERROR);
  }

  private applyValidationViolations(violations: ValidationViolation[]): boolean {
    let hasFieldErrors = false;

    for (const violation of violations) {
      if (!this.isLoginField(violation.field)) {
        continue;
      }

      const control = this.loginForm.controls[violation.field];
      control.setErrors({ ...(control.errors ?? {}), server: violation.message });
      hasFieldErrors = true;
    }

    return hasFieldErrors;
  }

  private clearServerErrors(): void {
    for (const control of Object.values(this.loginForm.controls)) {
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

  private isLoginField(field: string): field is LoginField {
    return field === 'email' || field === 'password';
  }
}
