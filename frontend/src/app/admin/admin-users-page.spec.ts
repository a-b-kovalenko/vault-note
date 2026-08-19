import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Observable, of, throwError } from 'rxjs';

import { AuthApiService } from '../auth/auth-api.service';
import { AuthStateService } from '../auth/auth-state.service';
import { UserProfile } from '../auth/auth.models';
import {
  AdminUsersApiService,
  AdminUsersPage as AdminUsersPageData,
} from './admin-users-api.service';
import { AdminUsersPage as AdminUsersPageComponent } from './admin-users-page';

describe('AdminUsersPage', () => {
  let fixture: ComponentFixture<AdminUsersPageComponent>;
  let profileResponse: Observable<UserProfile>;
  let adminUsersApiService: { list: ReturnType<typeof vi.fn> };

  const firstPage: AdminUsersPageData = {
    content: [
      { id: 7, email: 'admin@example.com', displayName: 'Admin User' },
      { id: 8, email: 'user@example.com', displayName: 'Regular User' },
    ],
    page: 0,
    size: 20,
    totalPages: 2,
    totalElements: 21,
    first: true,
    last: false,
  };

  beforeEach(async () => {
    profileResponse = of({
      id: 7,
      email: 'admin@example.com',
      displayName: 'Admin User',
      emailVerified: true,
      roles: ['ADMIN', 'USER'],
    });
    adminUsersApiService = {
      list: vi.fn(() => of(firstPage)),
    };

    await TestBed.configureTestingModule({
      imports: [AdminUsersPageComponent],
      providers: [
        {
          provide: AuthApiService,
          useValue: {
            profile: vi.fn(() => profileResponse),
          },
        },
        { provide: AdminUsersApiService, useValue: adminUsersApiService },
        provideRouter([]),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AdminUsersPageComponent);
  });

  it('loads and displays a read-only paginated user list for an administrator', () => {
    fixture.detectChanges();

    expect(adminUsersApiService.list).toHaveBeenCalledWith(0, 20);
    expect(fixture.nativeElement.textContent).toContain('All users');
    expect(fixture.nativeElement.textContent).toContain('Admin User');
    expect(fixture.nativeElement.textContent).toContain('admin@example.com');
    expect(fixture.nativeElement.textContent).toContain('Showing 1–2 of 21');
    expect(fixture.nativeElement.textContent).toContain('Read only');
    expect(fixture.nativeElement.querySelectorAll('table button')).toHaveLength(0);
  });

  it('loads the next page from the pagination controls', () => {
    const secondPage: AdminUsersPageData = {
      ...firstPage,
      content: [{ id: 28, email: 'last@example.com', displayName: 'Last User' }],
      page: 1,
      first: false,
      last: true,
    };
    adminUsersApiService.list
      .mockReturnValueOnce(of(firstPage))
      .mockReturnValueOnce(of(secondPage));

    fixture.detectChanges();
    const nextButton = Array.from(fixture.nativeElement.querySelectorAll('button')).find(
      (button) => (button as HTMLButtonElement).textContent?.trim() === 'Next',
    ) as HTMLButtonElement | undefined;
    nextButton?.click();
    fixture.detectChanges();

    expect(adminUsersApiService.list).toHaveBeenLastCalledWith(1, 20);
    expect(fixture.nativeElement.textContent).toContain('Last User');
    expect(fixture.nativeElement.textContent).toContain('Page 2 of 2');
  });

  it('does not request users for a regular user and shows an access message', () => {
    profileResponse = of({
      id: 8,
      email: 'user@example.com',
      displayName: 'Regular User',
      emailVerified: true,
      roles: ['USER'],
    });

    fixture.detectChanges();

    expect(adminUsersApiService.list).not.toHaveBeenCalled();
    expect(fixture.nativeElement.textContent).toContain('Access restricted');
  });

  it('shows a forbidden response from the users API', () => {
    adminUsersApiService.list.mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 403, statusText: 'Forbidden' })),
    );

    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Access restricted');
  });

  it('clears the session when the profile request is unauthorized', () => {
    profileResponse = throwError(
      () => new HttpErrorResponse({ status: 401, statusText: 'Unauthorized' }),
    );

    fixture.detectChanges();

    expect(TestBed.inject(AuthStateService).isAuthenticated()).toBe(false);
  });
});
