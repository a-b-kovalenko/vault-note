import { inject, Injectable } from '@angular/core';
import { defer, finalize, firstValueFrom, from, Observable, shareReplay, tap } from 'rxjs';

import { LoginResponse } from './auth.models';
import { AuthApiService } from './auth-api.service';
import { AuthStateService } from './auth-state.service';

const AUTH_REFRESH_LOCK_NAME = 'vaultnote-auth-refresh';

@Injectable({ providedIn: 'root' })
export class AuthRefreshService {
  private readonly authApiService = inject(AuthApiService);
  private readonly authState = inject(AuthStateService);

  private refreshRequest$: Observable<LoginResponse> | null = null;

  refresh(): Observable<LoginResponse> {
    if (!this.refreshRequest$) {
      this.refreshRequest$ = this.refreshWithSharedLock().pipe(
        tap((session) => this.authState.setSession(session)),
        finalize(() => (this.refreshRequest$ = null)),
        shareReplay({ bufferSize: 1, refCount: false }),
      );
    }

    return this.refreshRequest$;
  }

  private refreshWithSharedLock(): Observable<LoginResponse> {
    const lockManager = getLockManager();
    if (!lockManager) {
      return this.authApiService.refresh();
    }

    return defer(() =>
      from(
        lockManager.request(AUTH_REFRESH_LOCK_NAME, () =>
          firstValueFrom(this.authApiService.refresh()),
        ),
      ),
    );
  }
}

function getLockManager(): LockManager | null {
  if (typeof navigator === 'undefined' || !navigator.locks) {
    return null;
  }

  return navigator.locks;
}
