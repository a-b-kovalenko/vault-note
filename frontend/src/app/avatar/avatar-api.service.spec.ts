import { provideHttpClient, withInterceptors, withNoXsrfProtection } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { API_BASE_URL } from '../api/api-config';
import { csrfInterceptor } from '../auth/csrf.interceptor';
import { AvatarApiService } from './avatar-api.service';

describe('AvatarApiService', () => {
  let service: AvatarApiService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withNoXsrfProtection(), withInterceptors([csrfInterceptor])),
        provideHttpClientTesting(),
      ],
    });

    service = TestBed.inject(AvatarApiService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    vi.restoreAllMocks();
  });

  it('loads the current avatar as a Blob', () => {
    let response: Blob | undefined;

    service.getAvatar().subscribe((value) => (response = value));

    const request = httpMock.expectOne(API_BASE_URL + '/api/v1/users/me/avatar');
    expect(request.request.method).toBe('GET');
    expect(request.request.responseType).toBe('blob');
    expect(request.request.withCredentials).toBe(true);
    const content = new Blob(['avatar'], { type: 'image/jpeg' });
    request.flush(content);

    expect(response).toBe(content);
  });

  it('bootstraps CSRF before uploading an avatar', () => {
    vi.spyOn(document, 'cookie', 'get').mockReturnValue('XSRF-TOKEN=csrf-token');
    const file = new File(['avatar'], 'avatar.png', { type: 'image/png' });
    let response: { byte_size: number } | undefined;

    service.uploadAvatar(file).subscribe((value) => (response = value));

    httpMock.expectOne(API_BASE_URL + '/csrf').flush({ token: 'csrf-token' });

    const request = httpMock.expectOne(API_BASE_URL + '/api/v1/users/me/avatar');
    expect(request.request.method).toBe('PUT');
    expect(request.request.withCredentials).toBe(true);
    expect(request.request.headers.get('X-XSRF-TOKEN')).toBe('csrf-token');
    expect(request.request.body).toBeInstanceOf(FormData);
    expect((request.request.body as FormData).get('file')).toBe(file);
    request.flush({ byte_size: 123 });

    expect(response).toEqual({ byte_size: 123 });
  });

  it('bootstraps CSRF before removing an avatar', () => {
    vi.spyOn(document, 'cookie', 'get').mockReturnValue('XSRF-TOKEN=csrf-token');
    let completed = false;

    service.removeAvatar().subscribe({ complete: () => (completed = true) });

    httpMock.expectOne(API_BASE_URL + '/csrf').flush({ token: 'csrf-token' });

    const request = httpMock.expectOne(API_BASE_URL + '/api/v1/users/me/avatar');
    expect(request.request.method).toBe('DELETE');
    expect(request.request.withCredentials).toBe(true);
    expect(request.request.headers.get('X-XSRF-TOKEN')).toBe('csrf-token');
    request.flush(null);

    expect(completed).toBe(true);
  });
});
