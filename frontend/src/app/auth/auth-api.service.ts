import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { map, Observable, switchMap } from 'rxjs';

import { API_BASE_URL } from '../api/api-config';
import { CsrfService } from './csrf.service';
import {
  LoginApiResponse,
  LoginRequest,
  LoginResponse,
  PasswordResetConfirmRequest,
  PasswordResetRequest,
  RegisterUserApiResponse,
  RegisterUserRequest,
  RegisterUserResponse,
  UpdateUserProfileRequest,
  UserProfile,
  UserProfileApiResponse,
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

  profile(): Observable<UserProfile> {
    return this.http
      .get<UserProfileApiResponse>(`${API_BASE_URL}/api/v1/users/me`, {
        withCredentials: true,
      })
      .pipe(map(AuthApiService.toUserProfile));
  }

  updateProfile(request: UpdateUserProfileRequest): Observable<UserProfile> {
    return this.csrfService.ensureToken().pipe(
      switchMap(() =>
        this.http.patch<UserProfileApiResponse>(
          `${API_BASE_URL}/api/v1/users/me`,
          { display_name: request.displayName },
          { withCredentials: true },
        ),
      ),
      map(AuthApiService.toUserProfile),
    );
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

  private static toUserProfile(response: UserProfileApiResponse): UserProfile {
    if (
      response.id === undefined ||
      response.email === undefined ||
      response.display_name === undefined ||
      response.email_verified === undefined ||
      response.roles === undefined
    ) {
      throw new Error('The user-profile response did not contain a complete profile.');
    }

    return {
      id: response.id,
      email: response.email,
      displayName: response.display_name,
      emailVerified: response.email_verified,
      roles: response.roles,
    };
  }
}
