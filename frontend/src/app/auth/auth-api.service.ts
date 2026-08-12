import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { map, Observable } from 'rxjs';

import { LoginApiResponse, LoginRequest, LoginResponse } from './auth.models';

const API_BASE_URL = 'http://localhost:8080';

@Injectable({ providedIn: 'root' })
export class AuthApiService {
  private readonly http = inject(HttpClient);

  login(request: LoginRequest): Observable<LoginResponse> {
    return this.http
      .post<LoginApiResponse>(`${API_BASE_URL}/api/v1/auth/login`, request)
      .pipe(map(AuthApiService.toLoginResponse));
  }

  private static toLoginResponse(response: LoginApiResponse): LoginResponse {
    return {
      accessToken: response.access_token,
      tokenType: response.token_type,
      expiresIn: response.expires_in,
    };
  }
}
