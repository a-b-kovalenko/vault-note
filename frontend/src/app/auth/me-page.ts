import { HttpErrorResponse, HttpStatusCode } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import {
  AbstractControl,
  NonNullableFormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  ValidatorFn,
  Validators,
} from '@angular/forms';
import { Router } from '@angular/router';
import { finalize } from 'rxjs';

import { AuthApiService } from './auth-api.service';
import { AuthStateService } from './auth-state.service';
import { UserProfile } from './auth.models';

const GENERIC_USER_ERROR = 'Unable to load your profile right now. Please try again.';
const GENERIC_PROFILE_UPDATE_ERROR = 'Unable to save your profile right now. Please try again.';
const DISPLAY_NAME_MAX_LENGTH = 100;

@Component({
  selector: 'app-me-page',
  imports: [ReactiveFormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './me-page.html',
  styleUrl: './me-page.scss',
})
export class MePage implements OnInit {
  private readonly authApiService = inject(AuthApiService);
  private readonly authState = inject(AuthStateService);
  private readonly router = inject(Router);

  readonly profile = signal<UserProfile | null>(null);
  readonly isLoading = signal(true);
  readonly loadError = signal<string | null>(null);
  readonly isEditing = signal(false);
  readonly isSaving = signal(false);
  readonly saveError = signal<string | null>(null);
  readonly saveSuccess = signal<string | null>(null);

  readonly profileForm = inject(NonNullableFormBuilder).group({
    displayName: [
      '',
      [Validators.required, Validators.maxLength(DISPLAY_NAME_MAX_LENGTH), nonBlankValidator],
    ],
  });

  ngOnInit(): void {
    this.authState
      .loadProfile(() => this.authApiService.profile())
      .subscribe({
        next: (profile) => {
          this.profile.set(profile);
          this.resetForm(profile.displayName);
          this.isLoading.set(false);
        },
        error: (error: unknown) => this.handleLoadError(error),
      });
  }

  protected isInvalid(control: AbstractControl): boolean {
    return control.invalid && (control.dirty || control.touched);
  }

  protected onEdit(): void {
    const profile = this.profile();
    if (!profile) {
      return;
    }

    this.resetForm(profile.displayName);
    this.clearSaveMessages();
    this.isEditing.set(true);
  }

  protected onCancel(): void {
    if (this.isSaving()) {
      return;
    }

    const profile = this.profile();
    if (profile) {
      this.resetForm(profile.displayName);
    }

    this.clearSaveMessages();
    this.isEditing.set(false);
  }

  protected onSubmit(): void {
    this.profileForm.markAllAsTouched();
    this.saveError.set(null);
    this.saveSuccess.set(null);

    if (this.profileForm.invalid || !this.isEditing() || this.isSaving()) {
      return;
    }

    const displayName = this.profileForm.controls.displayName.value.trim();
    this.isSaving.set(true);
    this.authApiService
      .updateProfile({ displayName })
      .pipe(finalize(() => this.isSaving.set(false)))
      .subscribe({
        next: (profile) => {
          this.profile.set(profile);
          this.authState.setProfile(profile);
          this.resetForm(profile.displayName);
          this.isEditing.set(false);
          this.saveSuccess.set('Profile saved.');
        },
        error: (error: unknown) => this.saveError.set(this.toSaveErrorMessage(error)),
      });
  }

  private handleLoadError(error: unknown): void {
    this.isLoading.set(false);

    if (error instanceof HttpErrorResponse && error.status === HttpStatusCode.Unauthorized) {
      void this.router.navigate(['/login']);
      return;
    }

    this.loadError.set(GENERIC_USER_ERROR);
  }

  private resetForm(displayName: string): void {
    this.profileForm.reset({ displayName });
  }

  private clearSaveMessages(): void {
    this.saveError.set(null);
    this.saveSuccess.set(null);
  }

  private toSaveErrorMessage(error: unknown): string {
    if (error instanceof HttpErrorResponse && typeof error.error === 'object' && error.error) {
      const response = error.error as { message?: unknown };
      if (typeof response.message === 'string' && response.message.trim()) {
        return response.message;
      }
    }

    return GENERIC_PROFILE_UPDATE_ERROR;
  }
}

const nonBlankValidator: ValidatorFn = (control: AbstractControl): ValidationErrors | null =>
  typeof control.value === 'string' && control.value.trim().length > 0 ? null : { blank: true };
