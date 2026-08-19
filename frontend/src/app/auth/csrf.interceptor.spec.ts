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

describe('csrfInterceptor', () => {
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withNoXsrfProtection(), withInterceptors([csrfInterceptor])),
        provideHttpClientTesting(),
      ],
    });

    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    vi.restoreAllMocks();
  });

  it('bootstraps the CSRF token in memory before unsafe backend requests', () => {
    let response: unknown;
    httpMockClient()
      .post(`${API_BASE_URL}/api/v1/auth/login`, {})
      .subscribe((value) => {
        response = value;
      });

    httpMock.expectOne(`${API_BASE_URL}/csrf`).flush({ token: 'csrf token' });

    const request = httpMock.expectOne(`${API_BASE_URL}/api/v1/auth/login`);
    expect(request.request.headers.get('X-XSRF-TOKEN')).toBe('csrf token');

    request.flush({});

    expect(response).toEqual({});
  });

  it('does not add the token to safe or external requests', () => {
    httpMockClient().get(`${API_BASE_URL}/api/v1/users/me`).subscribe();
    const safeRequest = httpMock.expectOne(`${API_BASE_URL}/api/v1/users/me`);
    expect(safeRequest.request.headers.has('X-XSRF-TOKEN')).toBe(false);
    safeRequest.flush({});

    httpMockClient().post('https://example.test/api', {}).subscribe();
    const externalRequest = httpMock.expectOne('https://example.test/api');
    expect(externalRequest.request.headers.has('X-XSRF-TOKEN')).toBe(false);
    externalRequest.flush({});
  });

  it('refreshes the in-memory token once after a CSRF rejection', () => {
    httpMockClient().post(`${API_BASE_URL}/api/v1/users/me`, {}).subscribe();

    httpMock.expectOne(`${API_BASE_URL}/csrf`).flush({ token: 'expired-token' });

    const initialRequest = httpMock.expectOne(`${API_BASE_URL}/api/v1/users/me`);
    expect(initialRequest.request.headers.get('X-XSRF-TOKEN')).toBe('expired-token');
    initialRequest.flush({ code: 'CSRF_TOKEN_INVALID' }, { status: 403, statusText: 'Forbidden' });

    httpMock.expectOne(`${API_BASE_URL}/csrf`).flush({ token: 'fresh-token' });

    const retryRequest = httpMock.expectOne(`${API_BASE_URL}/api/v1/users/me`);
    expect(retryRequest.request.headers.get('X-XSRF-TOKEN')).toBe('fresh-token');
    retryRequest.flush({});
  });

  function httpMockClient() {
    return TestBed.inject(HttpClient);
  }
});
