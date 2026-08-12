import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { finalize, map, Observable, of, shareReplay, tap } from 'rxjs';

import type { CsrfToken } from '../api/generated';
import { API_BASE_URL } from '../api/api-config';

@Injectable({ providedIn: 'root' })
export class CsrfService {
  private readonly http = inject(HttpClient);

  private bootstrapRequest$: Observable<void> | null = null;
  private hasBootstrapped = false;

  ensureToken(): Observable<void> {
    if (this.hasBootstrapped) {
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

            return void 0;
          }),
          tap(() => (this.hasBootstrapped = true)),
          finalize(() => (this.bootstrapRequest$ = null)),
          shareReplay({ bufferSize: 1, refCount: false }),
        );
    }

    return this.bootstrapRequest$;
  }

  invalidate(): void {
    this.hasBootstrapped = false;
  }
}
