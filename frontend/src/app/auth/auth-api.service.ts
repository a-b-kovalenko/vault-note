import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { map, Observable, switchMap } from 'rxjs';

import { API_BASE_URL } from '../api/api-config';
import { CsrfService } from './csrf.service';
import {
  CurrentUserApiResponse,
  CurrentUserResponse,
  LoginApiResponse,
  LoginRequest,
  LoginResponse,
  PasswordResetConfirmRequest,
  PasswordResetRequest,
  RegisterUserApiResponse,
  RegisterUserRequest,
  RegisterUserResponse,
} from './auth.models';

@Injectable({ providedIn: 'root' })
export class AuthApiService {
  private readonly http = inject(HttpClient);
  private readonly csrfService = inject(CsrfService);

  login(request: LoginRequest): Observable<LoginResponse> {
    return this.csrfService.ensureToken().pipe(
      switchMap(() =>
        this.http.post<LoginApiResponse>(`${API_BASE_URL}/api/v1/auth/login`, request, {
          withCredentials: true,
        }),
      ),
      map(AuthApiService.toLoginResponse),
    );
  }

  register(request: RegisterUserRequest): Observable<RegisterUserResponse> {
    return this.csrfService.ensureToken().pipe(
      switchMap(() =>
        this.http.post<RegisterUserApiResponse>(
          `${API_BASE_URL}/api/v1/auth/registrations`,
          {
            email: request.email,
            display_name: request.displayName,
            password: request.password,
          },
          {
            withCredentials: true,
          },
        ),
      ),
      map(AuthApiService.toRegisterUserResponse),
    );
  }

  verifyEmail(token: string): Observable<void> {
    return this.http.post<void>(`${API_BASE_URL}/api/v1/auth/email-verification`, null, {
      params: { token },
      withCredentials: true,
    });
  }

  requestPasswordReset(request: PasswordResetRequest): Observable<void> {
    return this.csrfService
      .ensureToken()
      .pipe(
        switchMap(() =>
          this.http.post<void>(
            `${API_BASE_URL}/api/v1/auth/password-reset/request`,
            { email: request.email },
            { withCredentials: true },
          ),
        ),
      );
  }

  confirmPasswordReset(request: PasswordResetConfirmRequest): Observable<void> {
    return this.csrfService
      .ensureToken()
      .pipe(
        switchMap(() =>
          this.http.post<void>(
            `${API_BASE_URL}/api/v1/auth/password-reset/confirm`,
            { token: request.token, new_password: request.newPassword },
            { withCredentials: true },
          ),
        ),
      );
  }

  refresh(): Observable<LoginResponse> {
    return this.csrfService.ensureToken().pipe(
      switchMap(() =>
        this.http.post<LoginApiResponse>(`${API_BASE_URL}/api/v1/auth/refresh`, null, {
          withCredentials: true,
        }),
      ),
      map(AuthApiService.toLoginResponse),
    );
  }

  currentUser(): Observable<CurrentUserResponse> {
    return this.http
      .get<CurrentUserApiResponse>(`${API_BASE_URL}/api/v1/auth/me`, {
        withCredentials: true,
      })
      .pipe(map(AuthApiService.toCurrentUserResponse));
  }

  logout(): Observable<void> {
    return this.csrfService.ensureToken().pipe(
      switchMap(() =>
        this.http.post<void>(`${API_BASE_URL}/api/v1/auth/logout`, null, {
          withCredentials: true,
        }),
      ),
    );
  }

  private static toLoginResponse(response: LoginApiResponse): LoginResponse {
    return {
      accessToken: response.access_token,
      tokenType: response.token_type,
      expiresIn: response.expires_in,
    };
  }

  private static toRegisterUserResponse(response: RegisterUserApiResponse): RegisterUserResponse {
    if (response.userId === undefined) {
      throw new Error('The registration response did not contain a user identity.');
    }

    return {
      userId: response.userId,
    };
  }

  private static toCurrentUserResponse(response: CurrentUserApiResponse): CurrentUserResponse {
    if (response.user_id === undefined || response.roles === undefined) {
      throw new Error('The current-user response did not contain a complete user identity.');
    }

    return {
      userId: response.user_id,
      roles: response.roles,
    };
  }
}
