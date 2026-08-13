import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { Observable, of, throwError } from 'rxjs';

import { AuthApiService } from './auth-api.service';
import { ForgotPasswordPage } from './forgot-password-page';
import { PasswordResetRequest } from './auth.models';

describe('ForgotPasswordPage', () => {
  let fixture: ComponentFixture<ForgotPasswordPage>;
  let page: ForgotPasswordPage;
  let resetRequests: PasswordResetRequest[];
  let resetResponse: Observable<void>;
  let navigate: ReturnType<typeof vi.fn>;

  beforeEach(async () => {
    resetRequests = [];
    resetResponse = of(void 0);
    navigate = vi.fn().mockResolvedValue(true);

    const authApiService = {
      requestPasswordReset(request: PasswordResetRequest): Observable<void> {
        resetRequests.push(request);
        return resetResponse;
      },
    };

    await TestBed.configureTestingModule({
      imports: [ForgotPasswordPage],
      providers: [
        { provide: AuthApiService, useValue: authApiService },
        { provide: Router, useValue: { navigate } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ForgotPasswordPage);
    page = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should require a valid email', () => {
    expect(page.requestForm.invalid).toBe(true);

    page.requestForm.controls.email.setValue('invalid-email');

    expect(page.requestForm.controls.email.hasError('email')).toBe(true);
  });

  it('should submit the reset request and show neutral inbox guidance', () => {
    page.requestForm.controls.email.setValue('user@example.com');

    const form = fixture.nativeElement.querySelector('form') as HTMLFormElement;
    form.dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));
    fixture.detectChanges();

    expect(resetRequests).toEqual([{ email: 'user@example.com' }]);
    expect(page.requestSent()).toBe(true);
    expect(fixture.nativeElement.textContent).toContain('Your reset link is on its way.');
    expect(fixture.nativeElement.textContent).toContain('user@example.com');
  });

  it('should show the backend error when the request fails', () => {
    resetResponse = throwError(
      () =>
        new HttpErrorResponse({
          status: 500,
          error: {
            code: 'SERVER_ERROR',
            message: 'Mail service is unavailable.',
            violations: [],
          },
        }),
    );
    page.requestForm.controls.email.setValue('user@example.com');

    const form = fixture.nativeElement.querySelector('form') as HTMLFormElement;
    form.dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));
    fixture.detectChanges();

    expect(page.requestError()).toBe('Unable to send a reset link right now. Please try again.');
    expect(page.isSubmitting()).toBe(false);
  });

  it('should navigate back to login', () => {
    const signInButton = fixture.nativeElement.querySelector(
      '.recovery-prompt .text-button',
    ) as HTMLButtonElement;

    signInButton.click();

    expect(navigate).toHaveBeenCalledWith(['/login']);
  });
});
