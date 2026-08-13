import { HttpErrorResponse, HttpStatusCode } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import {
  AbstractControl,
  NonNullableFormBuilder,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { Router } from '@angular/router';

import { AuthApiService } from './auth-api.service';
import { ApiErrorResponse, PasswordResetRequest } from './auth.models';

const GENERIC_ERROR_MESSAGE = 'Unable to send a reset link right now. Please try again.';

@Component({
  selector: 'app-forgot-password-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule],
  templateUrl: './forgot-password-page.html',
  styleUrl: './password-recovery-page.scss',
})
export class ForgotPasswordPage {
  private readonly authApiService = inject(AuthApiService);
  private readonly router = inject(Router);

  readonly requestForm = inject(NonNullableFormBuilder).group({
    email: ['', [Validators.required, Validators.email, Validators.maxLength(320)]],
  });

  readonly isSubmitting = signal(false);
  readonly requestSent = signal(false);
  readonly requestedEmail = signal('');
  readonly requestError = signal<string | null>(null);

  protected isInvalid(control: AbstractControl): boolean {
    return control.invalid && (control.dirty || control.touched);
  }

  protected navigateToLogin(): void {
    void this.router.navigate(['/login']);
  }

  protected onSubmit(): void {
    this.requestForm.markAllAsTouched();
    this.requestError.set(null);

    if (this.requestForm.invalid || this.isSubmitting()) {
      return;
    }

    const request = this.requestForm.getRawValue();
    this.isSubmitting.set(true);
    this.authApiService.requestPasswordReset(request).subscribe({
      next: () => {
        this.requestedEmail.set(request.email);
        this.requestSent.set(true);
      },
      complete: () => this.isSubmitting.set(false),
      error: (error: unknown) => {
        this.handleRequestError(error);
        this.isSubmitting.set(false);
      },
    });
  }

  private handleRequestError(error: unknown): void {
    const httpError = error instanceof HttpErrorResponse ? error : null;
    const apiError = this.toApiErrorResponse(httpError);

    if (httpError?.status === HttpStatusCode.BadRequest) {
      this.requestError.set(apiError?.message ?? GENERIC_ERROR_MESSAGE);
      return;
    }

    this.requestError.set(GENERIC_ERROR_MESSAGE);
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
}
