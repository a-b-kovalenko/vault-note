import {
  HttpClient,
  provideHttpClient,
  withInterceptors,
  withNoXsrfProtection,
} from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { API_BASE_URL } from '../api/api-config';
import { csrfInterceptor } from './csrf.interceptor';
import { AuthApiService } from './auth-api.service';
import { LoginResponse } from './auth.models';

describe('AuthApiService', () => {
  let service: AuthApiService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withNoXsrfProtection(), withInterceptors([csrfInterceptor])),
        provideHttpClientTesting(),
      ],
    });

    service = TestBed.inject(AuthApiService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    vi.restoreAllMocks();
  });

  it('bootstraps CSRF before login and maps the generated API response', () => {
    vi.spyOn(document, 'cookie', 'get').mockReturnValue('XSRF-TOKEN=csrf-token');
    let response: LoginResponse | undefined;

    service.login({ email: 'user@example.com', password: 'password' }).subscribe((value) => {
      response = value;
    });

    const csrfRequest = httpMock.expectOne(`${API_BASE_URL}/csrf`);
    expect(csrfRequest.request.withCredentials).toBe(true);
    csrfRequest.flush({ token: 'csrf-token' });

    const loginRequest = httpMock.expectOne(`${API_BASE_URL}/api/v1/auth/login`);
    expect(loginRequest.request.withCredentials).toBe(true);
    expect(loginRequest.request.headers.get('X-XSRF-TOKEN')).toBe('csrf-token');
    expect(loginRequest.request.body).toEqual({
      email: 'user@example.com',
      password: 'password',
    });
    loginRequest.flush({
      access_token: 'access-token',
      token_type: 'Bearer',
      expires_in: 900,
    });

    expect(response).toEqual({
      accessToken: 'access-token',
      tokenType: 'Bearer',
      expiresIn: 900,
    });
  });

  it('uses the CSRF cookie and refresh endpoint for a session refresh', () => {
    vi.spyOn(document, 'cookie', 'get').mockReturnValue('XSRF-TOKEN=csrf-token');
    let response: LoginResponse | undefined;

    service.refresh().subscribe((value) => {
      response = value;
    });

    httpMock.expectOne(`${API_BASE_URL}/csrf`).flush({ token: 'csrf-token' });

    const refreshRequest = httpMock.expectOne(`${API_BASE_URL}/api/v1/auth/refresh`);
    expect(refreshRequest.request.method).toBe('POST');
    expect(refreshRequest.request.withCredentials).toBe(true);
    expect(refreshRequest.request.headers.get('X-XSRF-TOKEN')).toBe('csrf-token');
    refreshRequest.flush({
      access_token: 'refreshed-access-token',
      token_type: 'Bearer',
      expires_in: 900,
    });

    expect(response?.accessToken).toBe('refreshed-access-token');
  });
});
