import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import {
  AbstractControl,
  NonNullableFormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';

import { AuthApiService } from './auth-api.service';
import { PasswordResetConfirmRequest } from './auth.models';

type ResetPasswordStatus = 'ready' | 'success' | 'error';

const INVALID_LINK_MESSAGE = 'This reset link is invalid or has expired.';
const GENERIC_ERROR_MESSAGE = 'We could not reset your password right now. Please try again.';
const PASSWORD_POLICY_MESSAGE = 'Use at least 12 characters, two digits, and one letter.';

@Component({
  selector: 'app-reset-password-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule],
  templateUrl: './reset-password-page.html',
  styleUrl: './password-recovery-page.scss',
})
export class ResetPasswordPage implements OnInit {
  private readonly authApiService = inject(AuthApiService);
  private readonly formBuilder = inject(NonNullableFormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly resetForm = this.formBuilder.group(
    {
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

  readonly status = signal<ResetPasswordStatus>('ready');
  readonly isSubmitting = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly isPasswordVisible = signal(false);
  readonly isConfirmPasswordVisible = signal(false);

  private resetToken: string | null = null;

  ngOnInit(): void {
    this.resetToken = this.route.snapshot.queryParamMap.get('token');

    if (!this.resetToken) {
      this.status.set('error');
      this.errorMessage.set(INVALID_LINK_MESSAGE);
    }
  }

  protected isInvalid(control: AbstractControl): boolean {
    return control.invalid && (control.dirty || control.touched);
  }

  protected isConfirmPasswordInvalid(): boolean {
    const confirmPassword = this.resetForm.controls.confirmPassword;
    return (
      this.isInvalid(confirmPassword) ||
      (confirmPassword.touched && this.resetForm.hasError('passwordMismatch'))
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

  protected navigateToForgotPassword(): void {
    void this.router.navigate(['/forgot-password']);
  }

  protected onSubmit(): void {
    this.resetForm.markAllAsTouched();
    this.errorMessage.set(null);

    if (this.resetForm.invalid || this.isSubmitting() || !this.resetToken) {
      return;
    }

    const { confirmPassword: _confirmPassword, ...request } = this.resetForm.getRawValue();
    const resetRequest: PasswordResetConfirmRequest = {
      token: this.resetToken,
      newPassword: request.password,
    };
    this.isSubmitting.set(true);

    this.authApiService.confirmPasswordReset(resetRequest).subscribe({
      next: () => {
        this.resetToken = null;
        this.status.set('success');
        void this.router.navigate([], {
          relativeTo: this.route,
          queryParams: {},
          replaceUrl: true,
        });
      },
      complete: () => this.isSubmitting.set(false),
      error: (error: unknown) => {
        this.status.set('error');
        this.errorMessage.set(this.toErrorMessage(error));
        this.isSubmitting.set(false);
      },
    });
  }

  private toErrorMessage(error: unknown): string {
    if (!(error instanceof HttpErrorResponse)) {
      return GENERIC_ERROR_MESSAGE;
    }

    if (error.status === 400) {
      const response = error.error;
      if (response && typeof response.message === 'string') {
        return response.message;
      }

      return INVALID_LINK_MESSAGE;
    }

    return GENERIC_ERROR_MESSAGE;
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
