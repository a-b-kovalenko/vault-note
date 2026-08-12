import { AuthStateService } from './auth-state.service';
import { LoginResponse } from './auth.models';

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
});
