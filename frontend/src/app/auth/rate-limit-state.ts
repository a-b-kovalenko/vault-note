import { HttpErrorResponse, HttpStatusCode } from '@angular/common/http';
import { computed, DestroyRef, inject, Injectable, signal } from '@angular/core';
import { Subscription, timer } from 'rxjs';

const DEFAULT_RETRY_AFTER_SECONDS = 60;

@Injectable()
export class RateLimitState {
  private readonly destroyRef = inject(DestroyRef);
  private countdownSubscription: Subscription | null = null;

  readonly remainingSeconds = signal(0);
  readonly isActive = computed(() => this.remainingSeconds() > 0);

  constructor() {
    this.destroyRef.onDestroy(() => this.reset());
  }

  start(error: unknown): boolean {
    if (!(error instanceof HttpErrorResponse) || error.status !== HttpStatusCode.TooManyRequests) {
      return false;
    }

    this.reset();
    this.remainingSeconds.set(parseRetryAfter(error.headers.get('Retry-After')));
    this.countdownSubscription = timer(1000, 1000).subscribe(() => {
      const remainingSeconds = this.remainingSeconds();

      if (remainingSeconds <= 1) {
        this.reset();
        return;
      }

      this.remainingSeconds.set(remainingSeconds - 1);
    });

    return true;
  }

  reset(): void {
    this.countdownSubscription?.unsubscribe();
    this.countdownSubscription = null;
    this.remainingSeconds.set(0);
  }
}

function parseRetryAfter(value: string | null): number {
  if (!value) {
    return DEFAULT_RETRY_AFTER_SECONDS;
  }

  const seconds = Number(value);
  if (Number.isFinite(seconds) && seconds >= 0) {
    return Math.max(1, Math.ceil(seconds));
  }

  const retryAt = Date.parse(value);
  if (!Number.isNaN(retryAt)) {
    return Math.max(1, Math.ceil((retryAt - Date.now()) / 1000));
  }

  return DEFAULT_RETRY_AFTER_SECONDS;
}
