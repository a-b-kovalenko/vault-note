import { HttpErrorResponse, HttpHeaders } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';

import { RateLimitState } from './rate-limit-state';

describe('RateLimitState', () => {
  beforeEach(() => {
    vi.useFakeTimers();
    TestBed.configureTestingModule({ providers: [RateLimitState] });
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('starts and decrements the countdown from Retry-After', () => {
    const state = TestBed.inject(RateLimitState);

    expect(state.start(rateLimitError('5'))).toBe(true);
    expect(state.isActive()).toBe(true);
    expect(state.remainingSeconds()).toBe(5);

    vi.advanceTimersByTime(2000);

    expect(state.remainingSeconds()).toBe(3);
  });

  it('uses a safe fallback when Retry-After is missing', () => {
    const state = TestBed.inject(RateLimitState);

    state.start(rateLimitError(null));

    expect(state.remainingSeconds()).toBe(60);
  });

  it('ignores errors that are not rate-limit responses', () => {
    const state = TestBed.inject(RateLimitState);

    expect(state.start(new HttpErrorResponse({ status: 401 }))).toBe(false);
    expect(state.isActive()).toBe(false);
  });

  it('resets after the countdown reaches zero', () => {
    const state = TestBed.inject(RateLimitState);

    state.start(rateLimitError('1'));
    vi.advanceTimersByTime(1000);

    expect(state.isActive()).toBe(false);
    expect(state.remainingSeconds()).toBe(0);
  });
});

function rateLimitError(retryAfter: string | null): HttpErrorResponse {
  return new HttpErrorResponse({
    status: 429,
    headers: retryAfter ? new HttpHeaders({ 'Retry-After': retryAfter }) : undefined,
  });
}
