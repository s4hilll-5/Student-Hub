const SEGMENTS = [
  { key: 'completed', label: 'Completed', className: 'bg-brand' },
  { key: 'inProgress', label: 'In progress', className: 'bg-brand/45' },
  { key: 'pending', label: 'To do', className: 'bg-muted/30' },
];

/**
 * Part-to-whole of assignments by status as one segmented bar (three parts read more
 * accurately as lengths than as donut angles). Every segment is also listed with its
 * count, so colour is never the only way to read it.
 */
export default function AssignmentStatusBar({ counts }) {
  const total = (counts?.pending ?? 0) + (counts?.inProgress ?? 0) + (counts?.completed ?? 0);
  return (
    <div>
      <div className="flex h-3 w-full gap-[2px] overflow-hidden rounded-full bg-subtle" role="img"
        aria-label={`${counts?.completed ?? 0} completed, ${counts?.inProgress ?? 0} in progress, ${counts?.pending ?? 0} to do`}>
        {total > 0 &&
          SEGMENTS.map((segment) => {
            const value = counts?.[segment.key] ?? 0;
            if (!value) return null;
            return <div key={segment.key} className={`${segment.className} h-full`} style={{ width: `${(value / total) * 100}%` }} />;
          })}
      </div>
      <ul className="mt-4 grid grid-cols-3 gap-2">
        {SEGMENTS.map(({ key, label, className }) => (
          <li key={key}>
            <div className="flex items-center gap-1.5 whitespace-nowrap text-[13px] text-muted">
              <span className={`h-2.5 w-2.5 rounded-sm ${className}`} aria-hidden="true" />
              {label}
            </div>
            <p className="mt-0.5 text-lg font-semibold">{counts?.[key] ?? 0}</p>
          </li>
        ))}
      </ul>
    </div>
  );
}
