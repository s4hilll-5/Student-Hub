import { cx } from './cx';

const FILLS = {
  brand: 'bg-brand',
  accent: 'bg-accent',
  success: 'bg-emerald-500',
  warning: 'bg-amber-500',
  danger: 'bg-rose-500',
  neutral: 'bg-muted/50',
};

/**
 * Horizontal progress bar. `marker` (0-100) draws a target notch, used by attendance
 * so the gap between where you are and where you need to be is visible at a glance.
 */
export default function ProgressBar({ value, max = 100, tone = 'brand', marker, label, size = 'md', className }) {
  const percent = value === null || value === undefined ? 0 : Math.max(0, Math.min(100, (value / max) * 100));
  return (
    <div className={cx('relative', className)}>
      <div
        role="progressbar"
        aria-label={label}
        aria-valuemin={0}
        aria-valuemax={max}
        aria-valuenow={value ?? 0}
        className={cx('w-full overflow-hidden rounded-full bg-subtle', size === 'sm' ? 'h-1.5' : 'h-2.5')}
      >
        <div className={cx('h-full rounded-full transition-[width] duration-500', FILLS[tone])} style={{ width: `${percent}%` }} />
      </div>
      {marker !== undefined && marker !== null && (
        <span
          className={cx('absolute top-1/2 w-0.5 -translate-x-1/2 -translate-y-1/2 rounded-full bg-ink/70', size === 'sm' ? 'h-3' : 'h-4')}
          style={{ left: `${Math.max(0, Math.min(100, marker))}%` }}
          aria-hidden="true"
        />
      )}
    </div>
  );
}
