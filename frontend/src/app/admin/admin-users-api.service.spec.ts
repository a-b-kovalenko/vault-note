import { provideHttpClient, withNoXsrfProtection } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { API_BASE_URL } from '../api/api-config';
import { AdminUsersApiService } from './admin-users-api.service';

describe('AdminUsersApiService', () => {
  let service: AdminUsersApiService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withNoXsrfProtection()), provideHttpClientTesting()],
    });

    service = TestBed.inject(AdminUsersApiService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('requests and maps a paginated administrator user list', () => {
    let response:
      | {
          content: Array<{ id: number; email: string; displayName: string }>;
          page: number;
          size: number;
          totalPages: number;
          totalElements: number;
          first: boolean;
          last: boolean;
        }
      | undefined;

    service.list(1, 20).subscribe((value) => (response = value));

    const request = httpMock.expectOne((request) => request.url === `${API_BASE_URL}/api/v1/users`);
    expect(request.request.method).toBe('GET');
    expect(request.request.withCredentials).toBe(true);
    expect(request.request.params.get('page')).toBe('1');
    expect(request.request.params.get('size')).toBe('20');
    expect(request.request.params.get('sort')).toBe('displayName,asc');
    request.flush({
      number: 1,
      size: 20,
      totalPages: 3,
      totalElements: 42,
      first: false,
      last: false,
      content: [{ id: 7, email: 'admin@example.com', display_name: 'Admin User' }],
    });

    expect(response).toEqual({
      content: [{ id: 7, email: 'admin@example.com', displayName: 'Admin User' }],
      page: 1,
      size: 20,
      totalPages: 3,
      totalElements: 42,
      first: false,
      last: false,
    });
  });
});
