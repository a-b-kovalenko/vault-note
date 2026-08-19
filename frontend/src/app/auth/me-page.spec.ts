import { HttpErrorResponse } from '@angular/common/http';
import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Observable, of, throwError } from 'rxjs';

import { AvatarStateService } from '../avatar/avatar-state.service';
import { AuthApiService } from './auth-api.service';
import { AuthStateService } from './auth-state.service';
import { UserProfile } from './auth.models';
import { MePage } from './me-page';

describe('MePage', () => {
  let fixture: ComponentFixture<MePage>;
  let page: MePage;
  let profileResponse: Observable<UserProfile>;
  let updateResponse: Observable<UserProfile>;
  let updateProfile: ReturnType<typeof vi.fn>;
  let avatarState: {
    avatarUrl: ReturnType<typeof signal<string | null>>;
    isLoading: ReturnType<typeof signal<boolean>>;
    isUploading: ReturnType<typeof signal<boolean>>;
    isRemoving: ReturnType<typeof signal<boolean>>;
    error: ReturnType<typeof signal<string | null>>;
    load: ReturnType<typeof vi.fn>;
    upload: ReturnType<typeof vi.fn>;
    remove: ReturnType<typeof vi.fn>;
  };

  beforeEach(async () => {
    profileResponse = of({
      id: 42,
      email: 'user@example.com',
      displayName: 'Profile User',
      emailVerified: true,
      roles: ['USER'],
    });
    updateResponse = of({
      id: 42,
      email: 'user@example.com',
      displayName: 'Updated Profile',
      emailVerified: true,
      roles: ['USER'],
    });
    updateProfile = vi.fn(() => updateResponse);
    avatarState = {
      avatarUrl: signal<string | null>(null),
      isLoading: signal(false),
      isUploading: signal(false),
      isRemoving: signal(false),
      error: signal<string | null>(null),
      load: vi.fn(() => of(void 0)),
      upload: vi.fn(() => of(void 0)),
      remove: vi.fn(() => of(void 0)),
    };

    const authApiService = {
      profile(): Observable<UserProfile> {
        return profileResponse;
      },
      updateProfile,
    };

    await TestBed.configureTestingModule({
      imports: [MePage],
      providers: [
        { provide: AuthApiService, useValue: authApiService },
        { provide: AvatarStateService, useValue: avatarState },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(MePage);
    page = fixture.componentInstance;
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
    expect((fixture.nativeElement.querySelector('#email') as HTMLInputElement).value).toBe(
      'user@example.com',
    );
    expect(fixture.nativeElement.textContent).toContain('Profile User');
    expect(fixture.nativeElement.textContent).toContain('Email verification');
    expect(fixture.nativeElement.textContent).toContain('Verified');
    expect((fixture.nativeElement.querySelector('#email') as HTMLInputElement).readOnly).toBe(true);
    expect(fixture.nativeElement.textContent).toContain('USER');
    expect(fixture.nativeElement.querySelector('.edit-profile-button')).not.toBeNull();
  });

  it('uploads a supported avatar file from the profile screen', () => {
    fixture.detectChanges();
    (fixture.nativeElement.querySelector('.edit-profile-button') as HTMLButtonElement).click();
    fixture.detectChanges();

    const file = new File(['avatar'], 'avatar.png', { type: 'image/png' });
    const input = fixture.nativeElement.querySelector(
      '.avatar-upload-button input',
    ) as HTMLInputElement;
    Object.defineProperty(input, 'files', { value: [file] });
    input.dispatchEvent(new Event('change'));

    expect(avatarState.upload).toHaveBeenCalledWith(file);
    expect(fixture.nativeElement.textContent).not.toContain('must be');
  });

  it('rejects an unsupported avatar file before uploading', () => {
    fixture.detectChanges();
    (fixture.nativeElement.querySelector('.edit-profile-button') as HTMLButtonElement).click();
    fixture.detectChanges();

    const file = new File(['avatar'], 'avatar.gif', { type: 'image/gif' });
    const input = fixture.nativeElement.querySelector(
      '.avatar-upload-button input',
    ) as HTMLInputElement;
    Object.defineProperty(input, 'files', { value: [file] });
    input.dispatchEvent(new Event('change'));
    fixture.detectChanges();

    expect(avatarState.upload).not.toHaveBeenCalled();
    expect(fixture.nativeElement.textContent).toContain(
      'Avatar must be a JPEG, PNG, or WebP image.',
    );
  });

  it('removes the current avatar', () => {
    avatarState.avatarUrl.set('blob:avatar');
    fixture.detectChanges();
    (fixture.nativeElement.querySelector('.edit-profile-button') as HTMLButtonElement).click();
    fixture.detectChanges();

    (fixture.nativeElement.querySelector('.avatar-remove-button') as HTMLButtonElement).click();

    expect(avatarState.remove).toHaveBeenCalledOnce();
  });

  it('hides avatar actions outside profile edit mode', () => {
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.avatar-upload-button')).toBeNull();
    expect(fixture.nativeElement.querySelector('.avatar-remove-button')).toBeNull();
  });

  it('enters edit mode and restores the saved value on cancel', () => {
    fixture.detectChanges();

    (fixture.nativeElement.querySelector('.edit-profile-button') as HTMLButtonElement).click();
    fixture.detectChanges();

    const input = fixture.nativeElement.querySelector('#display-name') as HTMLInputElement;
    input.value = 'Unsaved Profile';
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();

    (fixture.nativeElement.querySelector('.cancel-button') as HTMLButtonElement).click();
    fixture.detectChanges();

    expect(page.isEditing()).toBe(false);
    expect(fixture.nativeElement.textContent).toContain('Profile User');
    expect(updateProfile).not.toHaveBeenCalled();
  });

  it('saves the edited display name and updates the shared profile', () => {
    fixture.detectChanges();
    (fixture.nativeElement.querySelector('.edit-profile-button') as HTMLButtonElement).click();
    fixture.detectChanges();

    const input = fixture.nativeElement.querySelector('#display-name') as HTMLInputElement;
    input.value = '  Updated Profile  ';
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    (fixture.nativeElement.querySelector('.save-button') as HTMLButtonElement).click();
    fixture.detectChanges();

    expect(updateProfile).toHaveBeenCalledWith({ displayName: 'Updated Profile' });
    expect(page.profile()?.displayName).toBe('Updated Profile');
    expect(page.isEditing()).toBe(false);
    expect(fixture.nativeElement.textContent).toContain('Profile saved.');
    expect(fixture.nativeElement.textContent).toContain('Updated Profile');
  });

  it('does not save a blank display name', () => {
    fixture.detectChanges();
    (fixture.nativeElement.querySelector('.edit-profile-button') as HTMLButtonElement).click();
    fixture.detectChanges();

    const input = fixture.nativeElement.querySelector('#display-name') as HTMLInputElement;
    input.value = '   ';
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();

    expect(
      (fixture.nativeElement.querySelector('.save-button') as HTMLButtonElement).disabled,
    ).toBe(true);
    expect(fixture.nativeElement.textContent).toContain('Display name cannot be blank.');
    expect(updateProfile).not.toHaveBeenCalled();
  });

  it('keeps edit mode and shows the server error when saving fails', () => {
    updateResponse = throwError(
      () => new HttpErrorResponse({ status: 400, error: { message: 'Display name is invalid.' } }),
    );

    fixture.detectChanges();
    (fixture.nativeElement.querySelector('.edit-profile-button') as HTMLButtonElement).click();
    fixture.detectChanges();

    const input = fixture.nativeElement.querySelector('#display-name') as HTMLInputElement;
    input.value = 'Updated Profile';
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    (fixture.nativeElement.querySelector('.save-button') as HTMLButtonElement).click();
    fixture.detectChanges();

    expect(page.isEditing()).toBe(true);
    expect(page.isSaving()).toBe(false);
    expect(fixture.nativeElement.textContent).toContain('Display name is invalid.');
  });

  it('clears the session when the current user is unauthorized', () => {
    profileResponse = throwError(
      () => new HttpErrorResponse({ status: 401, statusText: 'Unauthorized' }),
    );

    fixture.detectChanges();

    expect(page.isLoading()).toBe(false);
    expect(TestBed.inject(AuthStateService).isAuthenticated()).toBe(false);
  });
});
