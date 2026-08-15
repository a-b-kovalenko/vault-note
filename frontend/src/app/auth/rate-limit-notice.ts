import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

@Component({
  selector: 'app-rate-limit-notice',
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './rate-limit-notice.html',
  styleUrl: './rate-limit-notice.scss',
})
export class RateLimitNotice {
  readonly remainingSeconds = input.required<number>();
  protected readonly formattedRemainingTime = computed(() =>
    formatRemainingTime(this.remainingSeconds()),
  );
}

function formatRemainingTime(totalSeconds: number): string {
  const minutes = Math.floor(totalSeconds / 60);
  const seconds = totalSeconds % 60;

  if (minutes === 0) {
    return `${seconds} second${seconds === 1 ? '' : 's'}`;
  }

  return `${minutes}:${seconds.toString().padStart(2, '0')}`;
}
