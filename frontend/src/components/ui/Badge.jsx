import { cx } from './cx';

const TONES = {
  neutral: 'bg-subtle text-muted',
  brand: 'bg-brand-soft text-brand',
  success: 'bg-emerald-50 text-emerald-700 dark:bg-emerald-500/10 dark:text-emerald-300',
  warning: 'bg-amber-50 text-amber-700 dark:bg-amber-500/10 dark:text-amber-300',
  danger: 'bg-rose-50 text-rose-700 dark:bg-rose-500/10 dark:text-rose-300',
};

export default function Badge({ tone = 'neutral', children, className }) {
  return (
    <span className={cx('inline-flex items-center gap-1 whitespace-nowrap rounded-md px-2 py-0.5 text-xs font-medium', TONES[tone], className)}>
      {children}
    </span>
  );
}

/** Small coloured dot + subject name. */
export function SubjectTag({ subject, className, fallback = 'No subject' }) {
  if (!subject) return <span className={cx('text-sm text-muted', className)}>{fallback}</span>;
  return (
    <span className={cx('inline-flex min-w-0 items-center gap-1.5 text-sm', className)}>
      <span className="h-2 w-2 shrink-0 rounded-full" style={{ backgroundColor: subject.color }} aria-hidden="true" />
      <span className="truncate">{subject.name}</span>
    </span>
  );
}
