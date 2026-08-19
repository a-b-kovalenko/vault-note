import { HttpErrorResponse, HttpStatusCode } from '@angular/common/http';
import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  OnInit,
  signal,
} from '@angular/core';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { AuthApiService } from '../auth/auth-api.service';
import { AuthStateService } from '../auth/auth-state.service';
import {
  AdminUsersApiService,
  AdminUser,
  AdminUsersPage as AdminUsersPageData,
} from './admin-users-api.service';

const PAGE_SIZE = 20;
const GENERIC_USERS_ERROR = 'Unable to load users right now. Please try again.';
const FORBIDDEN_USERS_ERROR = 'You do not have permission to view the user list.';

@Component({
  selector: 'app-admin-users-page',
  imports: [RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './admin-users-page.html',
  styleUrl: './admin-users-page.scss',
})
export class AdminUsersPage implements OnInit {
  private readonly adminUsersApiService = inject(AdminUsersApiService);
  private readonly authApiService = inject(AuthApiService);
  private readonly authState = inject(AuthStateService);

  readonly users = signal<AdminUser[]>([]);
  readonly pageIndex = signal(0);
  readonly pageSize = PAGE_SIZE;
  readonly totalPages = signal(0);
  readonly totalElements = signal(0);
  readonly isLoading = signal(true);
  readonly accessDenied = signal(false);
  readonly loadError = signal<string | null>(null);
  readonly forbiddenUsersError = FORBIDDEN_USERS_ERROR;
  readonly hasPreviousPage = computed(() => this.pageIndex() > 0);
  readonly hasNextPage = computed(
    () => this.totalPages() > 0 && this.pageIndex() < this.totalPages() - 1,
  );
  readonly pageLabel = computed(() => {
    const totalPages = this.totalPages();
    return totalPages === 0 ? 'Page 0 of 0' : `Page ${this.pageIndex() + 1} of ${totalPages}`;
  });
  readonly resultsLabel = computed(() => {
    const totalElements = this.totalElements();
    if (totalElements === 0) {
      return 'No users found';
    }

    const firstResult = this.pageIndex() * this.pageSize + 1;
    const lastResult = Math.min(firstResult + this.users().length - 1, totalElements);
    return `Showing ${firstResult}–${lastResult} of ${totalElements}`;
  });

  ngOnInit(): void {
    this.authState
      .loadProfile(() => this.authApiService.profile())
      .subscribe({
        next: (profile) => {
          if (!profile.roles.includes('ADMIN')) {
            this.accessDenied.set(true);
            this.isLoading.set(false);
            return;
          }

          this.loadPage(0);
        },
        error: (error: unknown) => this.handleLoadError(error),
      });
  }

  protected onPreviousPage(): void {
    if (this.isLoading() || !this.hasPreviousPage()) {
      return;
    }

    this.loadPage(this.pageIndex() - 1);
  }

  protected onNextPage(): void {
    if (this.isLoading() || !this.hasNextPage()) {
      return;
    }

    this.loadPage(this.pageIndex() + 1);
  }

  private loadPage(page: number): void {
    this.isLoading.set(true);
    this.loadError.set(null);
    this.accessDenied.set(false);
    this.users.set([]);

    this.adminUsersApiService
      .list(page, PAGE_SIZE)
      .pipe(finalize(() => this.isLoading.set(false)))
      .subscribe({
        next: (response) => this.applyPage(response),
        error: (error: unknown) => this.handleLoadError(error),
      });
  }

  private applyPage(response: AdminUsersPageData): void {
    this.users.set(response.content);
    this.pageIndex.set(response.page);
    this.totalPages.set(response.totalPages);
    this.totalElements.set(response.totalElements);
  }

  private handleLoadError(error: unknown): void {
    this.isLoading.set(false);

    if (error instanceof HttpErrorResponse && error.status === HttpStatusCode.Unauthorized) {
      this.authState.clearSession();
      return;
    }

    if (error instanceof HttpErrorResponse && error.status === HttpStatusCode.Forbidden) {
      this.accessDenied.set(true);
      return;
    }

    this.loadError.set(GENERIC_USERS_ERROR);
  }
}
