import { HttpErrorResponse, HttpStatusCode } from '@angular/common/http';
import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  computed,
  ElementRef,
  HostListener,
  inject,
  OnInit,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Router, RouterLink, RouterOutlet } from '@angular/router';
import { finalize } from 'rxjs';

import { AuthApiService } from '../auth/auth-api.service';
import { AuthStateService } from '../auth/auth-state.service';
import { AvatarStateService } from '../avatar/avatar-state.service';
import { getInitials } from '../avatar/avatar-utils';

const GENERIC_PROFILE_ERROR = 'Unable to load your profile right now. Please try again.';

@Component({
  selector: 'app-authenticated-shell',
  imports: [RouterLink, RouterOutlet],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './authenticated-shell.html',
  styleUrl: './authenticated-shell.scss',
})
export class AuthenticatedShell implements OnInit {
  private readonly authApiService = inject(AuthApiService);
  private readonly authState = inject(AuthStateService);
  private readonly avatarState = inject(AvatarStateService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly router = inject(Router);
  private readonly elementRef = inject(ElementRef<HTMLElement>);

  readonly profile = this.authState.profile;
  readonly isProfileLoading = signal(true);
  readonly profileError = signal<string | null>(null);
  readonly isAccountMenuOpen = signal(false);
  readonly isLoggingOut = signal(false);
  readonly avatarUrl = this.avatarState.avatarUrl;
  readonly isAdmin = computed(() => this.profile()?.roles.includes('ADMIN') ?? false);
  readonly initials = computed(() => getInitials(this.profile()));
  private loginNavigationStarted = false;

  ngOnInit(): void {
    this.authState.startSessionSynchronization();
    this.authState.sessionInvalidated$
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.redirectToLogin());
    this.avatarState.load().subscribe({ error: () => undefined });

    this.authState
      .loadProfile(() => this.authApiService.profile())
      .subscribe({
        next: () => this.isProfileLoading.set(false),
        error: (error: unknown) => this.handleProfileError(error),
      });
  }

  protected toggleAccountMenu(): void {
    if (this.isProfileLoading() || this.isLoggingOut()) {
      return;
    }

    this.isAccountMenuOpen.update((isOpen) => !isOpen);
  }

  protected closeAccountMenu(): void {
    this.isAccountMenuOpen.set(false);
  }

  protected onLogout(): void {
    if (this.isLoggingOut()) {
      return;
    }

    this.closeAccountMenu();
    this.isLoggingOut.set(true);
    this.authApiService
      .logout()
      .pipe(
        finalize(() => {
          this.authState.clearSession();
          this.redirectToLogin();
        }),
      )
      .subscribe({ error: () => undefined });
  }

  @HostListener('document:click', ['$event'])
  protected onDocumentClick(event: MouseEvent): void {
    if (!this.elementRef.nativeElement.contains(event.target as Node)) {
      this.closeAccountMenu();
    }
  }

  @HostListener('document:keydown.escape')
  protected onEscape(): void {
    this.closeAccountMenu();
  }

  private handleProfileError(error: unknown): void {
    this.isProfileLoading.set(false);

    if (error instanceof HttpErrorResponse && error.status === HttpStatusCode.Unauthorized) {
      this.authState.clearSession();
      this.redirectToLogin();
      return;
    }

    this.profileError.set(GENERIC_PROFILE_ERROR);
  }

  private redirectToLogin(): void {
    if (this.loginNavigationStarted || this.router.url === '/login') {
      return;
    }

    this.loginNavigationStarted = true;
    this.avatarState.clear();
    void this.router.navigate(['/login']);
  }
}
