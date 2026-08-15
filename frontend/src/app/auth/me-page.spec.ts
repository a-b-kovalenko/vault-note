import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { Observable, of, throwError } from 'rxjs';

import { AuthApiService } from './auth-api.service';
import { AuthStateService } from './auth-state.service';
import { UserProfile } from './auth.models';
import { MePage } from './me-page';

describe('MePage', () => {
  let fixture: ComponentFixture<MePage>;
  let page: MePage;
  let profileResponse: Observable<UserProfile>;
  let logoutResponse: Observable<void>;
  let authState: AuthStateService;
  let navigate: ReturnType<typeof vi.fn>;

  beforeEach(async () => {
    profileResponse = of({
      id: 42,
      email: 'user@example.com',
      displayName: 'Profile User',
      emailVerified: true,
      roles: ['USER'],
    });
    logoutResponse = of(void 0);
    navigate = vi.fn().mockResolvedValue(true);

    const authApiService = {
      profile(): Observable<UserProfile> {
        return profileResponse;
      },
      logout(): Observable<void> {
        return logoutResponse;
      },
    };

    await TestBed.configureTestingModule({
      imports: [MePage],
      providers: [
        { provide: AuthApiService, useValue: authApiService },
        { provide: Router, useValue: { navigate } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(MePage);
    page = fixture.componentInstance;
    authState = TestBed.inject(AuthStateService);
  });

  it('loads and displays the authenticated user', () => {
    fixture.detectChanges();

    expect(page.profile()).toEqual({
      id: 42,
      email: 'user@example.com',
      displayName: 'Profile User',
      emailVerified: true,
      roles: ['USER'],
    });
    expect(page.isLoading()).toBe(false);
    expect(fixture.nativeElement.textContent).toContain('42');
    expect(fixture.nativeElement.textContent).toContain('user@example.com');
    expect(fixture.nativeElement.textContent).toContain('Profile User');
    expect(fixture.nativeElement.textContent).not.toContain('Email verification');
    expect(fixture.nativeElement.textContent).toContain('USER');
    expect(navigate).not.toHaveBeenCalled();
  });

  it('redirects to login when the current session is unauthorized', () => {
    profileResponse = throwError(
      () => new HttpErrorResponse({ status: 401, statusText: 'Unauthorized' }),
    );

    fixture.detectChanges();

    expect(page.isLoading()).toBe(false);
    expect(navigate).toHaveBeenCalledWith(['/login']);
  });

  it('logs out, clears the local session, and redirects to login', () => {
    authState.setSession({
      accessToken: 'access-token',
      tokenType: 'Bearer',
      expiresIn: 900,
    });
    fixture.detectChanges();

    const logoutButton = fixture.nativeElement.querySelector('.logout-button') as HTMLButtonElement;
    logoutButton.click();
    fixture.detectChanges();

    expect(authState.isAuthenticated()).toBe(false);
    expect(navigate).toHaveBeenCalledWith(['/login']);
    expect(page.isLoggingOut()).toBe(true);
  });

  it('clears the local session and redirects even when logout fails', () => {
    logoutResponse = throwError(() => new Error('Network error'));
    authState.setSession({
      accessToken: 'access-token',
      tokenType: 'Bearer',
      expiresIn: 900,
    });
    fixture.detectChanges();

    const logoutButton = fixture.nativeElement.querySelector('.logout-button') as HTMLButtonElement;
    logoutButton.click();

    expect(authState.isAuthenticated()).toBe(false);
    expect(navigate).toHaveBeenCalledWith(['/login']);
  });
});
