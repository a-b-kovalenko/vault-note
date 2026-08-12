import {
  HttpClient,
  HttpErrorResponse,
  provideHttpClient,
  withInterceptors,
  withNoXsrfProtection,
} from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Observable, of, throwError } from 'rxjs';

import { API_BASE_URL } from '../api/api-config';
import { AuthRefreshService } from './auth-refresh.service';
import { authInterceptor } from './auth.interceptor';
import { AuthStateService } from './auth-state.service';
import { LoginResponse } from './auth.models';

describe('authInterceptor', () => {
  let httpMock: HttpTestingController;
  let authState: AuthStateService;
  let refresh: ReturnType<typeof vi.fn>;

  beforeEach(() => {
    refresh = vi.fn((): Observable<LoginResponse> =>
      of({
        accessToken: 'refreshed-access-token',
        tokenType: 'Bearer',
        expiresIn: 900,
      }),
    );

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withNoXsrfProtection(), withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        AuthStateService,
        { provide: AuthRefreshService, useValue: { refresh } },
      ],
    });

    httpMock = TestBed.inject(HttpTestingController);
    authState = TestBed.inject(AuthStateService);
  });

  afterEach(() => httpMock.verify());

  it('adds the in-memory bearer token and credentials to backend requests', () => {
    authState.setSession({
      accessToken: 'access-token',
      tokenType: 'Bearer',
      expiresIn: 900,
    });

    httpClient().get(`${API_BASE_URL}/api/v1/auth/me`).subscribe();

    const request = httpMock.expectOne(`${API_BASE_URL}/api/v1/auth/me`);
    expect(request.request.headers.get('Authorization')).toBe('Bearer access-token');
    expect(request.request.withCredentials).toBe(true);
    request.flush({});
  });

  it('refreshes once after a 401 and retries with the new access token', () => {
    let response: unknown;
    httpClient()
      .get(`${API_BASE_URL}/api/v1/auth/me`)
      .subscribe((value) => (response = value));

    const initialRequest = httpMock.expectOne(`${API_BASE_URL}/api/v1/auth/me`);
    initialRequest.flush({}, { status: 401, statusText: 'Unauthorized' });

    expect(refresh).toHaveBeenCalledTimes(1);
    const retryRequest = httpMock.expectOne(`${API_BASE_URL}/api/v1/auth/me`);
    expect(retryRequest.request.headers.get('Authorization')).toBe('Bearer refreshed-access-token');
    retryRequest.flush({ user_id: 1 });

    expect(response).toEqual({ user_id: 1 });
  });

  it('does not refresh a failed public login request', () => {
    let error: unknown;
    authState.setSession({
      accessToken: 'stale-access-token',
      tokenType: 'Bearer',
      expiresIn: 900,
    });

    httpClient()
      .post(`${API_BASE_URL}/api/v1/auth/login`, {})
      .subscribe({ error: (value) => (error = value) });

    const request = httpMock.expectOne(`${API_BASE_URL}/api/v1/auth/login`);
    expect(request.request.headers.has('Authorization')).toBe(false);
    request.flush({}, { status: 401, statusText: 'Unauthorized' });

    expect(refresh).not.toHaveBeenCalled();
    expect(error).toBeInstanceOf(HttpErrorResponse);
  });

  it('clears the session when refresh fails', () => {
    const refreshError = new HttpErrorResponse({ status: 401, statusText: 'Unauthorized' });
    refresh.mockReturnValue(throwError(() => refreshError));
    authState.setSession({
      accessToken: 'expired-access-token',
      tokenType: 'Bearer',
      expiresIn: 900,
    });

    httpClient()
      .get(`${API_BASE_URL}/api/v1/auth/me`)
      .subscribe({ error: () => undefined });

    httpMock.expectOne(`${API_BASE_URL}/api/v1/auth/me`).flush(
      {},
      {
        status: 401,
        statusText: 'Unauthorized',
      },
    );

    expect(authState.isAuthenticated()).toBe(false);
    expect(authState.accessToken()).toBe(null);
  });

  function httpClient() {
    return TestBed.inject(HttpClient);
  }
});
