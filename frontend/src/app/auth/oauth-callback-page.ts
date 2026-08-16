import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, finalize, of, switchMap } from 'rxjs';

import { AvatarStateService } from '../avatar/avatar-state.service';
import { AuthApiService } from './auth-api.service';
import { AuthRefreshService } from './auth-refresh.service';
import { AuthStateService } from './auth-state.service';

@Component({
  selector: 'app-oauth-callback-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './oauth-callback-page.html',
  styleUrl: './oauth-callback-page.scss',
})
export class OAuthCallbackPage implements OnInit {
  private readonly avatarState = inject(AvatarStateService);
  private readonly authApiService = inject(AuthApiService);
  private readonly authRefreshService = inject(AuthRefreshService);
  private readonly authState = inject(AuthStateService);
  private readonly router = inject(Router);

  readonly isLoading = signal(true);

  ngOnInit(): void {
    this.authState.clearSession();

    this.authRefreshService
      .refresh()
      .pipe(
        switchMap(() => this.authState.loadProfile(() => this.authApiService.profile())),
        switchMap(() => this.avatarState.load().pipe(catchError(() => of(void 0)))),
        finalize(() => this.isLoading.set(false)),
      )
      .subscribe({
        next: () => void this.router.navigate(['/me']),
        error: () => this.handleAuthenticationError(),
      });
  }

  private handleAuthenticationError(): void {
    this.authState.clearSession();
    void this.router.navigate(['/login'], { queryParams: { error: 'oauth' } });
  }
}
