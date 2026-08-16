import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { Observable, of, throwError } from 'rxjs';

import { AvatarStateService } from '../avatar/avatar-state.service';
import { AuthApiService } from './auth-api.service';
import { AuthRefreshService } from './auth-refresh.service';
import { AuthStateService } from './auth-state.service';
import { LoginResponse, UserProfile } from './auth.models';
import { OAuthCallbackPage } from './oauth-callback-page';

describe('OAuthCallbackPage', () => {
  let fixture: ComponentFixture<OAuthCallbackPage>;
  let page: OAuthCallbackPage;
  let refreshResponse: Observable<LoginResponse>;
  let profileResponse: Observable<UserProfile>;
  let refresh: ReturnType<typeof vi.fn>;
  let profile: ReturnType<typeof vi.fn>;
  let navigate: ReturnType<typeof vi.fn>;
  let avatarLoad: ReturnType<typeof vi.fn>;
  let authState: AuthStateService;

  beforeEach(async () => {
    refreshResponse = of({
      accessToken: 'access-token',
      tokenType: 'Bearer',
      expiresIn: 900,
    });
    profileResponse = of({
      id: 10,
      email: 'user@example.com',
      displayName: 'OAuth User',
      emailVerified: true,
      roles: ['USER'],
    });
    refresh = vi.fn(() => refreshResponse);
    profile = vi.fn(() => profileResponse);
    navigate = vi.fn().mockResolvedValue(true);
    avatarLoad = vi.fn(() => of(void 0));

    await TestBed.configureTestingModule({
      imports: [OAuthCallbackPage],
      providers: [
        { provide: AvatarStateService, useValue: { load: avatarLoad } },
        { provide: AuthApiService, useValue: { profile } },
        { provide: AuthRefreshService, useValue: { refresh } },
        { provide: Router, useValue: { navigate } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(OAuthCallbackPage);
    page = fixture.componentInstance;
    authState = TestBed.inject(AuthStateService);
  });

  it('refreshes the session, loads the profile, and navigates to me', () => {
    fixture.detectChanges();

    expect(refresh).toHaveBeenCalledOnce();
    expect(profile).toHaveBeenCalledOnce();
    expect(avatarLoad).toHaveBeenCalledOnce();
    expect(authState.profile()).toEqual({
      id: 10,
      email: 'user@example.com',
      displayName: 'OAuth User',
      emailVerified: true,
      roles: ['USER'],
    });
    expect(page.isLoading()).toBe(false);
    expect(navigate).toHaveBeenCalledWith(['/me']);
  });

  it('clears the session and returns to login when the callback cannot be completed', () => {
    refreshResponse = throwError(() => new Error('refresh failed'));
    fixture.detectChanges();

    expect(page.isLoading()).toBe(false);
    expect(authState.accessToken()).toBeNull();
    expect(profile).not.toHaveBeenCalled();
    expect(navigate).toHaveBeenCalledWith(['/login'], {
      queryParams: { error: 'oauth' },
    });
  });

  it('continues to the application when the avatar cannot be loaded', () => {
    avatarLoad.mockReturnValue(throwError(() => new Error('avatar unavailable')));
    fixture.detectChanges();

    expect(page.isLoading()).toBe(false);
    expect(navigate).toHaveBeenCalledWith(['/me']);
  });
});
