import {
  HttpErrorResponse,
  HttpEvent,
  HttpHandlerFn,
  HttpInterceptorFn,
  HttpRequest,
} from '@angular/common/http';
import { inject } from '@angular/core';
import { Observable, catchError, switchMap, throwError } from 'rxjs';

import { API_BASE_URL } from '../api/api-config';
import { AuthRefreshService } from './auth-refresh.service';
import { AuthStateService } from './auth-state.service';

const PUBLIC_AUTH_PATHS = new Set([
  '/api/v1/auth/registrations',
  '/api/v1/auth/email-verification',
  '/api/v1/auth/password-reset/request',
  '/api/v1/auth/password-reset/confirm',
  '/api/v1/auth/login',
  '/api/v1/auth/refresh',
  '/api/v1/auth/logout',
  '/csrf',
]);

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  if (!isBackendRequest(request.url)) {
    return next(request);
  }

  const authState = inject(AuthStateService);
  const authRefreshService = inject(AuthRefreshService);
  const requestPath = new URL(request.url, API_BASE_URL).pathname;
  const isPublicAuthRequest = PUBLIC_AUTH_PATHS.has(requestPath);
  const accessToken = isPublicAuthRequest ? null : authState.accessToken();

  if (!isPublicAuthRequest && !accessToken && !request.headers.has('Authorization')) {
    return refreshAndSend(request, next, authRefreshService, authState);
  }

  const requestWithAuth = addAuthentication(request, accessToken);

  return next(requestWithAuth).pipe(
    catchError((error: unknown) => {
      if (!shouldRefresh(request.url, error)) {
        return throwError(() => error);
      }

      return refreshAndSend(request, next, authRefreshService, authState);
    }),
  );
};

function refreshAndSend(
  request: HttpRequest<unknown>,
  next: HttpHandlerFn,
  authRefreshService: AuthRefreshService,
  authState: AuthStateService,
): Observable<HttpEvent<unknown>> {
  return authRefreshService.refresh().pipe(
    catchError((refreshError: unknown) => {
      authState.clearSession();
      return throwError(() => refreshError);
    }),
    switchMap((session) => next(addAuthentication(request, session.accessToken, true))),
  );
}

function addAuthentication(
  request: HttpRequest<unknown>,
  accessToken: string | null,
  replaceExisting = false,
) {
  const headers =
    accessToken && (replaceExisting || !request.headers.has('Authorization'))
      ? request.headers.set('Authorization', `Bearer ${accessToken}`)
      : request.headers;

  return request.clone({ withCredentials: true, headers });
}

function shouldRefresh(url: string, error: unknown): error is HttpErrorResponse {
  if (!(error instanceof HttpErrorResponse) || error.status !== 401) {
    return false;
  }

  return !PUBLIC_AUTH_PATHS.has(new URL(url, API_BASE_URL).pathname);
}

function isBackendRequest(url: string): boolean {
  try {
    return new URL(url).origin === new URL(API_BASE_URL).origin;
  } catch {
    return false;
  }
}
