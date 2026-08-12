import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import {
  AbstractControl,
  NonNullableFormBuilder,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';

import { AuthApiService } from './auth-api.service';
import { AuthState } from './auth-state.service';

@Component({
  selector: 'app-login-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule],
  templateUrl: './login-page.html',
  styleUrl: './login-page.scss',
})
export class LoginPage {
  private readonly authApiService = inject(AuthApiService);
  private readonly authState = inject(AuthState);

  readonly loginForm = inject(NonNullableFormBuilder).group({
    email: ['', [Validators.required, Validators.email, Validators.maxLength(320)]],
    password: ['', [Validators.required, Validators.maxLength(256)]],
  });

  readonly isSubmitting = signal(false);

  protected isInvalid(control: AbstractControl): boolean {
    return control.invalid && (control.dirty || control.touched);
  }

  protected onSubmit(): void {
    this.loginForm.markAllAsTouched();

    if (this.loginForm.invalid || this.isSubmitting()) {
      return;
    }

    this.isSubmitting.set(true);
    this.authApiService.login(this.loginForm.getRawValue()).subscribe({
      next: (response) => this.authState.setSession(response),
      complete: () => this.isSubmitting.set(false),
      error: () => this.isSubmitting.set(false),
    });
  }
}
