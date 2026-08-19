import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { finalize, map, Observable, of, shareReplay } from 'rxjs';

import type { CsrfToken } from '../api/generated';
import { API_BASE_URL } from '../api/api-config';

@Injectable({ providedIn: 'root' })
export class CsrfService {
  private readonly http = inject(HttpClient);

  private bootstrapRequest$: Observable<void> | null = null;
  private csrfToken: string | null = null;

  ensureToken(): Observable<void> {
    if (this.csrfToken) {
      return of(void 0);
    }

    if (!this.bootstrapRequest$) {
      this.bootstrapRequest$ = this.http
        .get<CsrfToken>(`${API_BASE_URL}/csrf`, { withCredentials: true })
        .pipe(
          map((response) => {
            if (!response.token) {
              throw new Error('The CSRF bootstrap response did not contain a token.');
            }

            this.csrfToken = response.token;
            return void 0;
          }),
          finalize(() => (this.bootstrapRequest$ = null)),
          shareReplay({ bufferSize: 1, refCount: false }),
        );
    }

    return this.bootstrapRequest$;
  }

  getToken(): string | null {
    return this.csrfToken;
  }

  invalidate(): void {
    this.csrfToken = null;
  }
}
