import { HttpErrorResponse, HttpInterceptorFn, HttpRequest } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, switchMap, throwError } from 'rxjs';

import { API_BASE_URL } from '../api/api-config';
import { CsrfService } from './csrf.service';

const CSRF_HEADER = 'X-XSRF-TOKEN';
const SAFE_METHODS = new Set(['GET', 'HEAD', 'OPTIONS', 'TRACE']);

export const csrfInterceptor: HttpInterceptorFn = (request, next) => {
  if (
    !isBackendRequest(request.url) ||
    SAFE_METHODS.has(request.method) ||
    request.headers.has(CSRF_HEADER)
  ) {
    return next(request);
  }

  const csrfService = inject(CsrfService);

  return csrfService.ensureToken().pipe(
    switchMap(() => next(addToken(request, csrfService.getToken()))),
    catchError((error: unknown) => {
      if (!isCsrfRejection(error)) {
        return throwError(() => error);
      }

      csrfService.invalidate();
      return csrfService
        .ensureToken()
        .pipe(switchMap(() => next(addToken(request, csrfService.getToken()))));
    }),
  );
};

function addToken(request: HttpRequest<unknown>, csrfToken: string | null) {
  if (!csrfToken) {
    return request;
  }

  return request.clone({ headers: request.headers.set(CSRF_HEADER, csrfToken) });
}

function isCsrfRejection(error: unknown): error is HttpErrorResponse {
  return error instanceof HttpErrorResponse && error.status === 403;
}

function isBackendRequest(url: string): boolean {
  try {
    return new URL(url).origin === new URL(API_BASE_URL).origin;
  } catch {
    return false;
  }
}
