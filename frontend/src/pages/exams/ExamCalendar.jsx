import { useMemo, useState } from 'react';
import { ChevronLeft, ChevronRight } from 'lucide-react';
import { assignmentsApi, examsApi } from '../../api/services';
import Button from '../../components/ui/Button';
import ErrorState from '../../components/ui/ErrorState';
import { cx } from '../../components/ui/cx';
import { useApi } from '../../hooks/useApi';
import { addDays, startOfWeek, toISODate, todayISO } from '../../utils/date';
import { getErrorMessage } from '../../utils/errors';

const WEEKDAYS = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];

/** Month grid with exams and assignment deadlines, both fetched for the visible weeks only. */
export default function ExamCalendar({ onOpenExam, onAddOnDate, refreshKey }) {
  const [month, setMonth] = useState(() => {
    const now = new Date();
    return new Date(now.getFullYear(), now.getMonth(), 1);
  });
  const gridStart = startOfWeek(month);
  const lastOfMonth = new Date(month.getFullYear(), month.getMonth() + 1, 0);
  const gridEnd = addDays(startOfWeek(lastOfMonth), 7); // exclusive
  const fromIso = gridStart.toISOString();
  const toIso = gridEnd.toISOString();

  const { data, error, reload } = useApi(
    () =>
      Promise.all([
        examsApi.list({ from: fromIso, to: toIso, size: 100, sort: 'examAt,asc' }),
        assignmentsApi.list({ dueFrom: fromIso, dueTo: toIso, size: 100, sort: 'dueAt,asc' }),
      ]),
    [fromIso, toIso, refreshKey],
  );

  const eventsByDay = useMemo(() => {
    const map = {};
    const push = (dateIso, item) => {
      (map[dateIso] ||= []).push(item);
    };
    if (data) {
      data[0].content.forEach((exam) => push(toISODate(new Date(exam.examAt)), { type: 'exam', item: exam, at: exam.examAt }));
      data[1].content.forEach((a) => push(toISODate(new Date(a.dueAt)), { type: 'assignment', item: a, at: a.dueAt }));
    }
    Object.values(map).forEach((list) => list.sort((x, y) => x.at.localeCompare(y.at)));
    return map;
  }, [data]);

  const days = [];
  for (let d = gridStart; d < gridEnd; d = addDays(d, 1)) days.push(d);
  const today = todayISO();
  const title = month.toLocaleDateString(undefined, { month: 'long', year: 'numeric' });

  return (
    <div className="rounded-2xl border border-line bg-surface">
      <div className="flex flex-wrap items-center justify-between gap-3 px-5 py-4">
        <div className="flex items-center gap-1">
          <Button variant="ghost" size="icon-sm" icon={ChevronLeft} aria-label="Previous month" onClick={() => setMonth(new Date(month.getFullYear(), month.getMonth() - 1, 1))} />
          <h2 className="min-w-[160px] text-center font-sans text-base font-semibold" aria-live="polite">
            {title}
          </h2>
          <Button variant="ghost" size="icon-sm" icon={ChevronRight} aria-label="Next month" onClick={() => setMonth(new Date(month.getFullYear(), month.getMonth() + 1, 1))} />
        </div>
        <div className="flex items-center gap-4 text-[13px] text-muted">
          <span className="flex items-center gap-1.5">
            <span className="h-2.5 w-2.5 rounded-sm bg-brand" aria-hidden="true" /> Exam
          </span>
          <span className="flex items-center gap-1.5">
            <span className="h-2.5 w-2.5 rounded-sm border-2 border-[#eb6834]" aria-hidden="true" /> Assignment due
          </span>
        </div>
      </div>
      {error && <ErrorState className="mx-5 mb-4" message={getErrorMessage(error)} onRetry={reload} />}
      <div className="relative overflow-x-auto">
        <div className="grid min-w-[700px] grid-cols-7 border-t border-line">
          {WEEKDAYS.map((d) => (
            <div key={d} className="border-b border-line px-2 py-2 text-[13px] font-medium text-muted">
              {d}
            </div>
          ))}
          {days.map((day) => {
            const iso = toISODate(day);
            const inMonth = day.getMonth() === month.getMonth();
            const events = eventsByDay[iso] ?? [];
            return (
              <div key={iso} className={cx('group min-h-[104px] border-b border-r border-line p-1.5 [&:nth-child(7n)]:border-r-0', !inMonth && 'bg-subtle/40')}>
                <div className="flex items-center justify-between">
                  <span
                    className={cx(
                      'grid h-6 min-w-6 place-items-center rounded-full px-1 text-[13px] tabular',
                      iso === today ? 'bg-brand font-semibold text-white dark:text-[#14132a]' : inMonth ? 'text-ink' : 'text-muted',
                    )}
                  >
                    {day.getDate()}
                  </span>
                  <button
                    type="button"
                    onClick={() => onAddOnDate(iso)}
                    className="rounded px-1 text-[13px] text-muted opacity-0 hover:text-brand focus:opacity-100 group-hover:opacity-100"
                    aria-label={`Add an exam on ${day.toLocaleDateString()}`}
                  >
                    +
                  </button>
                </div>
                <ul className="mt-1 space-y-1">
                  {events.slice(0, 3).map(({ type, item }) => (
                    <li key={`${type}-${item.id}`}>
                      {type === 'exam' ? (
                        <button
                          type="button"
                          onClick={() => onOpenExam(item)}
                          className="w-full truncate rounded-md bg-brand px-1.5 py-0.5 text-left text-xs font-medium text-white hover:bg-brand-strong dark:text-[#14132a]"
                          title={item.title}
                        >
                          {item.title}
                        </button>
                      ) : (
                        <span
                          className="block truncate rounded-md border border-[#eb6834]/70 px-1.5 py-0.5 text-xs text-ink"
                          title={`${item.title} (assignment due)`}
                        >
                          {item.title}
                        </span>
                      )}
                    </li>
                  ))}
                  {events.length > 3 && <li className="px-1 text-xs text-muted">+{events.length - 3} more</li>}
                </ul>
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
}
