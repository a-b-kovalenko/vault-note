import { TestBed } from '@angular/core/testing';

import { AuthState } from './auth-state.service';

describe('AuthState', () => {
  let authState: AuthState;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    authState = TestBed.inject(AuthState);
  });

  it('should expose a session after storing a login response', () => {
    const before = Date.now();

    authState.setSession({
      accessToken: 'test-access-token',
      tokenType: 'Bearer',
      expiresIn: 900,
    });

    expect(authState.session()).toEqual({
      accessToken: 'test-access-token',
      tokenType: 'Bearer',
      expiresAt: expect.any(Number),
    });
    expect(authState.accessToken()).toBe('test-access-token');
    expect(authState.isAuthenticated()).toBe(true);
    expect(authState.session()?.expiresAt).toBeGreaterThanOrEqual(before + 900 * 1000);
  });

  it('should clear the in-memory session', () => {
    authState.setSession({
      accessToken: 'test-access-token',
      tokenType: 'Bearer',
      expiresIn: 900,
    });

    authState.clear();

    expect(authState.session()).toBeNull();
    expect(authState.accessToken()).toBeNull();
    expect(authState.isAuthenticated()).toBe(false);
  });
});
