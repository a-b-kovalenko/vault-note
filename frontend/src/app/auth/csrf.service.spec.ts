import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { API_BASE_URL } from '../api/api-config';
import { CsrfService } from './csrf.service';

describe('CsrfService', () => {
  let service: CsrfService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });

    service = TestBed.inject(CsrfService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('shares one bootstrap request and reuses the initialized token', () => {
    const completions: void[] = [];

    service.ensureToken().subscribe((value) => completions.push(value));
    service.ensureToken().subscribe((value) => completions.push(value));

    const request = httpMock.expectOne(`${API_BASE_URL}/csrf`);
    expect(request.request.method).toBe('GET');
    expect(request.request.withCredentials).toBe(true);

    request.flush({ token: 'csrf-token' });

    expect(service.getToken()).toBe('csrf-token');

    service.ensureToken().subscribe((value) => completions.push(value));

    expect(completions).toHaveLength(3);
    httpMock.expectNone(`${API_BASE_URL}/csrf`);
  });

  it('allows a failed bootstrap to be retried', () => {
    let bootstrapError: unknown;
    service.ensureToken().subscribe({ error: (error) => (bootstrapError = error) });

    httpMock.expectOne(`${API_BASE_URL}/csrf`).flush({});

    expect(bootstrapError).toBeInstanceOf(Error);

    service.ensureToken().subscribe();
    expect(httpMock.expectOne(`${API_BASE_URL}/csrf`)).toBeTruthy();
  });

  it('clears the in-memory token when invalidated', () => {
    service.ensureToken().subscribe();
    httpMock.expectOne(`${API_BASE_URL}/csrf`).flush({ token: 'csrf-token' });

    service.invalidate();

    expect(service.getToken()).toBeNull();
  });
});
