import { HttpInterceptorFn } from '@angular/common/http';

import { API_BASE_URL } from '../api/api-config';

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

  const csrfToken = readCookie('XSRF-TOKEN');
  if (!csrfToken) {
    return next(request);
  }

  return next(request.clone({ headers: request.headers.set(CSRF_HEADER, csrfToken) }));
};

function isBackendRequest(url: string): boolean {
  try {
    return new URL(url).origin === new URL(API_BASE_URL).origin;
  } catch {
    return false;
  }
}

function readCookie(name: string): string | null {
  if (typeof document === 'undefined') {
    return null;
  }

  const cookiePrefix = `${name}=`;
  const cookie = document.cookie
    .split(';')
    .map((value) => value.trim())
    .find((value) => value.startsWith(cookiePrefix));

  if (!cookie) {
    return null;
  }

  const encodedValue = cookie.slice(cookiePrefix.length);
  try {
    return decodeURIComponent(encodedValue);
  } catch {
    return encodedValue;
  }
}
