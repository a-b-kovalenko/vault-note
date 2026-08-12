import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { map, Observable, switchMap } from 'rxjs';

import { API_BASE_URL } from '../api/api-config';
import { CsrfService } from './csrf.service';
import { LoginApiResponse, LoginRequest, LoginResponse } from './auth.models';

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

  private static toLoginResponse(response: LoginApiResponse): LoginResponse {
    return {
      accessToken: response.access_token,
      tokenType: response.token_type,
      expiresIn: response.expires_in,
    };
  }
}
