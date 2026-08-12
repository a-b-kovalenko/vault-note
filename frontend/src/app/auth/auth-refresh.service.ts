import { finalize, Observable, shareReplay, tap } from 'rxjs';
import { inject, Injectable } from '@angular/core';

import { LoginResponse } from './auth.models';
import { AuthApiService } from './auth-api.service';
import { AuthStateService } from './auth-state.service';

@Injectable({ providedIn: 'root' })
export class AuthRefreshService {
  private readonly authApiService = inject(AuthApiService);
  private readonly authState = inject(AuthStateService);

  private refreshRequest$: Observable<LoginResponse> | null = null;

  refresh(): Observable<LoginResponse> {
    if (!this.refreshRequest$) {
      this.refreshRequest$ = this.authApiService.refresh().pipe(
        tap((session) => this.authState.setSession(session)),
        finalize(() => (this.refreshRequest$ = null)),
        shareReplay({ bufferSize: 1, refCount: false }),
      );
    }

    return this.refreshRequest$;
  }
}
