import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { Observable, of, throwError } from 'rxjs';

import { AuthApiService } from './auth-api.service';
import { AuthStateService } from './auth-state.service';
import { CurrentUserResponse } from './auth.models';
import { MePage } from './me-page';

describe('MePage', () => {
  let fixture: ComponentFixture<MePage>;
  let page: MePage;
  let currentUserResponse: Observable<CurrentUserResponse>;
  let logoutResponse: Observable<void>;
  let authState: AuthStateService;
  let navigate: ReturnType<typeof vi.fn>;

  beforeEach(async () => {
    currentUserResponse = of({ userId: 42, roles: ['USER'] });
    logoutResponse = of(void 0);
    navigate = vi.fn().mockResolvedValue(true);

    const authApiService = {
      currentUser(): Observable<CurrentUserResponse> {
        return currentUserResponse;
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

    expect(page.currentUser()).toEqual({ userId: 42, roles: ['USER'] });
    expect(page.isLoading()).toBe(false);
    expect(fixture.nativeElement.textContent).toContain('42');
    expect(fixture.nativeElement.textContent).toContain('USER');
    expect(navigate).not.toHaveBeenCalled();
  });

  it('redirects to login when the current session is unauthorized', () => {
    currentUserResponse = throwError(
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
