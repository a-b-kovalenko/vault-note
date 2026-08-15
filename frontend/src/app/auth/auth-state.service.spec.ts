import { of } from 'rxjs';

import { AuthStateService } from './auth-state.service';
import { LoginResponse, UserProfile } from './auth.models';

describe('AuthStateService', () => {
  it('keeps the access token in memory and exposes authentication state', () => {
    const service = new AuthStateService();
    const session: LoginResponse = {
      accessToken: 'access-token',
      tokenType: 'Bearer',
      expiresIn: 900,
    };

    expect(service.accessToken()).toBe(null);
    expect(service.isAuthenticated()).toBe(false);

    service.setSession(session);

    expect(service.session()).toEqual(session);
    expect(service.accessToken()).toBe('access-token');
    expect(service.isAuthenticated()).toBe(true);

    service.clearSession();

    expect(service.session()).toBe(null);
    expect(service.accessToken()).toBe(null);
    expect(service.isAuthenticated()).toBe(false);
  });

  it('caches the authenticated profile and clears it with the session', () => {
    const service = new AuthStateService();
    const profile: UserProfile = {
      id: 42,
      email: 'user@example.com',
      displayName: 'Profile User',
      emailVerified: true,
      roles: ['USER'],
    };
    const loader = vi.fn(() => of(profile));

    service.loadProfile(loader).subscribe();
    service.loadProfile(loader).subscribe();

    expect(loader).toHaveBeenCalledOnce();
    expect(service.profile()).toEqual(profile);

    service.clearSession();

    expect(service.profile()).toBe(null);
  });
});
