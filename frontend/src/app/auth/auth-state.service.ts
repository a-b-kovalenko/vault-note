import { computed, Injectable, signal } from '@angular/core';

import { LoginResponse } from './auth.models';

export interface AuthSession {
  accessToken: string;
  tokenType: string;
  expiresAt: number;
}

@Injectable({ providedIn: 'root' })
export class AuthState {
  private readonly sessionState = signal<AuthSession | null>(null);

  readonly session = this.sessionState.asReadonly();
  readonly accessToken = computed(() => this.sessionState()?.accessToken ?? null);
  readonly isAuthenticated = computed(() => this.sessionState() !== null);

  setSession(response: LoginResponse): void {
    this.sessionState.set({
      accessToken: response.accessToken,
      tokenType: response.tokenType,
      expiresAt: Date.now() + response.expiresIn * 1000,
    });
  }

  clear(): void {
    this.sessionState.set(null);
  }
}
