import { CircleAlert, RefreshCw } from 'lucide-react';
import Button from './Button';
import { cx } from './cx';

export default function ErrorState({ message, onRetry, className }) {
  return (
    <div
      role="alert"
      className={cx(
        'flex flex-col items-start gap-3 rounded-xl border border-rose-200 bg-rose-50 p-4 text-sm text-rose-800 sm:flex-row sm:items-center',
        'dark:border-rose-500/30 dark:bg-rose-500/10 dark:text-rose-200',
        className,
      )}
    >
      <CircleAlert size={18} className="shrink-0" aria-hidden="true" />
      <p className="flex-1">{message}</p>
      {onRetry && (
        <Button size="sm" variant="secondary" icon={RefreshCw} onClick={onRetry}>
          Try again
        </Button>
      )}
    </div>
  );
}
