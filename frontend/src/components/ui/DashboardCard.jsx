import { Link } from 'react-router-dom';
import { cx } from './cx';

const VALUE_TONES = {
  default: 'text-ink',
  danger: 'text-rose-600 dark:text-rose-400',
  success: 'text-emerald-600 dark:text-emerald-400',
  warning: 'text-amber-600 dark:text-amber-400',
};

/**
 * One statistic: a number with its label and a short line of context.
 * Rendered as a cell inside a shared panel on the dashboard (not as a floating card),
 * and as a link when `to` is given.
 */
export default function DashboardCard({ label, value, detail, icon: Icon, tone = 'default', to, children, className }) {
  const content = (
    <>
      <div className="flex items-center gap-2 text-sm text-muted">
        {Icon && <Icon size={16} aria-hidden="true" />}
        <span>{label}</span>
      </div>
      <p className={cx('mt-2 text-[30px] font-semibold leading-none tracking-tight', VALUE_TONES[tone])}>{value}</p>
      {detail && <p className="mt-2 text-[13px] text-muted">{detail}</p>}
      {children}
    </>
  );
  const classes = cx('block p-5', to && 'transition-colors hover:bg-subtle/60', className);
  return to ? (
    <Link to={to} className={classes}>
      {content}
    </Link>
  ) : (
    <div className={classes}>{content}</div>
  );
}
