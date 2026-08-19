import { HttpErrorResponse } from '@angular/common/http';
import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { Observable, of, throwError } from 'rxjs';

import { AuthApiService } from '../auth/auth-api.service';
import { AuthStateService } from '../auth/auth-state.service';
import { UserProfile } from '../auth/auth.models';
import { AvatarStateService } from '../avatar/avatar-state.service';
import { AuthenticatedShell } from './authenticated-shell';

describe('AuthenticatedShell', () => {
  let fixture: ComponentFixture<AuthenticatedShell>;
  let profileResponse: Observable<UserProfile>;
  let logoutResponse: Observable<void>;
  let authApiService: {
    profile: ReturnType<typeof vi.fn>;
    logout: ReturnType<typeof vi.fn>;
  };
  let avatarState: {
    avatarUrl: ReturnType<typeof signal<string | null>>;
    load: ReturnType<typeof vi.fn>;
    clear: ReturnType<typeof vi.fn>;
  };
  let router: Router;
  let navigate: ReturnType<typeof vi.spyOn>;

  beforeEach(async () => {
    profileResponse = of({
      id: 42,
      email: 'user@example.com',
      displayName: 'Profile User',
      emailVerified: true,
      roles: ['USER'],
    });
    logoutResponse = of(void 0);
    authApiService = {
      profile: vi.fn(() => profileResponse),
      logout: vi.fn(() => logoutResponse),
    };
    avatarState = {
      avatarUrl: signal<string | null>(null),
      load: vi.fn(() => of(void 0)),
      clear: vi.fn(),
    };

    await TestBed.configureTestingModule({
      imports: [AuthenticatedShell],
      providers: [
        { provide: AuthApiService, useValue: authApiService },
        { provide: AvatarStateService, useValue: avatarState },
        provideRouter([]),
      ],
    }).compileComponents();

    router = TestBed.inject(Router);
    navigate = vi.spyOn(router, 'navigate').mockResolvedValue(true);
    fixture = TestBed.createComponent(AuthenticatedShell);
  });

  it('shows the account summary and profile action for a regular user', () => {
    fixture.detectChanges();

    const trigger = fixture.nativeElement.querySelector('.account-trigger') as HTMLButtonElement;
    trigger.click();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('PU');
    expect(fixture.nativeElement.textContent).toContain('Profile User');
    expect(fixture.nativeElement.textContent).toContain('user@example.com');
    expect(fixture.nativeElement.textContent).toContain('Profile');
    expect(fixture.nativeElement.textContent).not.toContain('All users');
    expect(fixture.nativeElement.querySelector('.account-menu-separator')).not.toBeNull();
    expect(fixture.nativeElement.textContent).toContain('Log out');
  });

  it('shows the avatar preview when the user has an avatar', () => {
    avatarState.avatarUrl.set('blob:avatar');

    fixture.detectChanges();

    const image = fixture.nativeElement.querySelector('.account-trigger img') as HTMLImageElement;
    expect(image).not.toBeNull();
    expect(image.src).toContain('blob:avatar');
    expect(fixture.nativeElement.querySelector('.account-trigger .account-avatar span')).toBeNull();
  });

  it('shows the All users link only for an administrator', () => {
    profileResponse = of({
      id: 7,
      email: 'admin@example.com',
      displayName: 'Admin User',
      emailVerified: true,
      roles: ['ADMIN', 'USER'],
    });

    fixture.detectChanges();
    (fixture.nativeElement.querySelector('.account-trigger') as HTMLButtonElement).click();
    fixture.detectChanges();

    const adminLink = fixture.nativeElement.querySelector(
      'a[routerlink="/admin/users"]',
    ) as HTMLAnchorElement | null;

    expect(adminLink).not.toBeNull();
    expect(fixture.nativeElement.textContent).toContain('All users');
  });

  it('logs out, clears the local session, and redirects to login', () => {
    const authState = TestBed.inject(AuthStateService);
    authState.setSession({
      accessToken: 'access-token',
      tokenType: 'Bearer',
      expiresIn: 900,
    });

    fixture.detectChanges();
    (fixture.nativeElement.querySelector('.account-trigger') as HTMLButtonElement).click();
    fixture.detectChanges();
    (fixture.nativeElement.querySelector('.account-menu-item-logout') as HTMLButtonElement).click();

    expect(authApiService.logout).toHaveBeenCalledOnce();
    expect(authState.isAuthenticated()).toBe(false);
    expect(navigate).toHaveBeenCalledWith(['/login']);
  });

  it('redirects to login when the profile request is unauthorized', () => {
    profileResponse = throwError(
      () => new HttpErrorResponse({ status: 401, statusText: 'Unauthorized' }),
    );

    fixture.detectChanges();

    expect(navigate).toHaveBeenCalledWith(['/login']);
  });

  it('redirects once when the session is invalidated after the shell is open', () => {
    const authState = TestBed.inject(AuthStateService);

    fixture.detectChanges();
    authState.setSession({
      accessToken: 'access-token',
      tokenType: 'Bearer',
      expiresIn: 900,
    });

    authState.clearSession();
    authState.clearSession();

    expect(navigate).toHaveBeenCalledTimes(1);
    expect(navigate).toHaveBeenCalledWith(['/login']);
    expect(avatarState.clear).toHaveBeenCalledOnce();
  });
});
