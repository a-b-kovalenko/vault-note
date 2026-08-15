import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { Observable, of, throwError } from 'rxjs';

import { AuthApiService } from './auth-api.service';
import { UserProfile } from './auth.models';
import { MePage } from './me-page';

describe('MePage', () => {
  let fixture: ComponentFixture<MePage>;
  let page: MePage;
  let profileResponse: Observable<UserProfile>;
  let updateResponse: Observable<UserProfile>;
  let updateProfile: ReturnType<typeof vi.fn>;
  let navigate: ReturnType<typeof vi.fn>;

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
    navigate = vi.fn().mockResolvedValue(true);

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
        { provide: Router, useValue: { navigate } },
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
    expect(navigate).not.toHaveBeenCalled();
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

  it('redirects to login when the current session is unauthorized', () => {
    profileResponse = throwError(
      () => new HttpErrorResponse({ status: 401, statusText: 'Unauthorized' }),
    );

    fixture.detectChanges();

    expect(page.isLoading()).toBe(false);
    expect(navigate).toHaveBeenCalledWith(['/login']);
  });
});
