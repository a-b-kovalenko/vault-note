import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

import { AuthApiService } from './auth-api.service';

type VerificationStatus = 'loading' | 'success' | 'error';

const INVALID_LINK_MESSAGE = 'This verification link is invalid or has expired.';
const GENERIC_ERROR_MESSAGE = 'We could not verify your email right now. Please try again.';

@Component({
  selector: 'app-verify-email-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './verify-email-page.html',
  styleUrl: './verify-email-page.scss',
})
export class VerifyEmailPage implements OnInit {
  private readonly authApiService = inject(AuthApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly status = signal<VerificationStatus>('loading');
  readonly errorMessage = signal<string | null>(null);

  ngOnInit(): void {
    const token = this.route.snapshot.queryParamMap.get('token');

    if (!token) {
      this.status.set('error');
      this.errorMessage.set(INVALID_LINK_MESSAGE);
      return;
    }

    this.authApiService.verifyEmail(token).subscribe({
      next: () => this.status.set('success'),
      error: (error: unknown) => {
        this.status.set('error');
        this.errorMessage.set(this.toErrorMessage(error));
      },
    });
  }

  protected navigateToLogin(): void {
    void this.router.navigate(['/login']);
  }

  protected navigateToRegistration(): void {
    void this.router.navigate(['/register']);
  }

  private toErrorMessage(error: unknown): string {
    if (!(error instanceof HttpErrorResponse)) {
      return GENERIC_ERROR_MESSAGE;
    }

    if (error.status === 400 || error.status === 404) {
      const response = error.error;
      if (response && typeof response.message === 'string') {
        return response.message;
      }

      return INVALID_LINK_MESSAGE;
    }

    return GENERIC_ERROR_MESSAGE;
  }
}
