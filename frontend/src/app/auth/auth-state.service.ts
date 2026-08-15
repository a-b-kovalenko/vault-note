import { computed, Injectable, signal } from '@angular/core';
import { Observable, finalize, of, shareReplay, tap } from 'rxjs';

import { LoginResponse, UserProfile } from './auth.models';

@Injectable({ providedIn: 'root' })
export class AuthStateService {
  private readonly sessionState = signal<LoginResponse | null>(null);
  private readonly profileState = signal<UserProfile | null>(null);
  private profileRequest: Observable<UserProfile> | null = null;

  readonly session = this.sessionState.asReadonly();
  readonly profile = this.profileState.asReadonly();
  readonly accessToken = computed(() => this.sessionState()?.accessToken ?? null);
  readonly isAuthenticated = computed(() => this.accessToken() !== null);

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
    this.sessionState.set(null);
    this.profileState.set(null);
    this.profileRequest = null;
  }
}
