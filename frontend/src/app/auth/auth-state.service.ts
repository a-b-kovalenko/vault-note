import { computed, Injectable, OnDestroy, signal } from '@angular/core';
import { Observable, Subject, finalize, of, shareReplay, tap } from 'rxjs';

import { LoginResponse, UserProfile } from './auth.models';

const AUTH_SESSION_CHANNEL_NAME = 'vaultnote-auth-session';
const SESSION_INVALIDATED_MESSAGE = 'session-invalidated';

@Injectable({ providedIn: 'root' })
export class AuthStateService implements OnDestroy {
  private readonly sessionState = signal<LoginResponse | null>(null);
  private readonly profileState = signal<UserProfile | null>(null);
  private readonly sessionInvalidatedSubject = new Subject<void>();
  private profileRequest: Observable<UserProfile> | null = null;
  private sessionChannel: BroadcastChannel | null = null;
  private sessionSynchronizationStarted = false;

  readonly session = this.sessionState.asReadonly();
  readonly profile = this.profileState.asReadonly();
  readonly sessionInvalidated$ = this.sessionInvalidatedSubject.asObservable();
  readonly accessToken = computed(() => this.sessionState()?.accessToken ?? null);
  readonly isAuthenticated = computed(() => this.accessToken() !== null);

  startSessionSynchronization(): void {
    if (this.sessionSynchronizationStarted) {
      return;
    }

    this.sessionSynchronizationStarted = true;
    if (typeof BroadcastChannel === 'undefined') {
      return;
    }

    try {
      this.sessionChannel = new BroadcastChannel(AUTH_SESSION_CHANNEL_NAME);
      this.sessionChannel.addEventListener('message', this.onSessionMessage);
    } catch {
      this.sessionChannel = null;
    }
  }

  setSession(session: LoginResponse): void {
    this.sessionState.set(session);
  }

  loadProfile(loader: () => Observable<UserProfile>): Observable<UserProfile> {
    const cachedProfile = this.profileState();
    if (cachedProfile) {
      return of(cachedProfile);
    }

    if (!this.profileRequest) {
      this.profileRequest = loader().pipe(
        tap((profile) => this.profileState.set(profile)),
        finalize(() => (this.profileRequest = null)),
        shareReplay({ bufferSize: 1, refCount: false }),
      );
    }

    return this.profileRequest;
  }

  setProfile(profile: UserProfile): void {
    this.profileState.set(profile);
  }

  clearSession(): void {
    this.clearSessionState();
    this.sessionInvalidatedSubject.next();
    this.sessionChannel?.postMessage({ type: SESSION_INVALIDATED_MESSAGE });
  }

  ngOnDestroy(): void {
    this.sessionChannel?.removeEventListener('message', this.onSessionMessage);
    this.sessionChannel?.close();
    this.sessionChannel = null;
  }

  private readonly onSessionMessage = (event: MessageEvent<unknown>): void => {
    if (!isSessionInvalidatedMessage(event.data)) {
      return;
    }

    this.clearSessionState();
    this.sessionInvalidatedSubject.next();
  };

  private clearSessionState(): void {
    this.sessionState.set(null);
    this.profileState.set(null);
    this.profileRequest = null;
  }
}

function isSessionInvalidatedMessage(value: unknown): value is { type: string } {
  return (
    typeof value === 'object' &&
    value !== null &&
    'type' in value &&
    value.type === SESSION_INVALIDATED_MESSAGE
  );
}
