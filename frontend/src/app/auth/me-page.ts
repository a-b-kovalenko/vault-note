import { HttpErrorResponse, HttpStatusCode } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { Router } from '@angular/router';
import { finalize } from 'rxjs';

import { AuthApiService } from './auth-api.service';
import { AuthStateService } from './auth-state.service';
import { CurrentUserResponse } from './auth.models';

const GENERIC_USER_ERROR = 'Unable to load your profile right now. Please try again.';

@Component({
  selector: 'app-me-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './me-page.html',
  styleUrl: './me-page.scss',
})
export class MePage implements OnInit {
  private readonly authApiService = inject(AuthApiService);
  private readonly authState = inject(AuthStateService);
  private readonly router = inject(Router);

  readonly currentUser = signal<CurrentUserResponse | null>(null);
  readonly isLoading = signal(true);
  readonly isLoggingOut = signal(false);
  readonly loadError = signal<string | null>(null);

  ngOnInit(): void {
    this.authApiService.currentUser().subscribe({
      next: (user) => {
        this.currentUser.set(user);
        this.isLoading.set(false);
      },
      error: (error: unknown) => this.handleLoadError(error),
    });
  }

  protected onLogout(): void {
    if (this.isLoggingOut()) {
      return;
    }

    this.isLoggingOut.set(true);
    this.authApiService
      .logout()
      .pipe(
        finalize(() => {
          this.authState.clearSession();
          void this.router.navigate(['/login']);
        }),
      )
      .subscribe({ error: () => undefined });
  }

  private handleLoadError(error: unknown): void {
    this.isLoading.set(false);

    if (error instanceof HttpErrorResponse && error.status === HttpStatusCode.Unauthorized) {
      void this.router.navigate(['/login']);
      return;
    }

    this.loadError.set(GENERIC_USER_ERROR);
  }
}
