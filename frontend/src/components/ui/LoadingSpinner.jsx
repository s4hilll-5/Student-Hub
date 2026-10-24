import { LoaderCircle } from 'lucide-react';
import { cx } from './cx';

export default function LoadingSpinner({ label = 'Loading', size = 22, className }) {
  return (
    <span role="status" className={cx('inline-flex items-center gap-2 text-muted', className)}>
      <LoaderCircle size={size} className="animate-spin text-brand" aria-hidden="true" />
      <span className="sr-only">{label}</span>
    </span>
  );
}

export function FullPageLoader({ label = 'Loading StudentHub' }) {
  return (
    <div className="flex min-h-screen items-center justify-center bg-bg">
      <LoadingSpinner label={label} size={28} />
    </div>
  );
}
