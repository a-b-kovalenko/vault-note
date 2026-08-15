import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';

import { AvatarApiService } from './avatar-api.service';
import { AvatarStateService } from './avatar-state.service';

describe('AvatarStateService', () => {
  let service: AvatarStateService;
  let avatarApiService: {
    getAvatar: ReturnType<typeof vi.fn>;
    uploadAvatar: ReturnType<typeof vi.fn>;
    removeAvatar: ReturnType<typeof vi.fn>;
  };
  let createObjectUrl: ReturnType<typeof vi.fn>;
  let revokeObjectUrl: ReturnType<typeof vi.fn>;

  beforeEach(() => {
    createObjectUrl = vi.fn((content: Blob) => 'blob:avatar-' + content.size);
    revokeObjectUrl = vi.fn();
    vi.stubGlobal('URL', { createObjectURL: createObjectUrl, revokeObjectURL: revokeObjectUrl });

    avatarApiService = {
      getAvatar: vi.fn(),
      uploadAvatar: vi.fn(),
      removeAvatar: vi.fn(),
    };

    TestBed.configureTestingModule({
      providers: [{ provide: AvatarApiService, useValue: avatarApiService }],
    });

    service = TestBed.inject(AvatarStateService);
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('loads and caches the avatar preview', () => {
    const content = new Blob(['avatar'], { type: 'image/jpeg' });
    avatarApiService.getAvatar.mockReturnValue(of(content));

    service.load().subscribe();
    service.load().subscribe();

    expect(avatarApiService.getAvatar).toHaveBeenCalledOnce();
    expect(createObjectUrl).toHaveBeenCalledWith(content);
    expect(service.avatarUrl()).toBe('blob:avatar-6');
    expect(service.error()).toBe(null);
  });

  it('uses the initials fallback when the backend reports no avatar', () => {
    avatarApiService.getAvatar.mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 404 })),
    );

    service.load().subscribe();

    expect(service.avatarUrl()).toBe(null);
    expect(service.error()).toBe(null);
    expect(service.isLoading()).toBe(false);
  });

  it('replaces the preview after upload and releases the old Object URL', () => {
    const firstContent = new Blob(['first'], { type: 'image/jpeg' });
    const secondContent = new Blob(['second'], { type: 'image/jpeg' });
    avatarApiService.getAvatar
      .mockReturnValueOnce(of(firstContent))
      .mockReturnValueOnce(of(secondContent));
    avatarApiService.uploadAvatar.mockReturnValue(of({ byte_size: secondContent.size }));

    service.load().subscribe();
    service.upload(new File(['source'], 'avatar.png', { type: 'image/png' })).subscribe();

    expect(avatarApiService.uploadAvatar).toHaveBeenCalledOnce();
    expect(avatarApiService.getAvatar).toHaveBeenCalledTimes(2);
    expect(service.avatarUrl()).toBe('blob:avatar-6');
    expect(revokeObjectUrl).toHaveBeenCalledWith('blob:avatar-5');
  });

  it('removes the preview after a successful delete', () => {
    const content = new Blob(['avatar'], { type: 'image/jpeg' });
    avatarApiService.getAvatar.mockReturnValue(of(content));
    avatarApiService.removeAvatar.mockReturnValue(of(void 0));

    service.load().subscribe();
    service.remove().subscribe();

    expect(avatarApiService.removeAvatar).toHaveBeenCalledOnce();
    expect(service.avatarUrl()).toBe(null);
    expect(revokeObjectUrl).toHaveBeenCalledWith('blob:avatar-6');
  });

  it('keeps the existing preview when upload fails', () => {
    const content = new Blob(['avatar'], { type: 'image/jpeg' });
    avatarApiService.getAvatar.mockReturnValue(of(content));
    avatarApiService.uploadAvatar.mockReturnValue(
      throwError(
        () =>
          new HttpErrorResponse({
            status: 400,
            error: { message: 'Avatar content is invalid.' },
          }),
      ),
    );

    service.load().subscribe();
    service
      .upload(new File(['source'], 'avatar.png', { type: 'image/png' }))
      .subscribe({ error: () => undefined });

    expect(service.avatarUrl()).toBe('blob:avatar-6');
    expect(service.error()).toBe('Avatar content is invalid.');
    expect(revokeObjectUrl).not.toHaveBeenCalled();
  });
});
