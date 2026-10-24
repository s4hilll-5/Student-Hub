import { useState } from 'react';
import { Panel } from '../ui/Layout';

/**
 * Wraps a chart with its title, an HTML legend and a "Table" toggle. The table view is
 * the accessible twin of every chart: each value can be read without hovering.
 */
export default function ChartFrame({ title, description, legend, table, children, className }) {
  const [showTable, setShowTable] = useState(false);
  return (
    <Panel
      className={className}
      title={title}
      action={
        table && (
          <button
            type="button"
            onClick={() => setShowTable((v) => !v)}
            aria-pressed={showTable}
            className="rounded-md px-2 py-1 text-[13px] font-medium text-muted hover:bg-subtle hover:text-ink"
          >
            {showTable ? 'Chart' : 'Table'}
          </button>
        )
      }
      bodyClassName="px-5 pb-5 pt-1"
    >
      {description && <p className="text-[13px] text-muted">{description}</p>}
      {legend && !showTable && (
        <ul className="mt-3 flex flex-wrap gap-x-4 gap-y-1 text-[13px] text-muted">
          {legend.map((item) => (
            <li key={item.label} className="flex items-center gap-1.5">
              <span
                className={item.line ? 'h-0.5 w-4 rounded-full' : 'h-2.5 w-2.5 rounded-sm'}
                style={{ backgroundColor: item.color }}
                aria-hidden="true"
              />
              {item.label}
            </li>
          ))}
        </ul>
      )}
      <div className="mt-3">{showTable ? table : children}</div>
    </Panel>
  );
}

export function ChartTable({ columns, rows }) {
  return (
    <div className="max-h-72 overflow-auto">
      <table className="w-full text-sm">
        <thead>
          <tr className="border-b border-line text-left text-[13px] text-muted">
            {columns.map((c) => (
              <th key={c.key} scope="col" className={`py-2 pr-4 font-medium ${c.numeric ? 'text-right' : ''}`}>
                {c.header}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.map((row, i) => (
            <tr key={i} className="border-b border-line last:border-0">
              {columns.map((c) => (
                <td key={c.key} className={`py-2 pr-4 ${c.numeric ? 'text-right tabular' : ''}`}>
                  {row[c.key]}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

/** Tooltip body shared by the Recharts charts. */
export function ChartTooltip({ active, payload, label, formatLabel, formatValue }) {
  if (!active || !payload?.length) return null;
  return (
    <div className="rounded-lg border border-line bg-surface px-3 py-2 text-[13px] shadow-lg">
      <p className="mb-1 font-medium">{formatLabel ? formatLabel(label) : label}</p>
      {payload.map((entry) => (
        <p key={entry.dataKey} className="flex items-center gap-2 text-muted">
          <span className="h-2 w-2 rounded-sm" style={{ backgroundColor: entry.color }} aria-hidden="true" />
          {entry.name}: <span className="font-medium text-ink">{formatValue ? formatValue(entry.value, entry) : entry.value}</span>
        </p>
      ))}
    </div>
  );
}
