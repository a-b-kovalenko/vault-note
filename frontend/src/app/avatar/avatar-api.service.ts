import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable, switchMap } from 'rxjs';

import { API_BASE_URL } from '../api/api-config';
import { CsrfService } from '../auth/csrf.service';

export interface AvatarUploadResponse {
  byte_size: number;
}

@Injectable({ providedIn: 'root' })
export class AvatarApiService {
  private readonly http = inject(HttpClient);
  private readonly csrfService = inject(CsrfService);

  getAvatar(): Observable<Blob> {
    return this.http.get(`${API_BASE_URL}/api/v1/users/me/avatar`, {
      responseType: 'blob',
      withCredentials: true,
    });
  }

  uploadAvatar(file: File): Observable<AvatarUploadResponse> {
    const formData = new FormData();
    formData.append('file', file);

    return this.csrfService
      .ensureToken()
      .pipe(
        switchMap(() =>
          this.http.put<AvatarUploadResponse>(`${API_BASE_URL}/api/v1/users/me/avatar`, formData, {
            withCredentials: true,
          }),
        ),
      );
  }

  removeAvatar(): Observable<void> {
    return this.csrfService.ensureToken().pipe(
      switchMap(() =>
        this.http.delete<void>(`${API_BASE_URL}/api/v1/users/me/avatar`, {
          withCredentials: true,
        }),
      ),
    );
  }
}
