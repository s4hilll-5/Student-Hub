import { cx } from './cx';

export default function Skeleton({ className }) {
  return <div className={cx('animate-pulse rounded-md bg-subtle', className)} aria-hidden="true" />;
}

/** Placeholder rows shaped like list content while it loads. */
export function SkeletonList({ rows = 4, className }) {
  return (
    <div className={cx('space-y-3', className)} role="status" aria-label="Loading">
      {Array.from({ length: rows }, (_, i) => (
        <div key={i} className="flex items-center gap-3">
          <Skeleton className="h-9 w-9 rounded-lg" />
          <div className="flex-1 space-y-2">
            <Skeleton className="h-3.5 w-2/5" />
            <Skeleton className="h-3 w-1/4" />
          </div>
        </div>
      ))}
    </div>
  );
}
