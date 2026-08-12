import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { Observable, of, throwError } from 'rxjs';

import { AuthApiService } from './auth-api.service';
import { VerifyEmailPage } from './verify-email-page';

describe('VerifyEmailPage', () => {
  let fixture: ComponentFixture<VerifyEmailPage>;
  let page: VerifyEmailPage;
  let verificationResponse: Observable<void>;
  let verificationRequests: string[];
  let routeToken: string | null;
  let navigate: ReturnType<typeof vi.fn>;

  beforeEach(async () => {
    verificationResponse = of(void 0);
    verificationRequests = [];
    routeToken = 'raw-token';
    navigate = vi.fn().mockResolvedValue(true);

    const authApiService = {
      verifyEmail(token: string): Observable<void> {
        verificationRequests.push(token);
        return verificationResponse;
      },
    };

    await TestBed.configureTestingModule({
      imports: [VerifyEmailPage],
      providers: [
        { provide: AuthApiService, useValue: authApiService },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              queryParamMap: {
                get: (name: string) => (name === 'token' ? routeToken : null),
              },
            },
          },
        },
        { provide: Router, useValue: { navigate } },
      ],
    }).compileComponents();
  });

  function createPage(): void {
    fixture = TestBed.createComponent(VerifyEmailPage);
    page = fixture.componentInstance;
    fixture.detectChanges();
  }

  it('verifies the token and shows the success state', () => {
    createPage();

    expect(verificationRequests).toEqual(['raw-token']);
    expect(page.status()).toBe('success');
    expect(fixture.nativeElement.textContent).toContain('Email verified');
  });

  it('shows an invalid-link message when the token is missing', () => {
    routeToken = null;
    createPage();

    expect(verificationRequests).toEqual([]);
    expect(page.status()).toBe('error');
    expect(fixture.nativeElement.textContent).toContain(
      'This verification link is invalid or has expired.',
    );
  });

  it('shows the backend message when verification fails', () => {
    verificationResponse = throwError(
      () =>
        new HttpErrorResponse({
          status: 400,
          error: {
            code: 'EMAIL_VERIFICATION_FAILED',
            message: 'Email verification link is invalid or has expired.',
          },
        }),
    );
    createPage();

    expect(page.status()).toBe('error');
    expect(page.errorMessage()).toBe('Email verification link is invalid or has expired.');
  });

  it('navigates to login from the success state', () => {
    createPage();

    (fixture.nativeElement.querySelector('.verify-login-button') as HTMLButtonElement).click();

    expect(navigate).toHaveBeenCalledWith(['/login']);
  });

  it('navigates to registration from the error state', () => {
    routeToken = null;
    createPage();

    (fixture.nativeElement.querySelector('.secondary-button') as HTMLButtonElement).click();

    expect(navigate).toHaveBeenCalledWith(['/register']);
  });
});
