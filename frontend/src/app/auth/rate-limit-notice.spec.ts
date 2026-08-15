import { ComponentFixture, TestBed } from '@angular/core/testing';

import { RateLimitNotice } from './rate-limit-notice';

describe('RateLimitNotice', () => {
  let fixture: ComponentFixture<RateLimitNotice>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [RateLimitNotice] }).compileComponents();

    fixture = TestBed.createComponent(RateLimitNotice);
    fixture.componentRef.setInput('remainingSeconds', 472);
    fixture.detectChanges();
  });

  it('shows the remaining time in a readable format', () => {
    const notice = fixture.nativeElement.querySelector('.rate-limit-notice') as HTMLElement;

    expect(notice.textContent).toContain('7:52');
    expect(notice.getAttribute('role')).toBe('alert');
  });
});
