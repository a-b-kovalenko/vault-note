import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';

import { API_BASE_URL } from '../api/api-config';
import type { PageUserInfoDto, UserInfoDto } from '../api/generated';

export interface AdminUser {
  id: number;
  email: string;
  displayName: string;
}

export interface AdminUsersPage {
  content: AdminUser[];
  page: number;
  size: number;
  totalPages: number;
  totalElements: number;
  first: boolean;
  last: boolean;
}

@Injectable({ providedIn: 'root' })
export class AdminUsersApiService {
  private readonly http = inject(HttpClient);

  list(page: number, size: number): Observable<AdminUsersPage> {
    return this.http
      .get<PageUserInfoDto>(`${API_BASE_URL}/api/v1/users`, {
        params: {
          page,
          size,
          sort: 'displayName,asc',
        },
        withCredentials: true,
      })
      .pipe(map((response) => AdminUsersApiService.toPage(response, page, size)));
  }

  private static toPage(response: PageUserInfoDto, page: number, size: number): AdminUsersPage {
    const currentPage = response.number ?? page;
    const totalPages = response.totalPages ?? 0;

    return {
      content: (response.content ?? []).map(AdminUsersApiService.toUser),
      page: currentPage,
      size: response.size ?? size,
      totalPages,
      totalElements: response.totalElements ?? response.content?.length ?? 0,
      first: response.first ?? currentPage === 0,
      last: response.last ?? (totalPages === 0 || currentPage >= totalPages - 1),
    };
  }

  private static toUser(user: UserInfoDto): AdminUser {
    if (user.id === undefined || user.email === undefined || user.display_name === undefined) {
      throw new Error('The administrator user response did not contain a complete user.');
    }

    return {
      id: user.id,
      email: user.email,
      displayName: user.display_name,
    };
  }
}
