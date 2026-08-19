import { TestBed } from '@angular/core/testing';
import { firstValueFrom, Observable, of, Subject } from 'rxjs';

import { AuthApiService } from './auth-api.service';
import { AuthRefreshService } from './auth-refresh.service';
import { AuthStateService } from './auth-state.service';
import { LoginResponse } from './auth.models';

describe('AuthRefreshService', () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('shares one refresh request between concurrent callers', () => {
    vi.stubGlobal('navigator', { locks: undefined });
    const refreshSubject = new Subject<LoginResponse>();
    const refresh = vi.fn((): Observable<LoginResponse> => refreshSubject.asObservable());
    const session: LoginResponse = {
      accessToken: 'refreshed-access-token',
      tokenType: 'Bearer',
      expiresIn: 900,
    };

    TestBed.configureTestingModule({
      providers: [
        AuthRefreshService,
        AuthStateService,
        { provide: AuthApiService, useValue: { refresh } },
      ],
    });

    const service = TestBed.inject(AuthRefreshService);
    const authState = TestBed.inject(AuthStateService);
    const firstValues: LoginResponse[] = [];
    const secondValues: LoginResponse[] = [];

    service.refresh().subscribe((value) => firstValues.push(value));
    service.refresh().subscribe((value) => secondValues.push(value));

    expect(refresh).toHaveBeenCalledTimes(1);

    refreshSubject.next(session);
    refreshSubject.complete();

    expect(firstValues).toEqual([session]);
    expect(secondValues).toEqual([session]);
    expect(authState.session()).toEqual(session);
  });

  it('waits for the shared Web Lock before starting refresh', async () => {
    let grantLock: (() => void) | undefined;
    const request = vi.fn(
      (_name: string, callback: () => Promise<LoginResponse>) =>
        new Promise<LoginResponse>((resolve, reject) => {
          grantLock = () => callback().then(resolve, reject);
        }),
    );
    vi.stubGlobal('navigator', { locks: { request } });

    const session: LoginResponse = {
      accessToken: 'refreshed-access-token',
      tokenType: 'Bearer',
      expiresIn: 900,
    };
    const refresh = vi.fn((): Observable<LoginResponse> => of(session));

    TestBed.configureTestingModule({
      providers: [
        AuthRefreshService,
        AuthStateService,
        { provide: AuthApiService, useValue: { refresh } },
      ],
    });

    const service = TestBed.inject(AuthRefreshService);
    const result = firstValueFrom(service.refresh());

    expect(request).toHaveBeenCalledWith('vaultnote-auth-refresh', expect.any(Function));
    expect(refresh).not.toHaveBeenCalled();

    grantLock?.();

    await expect(result).resolves.toEqual(session);
    expect(refresh).toHaveBeenCalledOnce();
  });
});
