import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { Observable, of, throwError } from 'rxjs';

import { AuthApiService } from './auth-api.service';
import { PasswordResetConfirmRequest } from './auth.models';
import { ResetPasswordPage } from './reset-password-page';

describe('ResetPasswordPage', () => {
  let fixture: ComponentFixture<ResetPasswordPage>;
  let page: ResetPasswordPage;
  let resetRequests: PasswordResetConfirmRequest[];
  let resetResponse: Observable<void>;
  let routeToken: string | null;
  let navigate: ReturnType<typeof vi.fn>;

  beforeEach(async () => {
    resetRequests = [];
    resetResponse = of(void 0);
    routeToken = 'raw-token';
    navigate = vi.fn().mockResolvedValue(true);

    const authApiService = {
      confirmPasswordReset(request: PasswordResetConfirmRequest): Observable<void> {
        resetRequests.push(request);
        return resetResponse;
      },
    };

    await TestBed.configureTestingModule({
      imports: [ResetPasswordPage],
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
    fixture = TestBed.createComponent(ResetPasswordPage);
    page = fixture.componentInstance;
    fixture.detectChanges();
  }

  it('should require a strong matching password', () => {
    createPage();

    expect(page.resetForm.invalid).toBe(true);

    page.resetForm.setValue({
      password: 'NewPassword1234',
      confirmPassword: 'NewPassword4321',
    });
    page.resetForm.controls.confirmPassword.markAsTouched();
    fixture.detectChanges();

    expect(page.resetForm.hasError('passwordMismatch')).toBe(true);
    expect(
      (fixture.nativeElement.querySelector('#reset-confirm-password-error') as HTMLElement)
        .textContent,
    ).toContain('Passwords do not match.');
  });

  it('should confirm the reset and remove the token from the URL', () => {
    createPage();
    page.resetForm.setValue({
      password: 'NewPassword1234',
      confirmPassword: 'NewPassword1234',
    });

    const form = fixture.nativeElement.querySelector('form') as HTMLFormElement;
    form.dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));
    fixture.detectChanges();

    expect(resetRequests).toEqual([{ token: 'raw-token', newPassword: 'NewPassword1234' }]);
    expect(page.status()).toBe('success');
    expect(navigate).toHaveBeenCalledWith([], {
      relativeTo: expect.anything(),
      queryParams: {},
      replaceUrl: true,
    });
    expect(fixture.nativeElement.textContent).toContain('Password updated.');
  });

  it('should show an error when the reset token is missing', () => {
    routeToken = null;
    createPage();

    expect(page.status()).toBe('error');
    expect(fixture.nativeElement.textContent).toContain(
      'This reset link is invalid or has expired.',
    );
    expect(fixture.nativeElement.querySelector('form')).toBeNull();
  });

  it('should show the backend message when the reset token is invalid', () => {
    resetResponse = throwError(
      () =>
        new HttpErrorResponse({
          status: 400,
          error: {
            code: 'PASSWORD_RESET_FAILED',
            message: 'Password reset link is invalid or has expired.',
            violations: [],
          },
        }),
    );
    createPage();
    page.resetForm.setValue({
      password: 'NewPassword1234',
      confirmPassword: 'NewPassword1234',
    });

    const form = fixture.nativeElement.querySelector('form') as HTMLFormElement;
    form.dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));
    fixture.detectChanges();

    expect(page.status()).toBe('error');
    expect(page.errorMessage()).toBe('Password reset link is invalid or has expired.');
  });

  it('should navigate to a new reset request from the error state', () => {
    routeToken = null;
    createPage();

    (
      fixture.nativeElement.querySelector('.recovery-actions .submit-button') as HTMLButtonElement
    ).click();

    expect(navigate).toHaveBeenCalledWith(['/forgot-password']);
  });
});
