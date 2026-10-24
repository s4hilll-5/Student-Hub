import { cx } from './cx';
import Skeleton from './Skeleton';

/**
 * Generic table.
 * columns: [{ key, header, render?(row), className?, headerClassName? }]
 * On narrow screens the table scrolls horizontally inside its panel instead of the page.
 */
export default function DataTable({
  columns,
  rows,
  rowKey = (row) => row.id,
  loading = false,
  skeletonRows = 5,
  empty,
  rowClassName,
  caption,
}) {
  return (
    <div className="relative overflow-x-auto">
      <table className="w-full min-w-[640px] border-collapse text-left text-sm">
        {caption && <caption className="sr-only">{caption}</caption>}
        <thead>
          <tr className="border-b border-line">
            {columns.map((column) => (
              <th
                key={column.key}
                scope="col"
                className={cx('px-4 py-3 text-[13px] font-medium text-muted first:pl-5 last:pr-5', column.headerClassName)}
              >
                {column.header}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {loading
            ? Array.from({ length: skeletonRows }, (_, i) => (
                <tr key={`skeleton-${i}`} className="border-b border-line last:border-0">
                  {columns.map((column) => (
                    <td key={column.key} className="px-4 py-4 first:pl-5 last:pr-5">
                      <Skeleton className="h-4 w-3/4" />
                    </td>
                  ))}
                </tr>
              ))
            : rows.map((row) => (
                <tr
                  key={rowKey(row)}
                  className={cx('border-b border-line align-middle transition-colors last:border-0 hover:bg-subtle/50', rowClassName?.(row))}
                >
                  {columns.map((column) => (
                    <td key={column.key} className={cx('px-4 py-3 first:pl-5 last:pr-5', column.className)}>
                      {column.render ? column.render(row) : row[column.key]}
                    </td>
                  ))}
                </tr>
              ))}
        </tbody>
      </table>
      {!loading && rows.length === 0 && empty}
    </div>
  );
}
