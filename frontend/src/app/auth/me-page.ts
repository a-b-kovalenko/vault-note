import { HttpErrorResponse, HttpStatusCode } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { Router } from '@angular/router';

import { AuthApiService } from './auth-api.service';
import { AuthStateService } from './auth-state.service';
import { UserProfile } from './auth.models';

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

  readonly profile = signal<UserProfile | null>(null);
  readonly isLoading = signal(true);
  readonly loadError = signal<string | null>(null);

  ngOnInit(): void {
    this.authState
      .loadProfile(() => this.authApiService.profile())
      .subscribe({
        next: (profile) => {
          this.profile.set(profile);
          this.isLoading.set(false);
        },
        error: (error: unknown) => this.handleLoadError(error),
      });
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
