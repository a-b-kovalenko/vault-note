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
import { LoginResponse, RegisterUserResponse, UserProfile } from './auth.models';

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

  it('uses the in-memory CSRF token and refresh endpoint for a session refresh', () => {
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

  it('bootstraps CSRF before registration and maps the response', () => {
    let response: RegisterUserResponse | undefined;

    service
      .register({
        email: 'new-user@example.com',
        displayName: 'New User',
        password: 'Password1234',
      })
      .subscribe((value) => {
        response = value;
      });

    const csrfRequest = httpMock.expectOne(`${API_BASE_URL}/csrf`);
    csrfRequest.flush({ token: 'csrf-token' });

    const registrationRequest = httpMock.expectOne(`${API_BASE_URL}/api/v1/auth/registrations`);
    expect(registrationRequest.request.method).toBe('POST');
    expect(registrationRequest.request.withCredentials).toBe(true);
    expect(registrationRequest.request.headers.get('X-XSRF-TOKEN')).toBe('csrf-token');
    expect(registrationRequest.request.body).toEqual({
      email: 'new-user@example.com',
      display_name: 'New User',
      password: 'Password1234',
    });
    registrationRequest.flush({ userId: 7 });

    expect(response).toEqual({ userId: 7 });
  });

  it('sends the email verification token without requiring a session', () => {
    let completed = false;

    service.verifyEmail('raw-token').subscribe({ complete: () => (completed = true) });

    httpMock.expectOne(`${API_BASE_URL}/csrf`).flush({ token: 'csrf-token' });

    const request = httpMock.expectOne(
      `${API_BASE_URL}/api/v1/auth/email-verification?token=raw-token`,
    );
    expect(request.request.method).toBe('POST');
    expect(request.request.withCredentials).toBe(true);
    expect(request.request.body).toBe(null);
    request.flush(null);

    expect(completed).toBe(true);
  });

  it('bootstraps CSRF before requesting a password reset', () => {
    let completed = false;

    service.requestPasswordReset({ email: 'user@example.com' }).subscribe({
      complete: () => (completed = true),
    });

    httpMock.expectOne(`${API_BASE_URL}/csrf`).flush({ token: 'csrf-token' });

    const request = httpMock.expectOne(`${API_BASE_URL}/api/v1/auth/password-reset/request`);
    expect(request.request.method).toBe('POST');
    expect(request.request.withCredentials).toBe(true);
    expect(request.request.headers.get('X-XSRF-TOKEN')).toBe('csrf-token');
    expect(request.request.body).toEqual({ email: 'user@example.com' });
    request.flush(null);

    expect(completed).toBe(true);
  });

  it('bootstraps CSRF before confirming a password reset', () => {
    let completed = false;

    service
      .confirmPasswordReset({ token: 'raw-token', newPassword: 'NewPassword1234' })
      .subscribe({ complete: () => (completed = true) });

    httpMock.expectOne(`${API_BASE_URL}/csrf`).flush({ token: 'csrf-token' });

    const request = httpMock.expectOne(`${API_BASE_URL}/api/v1/auth/password-reset/confirm`);
    expect(request.request.method).toBe('POST');
    expect(request.request.withCredentials).toBe(true);
    expect(request.request.headers.get('X-XSRF-TOKEN')).toBe('csrf-token');
    expect(request.request.body).toEqual({
      token: 'raw-token',
      new_password: 'NewPassword1234',
    });
    request.flush(null);

    expect(completed).toBe(true);
  });

  it('loads the current profile and maps the API response', () => {
    let response: UserProfile | undefined;

    service.profile().subscribe((value) => {
      response = value;
    });

    const request = httpMock.expectOne(`${API_BASE_URL}/api/v1/users/me`);
    expect(request.request.method).toBe('GET');
    expect(request.request.withCredentials).toBe(true);
    request.flush({
      id: 42,
      email: 'user@example.com',
      display_name: 'Profile User',
      email_verified: true,
      roles: ['USER'],
    });

    expect(response).toEqual({
      id: 42,
      email: 'user@example.com',
      displayName: 'Profile User',
      emailVerified: true,
      roles: ['USER'],
    });
  });

  it('bootstraps CSRF before updating the current profile', () => {
    let response: UserProfile | undefined;

    service.updateProfile({ displayName: 'Updated Profile' }).subscribe((value) => {
      response = value;
    });

    const csrfRequest = httpMock.expectOne(`${API_BASE_URL}/csrf`);
    csrfRequest.flush({ token: 'csrf-token' });

    const request = httpMock.expectOne(`${API_BASE_URL}/api/v1/users/me`);
    expect(request.request.method).toBe('PATCH');
    expect(request.request.withCredentials).toBe(true);
    expect(request.request.headers.get('X-XSRF-TOKEN')).toBe('csrf-token');
    expect(request.request.body).toEqual({ display_name: 'Updated Profile' });
    request.flush({
      id: 42,
      email: 'user@example.com',
      display_name: 'Updated Profile',
      email_verified: true,
      roles: ['USER'],
    });

    expect(response).toEqual({
      id: 42,
      email: 'user@example.com',
      displayName: 'Updated Profile',
      emailVerified: true,
      roles: ['USER'],
    });
  });

  it('bootstraps CSRF before logout and sends credentials', () => {
    let completed = false;

    service.logout().subscribe({ complete: () => (completed = true) });

    const csrfRequest = httpMock.expectOne(`${API_BASE_URL}/csrf`);
    csrfRequest.flush({ token: 'csrf-token' });

    const logoutRequest = httpMock.expectOne(`${API_BASE_URL}/api/v1/auth/logout`);
    expect(logoutRequest.request.method).toBe('POST');
    expect(logoutRequest.request.withCredentials).toBe(true);
    expect(logoutRequest.request.headers.get('X-XSRF-TOKEN')).toBe('csrf-token');
    logoutRequest.flush(null);

    expect(completed).toBe(true);
  });
});
