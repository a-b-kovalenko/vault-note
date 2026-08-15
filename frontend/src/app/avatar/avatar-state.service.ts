import { HttpErrorResponse, HttpStatusCode } from '@angular/common/http';
import { inject, Injectable, signal } from '@angular/core';
import {
  catchError,
  finalize,
  map,
  Observable,
  of,
  shareReplay,
  switchMap,
  tap,
  throwError,
} from 'rxjs';

import { AvatarApiService } from './avatar-api.service';

const GENERIC_AVATAR_LOAD_ERROR = 'Unable to load your avatar right now. Please try again.';
const GENERIC_AVATAR_UPLOAD_ERROR = 'Unable to upload your avatar right now. Please try again.';
const GENERIC_AVATAR_REMOVE_ERROR = 'Unable to remove your avatar right now. Please try again.';

@Injectable({ providedIn: 'root' })
export class AvatarStateService {
  private readonly avatarApiService = inject(AvatarApiService);
  private readonly avatarUrlState = signal<string | null>(null);

  private loadRequest: Observable<void> | null = null;
  private hasLoaded = false;
  private requestVersion = 0;

  readonly avatarUrl = this.avatarUrlState.asReadonly();
  readonly isLoading = signal(false);
  readonly isUploading = signal(false);
  readonly isRemoving = signal(false);
  readonly error = signal<string | null>(null);

  load(): Observable<void> {
    if (this.hasLoaded) {
      return of(void 0);
    }

    if (this.loadRequest) {
      return this.loadRequest;
    }

    const version = this.requestVersion;
    this.isLoading.set(true);

    const request = this.avatarApiService.getAvatar().pipe(
      map((content): Blob | null => content),
      catchError((error: unknown) => {
        if (isAvatarNotFound(error)) {
          return of(null);
        }

        if (version === this.requestVersion) {
          this.setError(error, GENERIC_AVATAR_LOAD_ERROR);
        }

        return throwError(() => error);
      }),
      tap((content) => {
        if (version !== this.requestVersion) {
          return;
        }

        this.replaceContent(content);
        this.hasLoaded = true;
      }),
      map(() => void 0),
      finalize(() => {
        if (version === this.requestVersion) {
          this.isLoading.set(false);
          this.loadRequest = null;
        }
      }),
      shareReplay({ bufferSize: 1, refCount: false }),
    );

    this.loadRequest = request;
    return request;
  }

  upload(file: File): Observable<void> {
    const version = this.requestVersion;
    this.error.set(null);
    this.isUploading.set(true);

    return this.avatarApiService.uploadAvatar(file).pipe(
      switchMap(() => this.avatarApiService.getAvatar()),
      tap((content) => {
        if (version !== this.requestVersion) {
          return;
        }

        this.replaceContent(content);
        this.hasLoaded = true;
      }),
      map(() => void 0),
      catchError((error: unknown) => {
        if (version === this.requestVersion) {
          this.setError(error, GENERIC_AVATAR_UPLOAD_ERROR);
        }

        return throwError(() => error);
      }),
      finalize(() => {
        if (version === this.requestVersion) {
          this.isUploading.set(false);
        }
      }),
    );
  }

  remove(): Observable<void> {
    const version = this.requestVersion;
    this.error.set(null);
    this.isRemoving.set(true);

    return this.avatarApiService.removeAvatar().pipe(
      tap(() => {
        if (version !== this.requestVersion) {
          return;
        }

        this.replaceContent(null);
        this.hasLoaded = true;
      }),
      catchError((error: unknown) => {
        if (version === this.requestVersion) {
          this.setError(error, GENERIC_AVATAR_REMOVE_ERROR);
        }

        return throwError(() => error);
      }),
      finalize(() => {
        if (version === this.requestVersion) {
          this.isRemoving.set(false);
        }
      }),
    );
  }

  clear(): void {
    this.requestVersion += 1;
    this.loadRequest = null;
    this.hasLoaded = false;
    this.isLoading.set(false);
    this.isUploading.set(false);
    this.isRemoving.set(false);
    this.error.set(null);
    this.replaceContent(null);
  }

  private replaceContent(content: Blob | null): void {
    if (content === null) {
      this.releaseObjectUrl();
      this.error.set(null);
      return;
    }

    const nextObjectUrl = URL.createObjectURL(content);
    this.releaseObjectUrl();
    this.avatarUrlState.set(nextObjectUrl);
    this.error.set(null);
  }

  private releaseObjectUrl(): void {
    const currentObjectUrl = this.avatarUrlState();
    if (currentObjectUrl) {
      URL.revokeObjectURL(currentObjectUrl);
      this.avatarUrlState.set(null);
    }
  }

  private setError(error: unknown, fallback: string): void {
    if (error instanceof HttpErrorResponse && isRecord(error.error)) {
      const message = error.error['message'];
      if (typeof message === 'string' && message.trim()) {
        this.error.set(message);
        return;
      }
    }

    this.error.set(fallback);
  }
}

function isAvatarNotFound(error: unknown): boolean {
  return error instanceof HttpErrorResponse && error.status === HttpStatusCode.NotFound;
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null;
}
