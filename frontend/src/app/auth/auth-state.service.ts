import { computed, Injectable, signal } from '@angular/core';

import { LoginResponse } from './auth.models';

@Injectable({ providedIn: 'root' })
export class AuthStateService {
  private readonly sessionState = signal<LoginResponse | null>(null);

  readonly session = this.sessionState.asReadonly();
  readonly accessToken = computed(() => this.sessionState()?.accessToken ?? null);
  readonly isAuthenticated = computed(() => this.accessToken() !== null);

  setSession(session: LoginResponse): void {
    this.sessionState.set(session);
  }

  clearSession(): void {
    this.sessionState.set(null);
  }
}
