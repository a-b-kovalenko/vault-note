import { TestBed } from '@angular/core/testing';
import { Observable, Subject } from 'rxjs';

import { AuthApiService } from './auth-api.service';
import { AuthRefreshService } from './auth-refresh.service';
import { AuthStateService } from './auth-state.service';
import { LoginResponse } from './auth.models';

describe('AuthRefreshService', () => {
  it('shares one refresh request between concurrent callers', () => {
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
});
