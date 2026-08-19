import { of } from 'rxjs';

import { AuthStateService } from './auth-state.service';
import { LoginResponse, UserProfile } from './auth.models';

describe('AuthStateService', () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

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

  it('broadcasts session invalidation without exposing token values', () => {
    const postMessage = vi.fn();
    const addEventListener = vi.fn();
    const removeEventListener = vi.fn();
    const close = vi.fn();
    const channel = { addEventListener, removeEventListener, postMessage, close };
    const broadcastChannel = vi.fn(function () {
      return channel;
    });
    vi.stubGlobal('BroadcastChannel', broadcastChannel);

    const service = new AuthStateService();
    service.startSessionSynchronization();
    service.setSession({
      accessToken: 'access-token',
      tokenType: 'Bearer',
      expiresIn: 900,
    });

    service.clearSession();

    expect(broadcastChannel).toHaveBeenCalledWith('vaultnote-auth-session');
    expect(postMessage).toHaveBeenCalledWith({ type: 'session-invalidated' });
    expect(JSON.stringify(postMessage.mock.calls)).not.toContain('access-token');
  });

  it('clears local state when another tab broadcasts session invalidation', () => {
    let messageHandler: ((event: MessageEvent<unknown>) => void) | undefined;
    const channel = {
      addEventListener: vi.fn((_type: string, handler: (event: MessageEvent<unknown>) => void) => {
        messageHandler = handler;
      }),
      removeEventListener: vi.fn(),
      postMessage: vi.fn(),
      close: vi.fn(),
    };
    vi.stubGlobal(
      'BroadcastChannel',
      vi.fn(function () {
        return channel;
      }),
    );

    const service = new AuthStateService();
    service.startSessionSynchronization();
    service.setSession({
      accessToken: 'access-token',
      tokenType: 'Bearer',
      expiresIn: 900,
    });

    messageHandler?.({ data: { type: 'session-invalidated' } } as MessageEvent<unknown>);

    expect(service.isAuthenticated()).toBe(false);
    expect(service.accessToken()).toBe(null);
    expect(channel.postMessage).not.toHaveBeenCalled();
  });

  it('ignores unrelated broadcast messages', () => {
    let messageHandler: ((event: MessageEvent<unknown>) => void) | undefined;
    const channel = {
      addEventListener: vi.fn((_type: string, handler: (event: MessageEvent<unknown>) => void) => {
        messageHandler = handler;
      }),
      removeEventListener: vi.fn(),
      postMessage: vi.fn(),
      close: vi.fn(),
    };
    vi.stubGlobal(
      'BroadcastChannel',
      vi.fn(function () {
        return channel;
      }),
    );

    const service = new AuthStateService();
    service.startSessionSynchronization();
    service.setSession({
      accessToken: 'access-token',
      tokenType: 'Bearer',
      expiresIn: 900,
    });

    messageHandler?.({ data: { type: 'other-event' } } as MessageEvent<unknown>);

    expect(service.isAuthenticated()).toBe(true);
    expect(channel.postMessage).not.toHaveBeenCalled();
  });
});
