import ProgressBar from '../../components/ui/ProgressBar';
import { cx } from '../../components/ui/cx';
import { attendanceVerdict, formatPercent } from '../../utils/attendance';

const TEXT_TONES = {
  neutral: 'text-muted',
  success: 'text-emerald-700 dark:text-emerald-300',
  warning: 'text-amber-700 dark:text-amber-300',
  danger: 'text-rose-700 dark:text-rose-300',
};

const BAR_TONES = { neutral: 'neutral', success: 'success', warning: 'warning', danger: 'danger' };

/**
 * StudentHub's signature element: the attendance bar with a notch at the target, and a
 * sentence that answers "can I miss a class?" instead of just showing a number.
 */
export default function AttendanceMeter({ stats, compact = false, className }) {
  const verdict = attendanceVerdict(stats);
  const target = Number(stats?.target ?? 75);
  return (
    <div className={className}>
      <div className="flex items-baseline justify-between gap-3">
        <span className={cx('font-semibold tracking-tight', compact ? 'text-lg' : 'text-2xl')}>
          {formatPercent(stats?.percentage, 2)}
        </span>
        <span className="text-[13px] text-muted tabular">
          {stats?.present ?? 0} of {stats?.totalCounted ?? 0} attended, target {formatPercent(target, 2)}
        </span>
      </div>
      <ProgressBar
        className="mt-2"
        value={stats?.percentage ?? 0}
        marker={target}
        tone={BAR_TONES[verdict.tone]}
        size={compact ? 'sm' : 'md'}
        label={`Attendance ${formatPercent(stats?.percentage, 2)} against a target of ${target}%`}
      />
      <p className={cx('mt-2 text-sm font-medium', TEXT_TONES[verdict.tone])}>{verdict.text}</p>
    </div>
  );
}
