import { useEffect, useMemo, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import toast from 'react-hot-toast';
import { BookOpen, CalendarDays, Check, ChevronLeft, ChevronRight, MapPin, Pencil, Plus, Trash2, Undo2 } from 'lucide-react';
import { studySessionsApi, timetableApi } from '../../api/services';
import Badge from '../../components/ui/Badge';
import Button from '../../components/ui/Button';
import ConfirmDialog from '../../components/ui/ConfirmDialog';
import EmptyState from '../../components/ui/EmptyState';
import ErrorState from '../../components/ui/ErrorState';
import { PageHeader, Panel, Segmented } from '../../components/ui/Layout';
import ProgressBar from '../../components/ui/ProgressBar';
import { SkeletonList } from '../../components/ui/Skeleton';
import { cx } from '../../components/ui/cx';
import { useAuth } from '../../context/AuthContext';
import { useApi } from '../../hooks/useApi';
import { useSubjects } from '../../hooks/useSubjects';
import {
  addDays,
  DAY_LABELS,
  DAYS,
  dayKeyOf,
  formatDate,
  formatMinutes,
  formatTimeRange,
  startOfWeek,
  toISODate,
  todayISO,
} from '../../utils/date';
import { getErrorMessage } from '../../utils/errors';
import ClassFormModal from './ClassFormModal';
import { CompleteSessionModal, StudySessionFormModal } from './StudySessionModals';

function ClassBlock({ entry, onEdit }) {
  return (
    <button
      type="button"
      onClick={() => onEdit(entry)}
      className="w-full rounded-lg border border-line bg-surface p-2.5 text-left transition hover:border-brand/40 hover:bg-subtle/60"
      style={{ boxShadow: `inset 3px 0 0 ${entry.subject.color}` }}
    >
      <p className="pl-1 text-[13px] text-muted">{formatTimeRange(entry.startTime, entry.endTime)}</p>
      <p className="truncate pl-1 text-sm font-medium">{entry.subject.name}</p>
      {entry.location && (
        <p className="flex items-center gap-1 truncate pl-1 text-[13px] text-muted">
          <MapPin size={12} aria-hidden="true" />
          {entry.location}
        </p>
      )}
    </button>
  );
}

function WeekView({ entries, today, onEdit, onAdd }) {
  const byDay = useMemo(() => {
    const map = Object.fromEntries(DAYS.map((d) => [d, []]));
    entries.forEach((e) => map[e.dayOfWeek].push(e));
    return map;
  }, [entries]);
  const showWeekend = byDay.SATURDAY.length > 0 || byDay.SUNDAY.length > 0;
  const days = showWeekend ? DAYS : DAYS.slice(0, 5);

  return (
    <div className={cx('grid gap-3 md:grid-cols-3', showWeekend ? 'xl:grid-cols-7' : 'xl:grid-cols-5')}>
      {days.map((day) => (
        <section key={day} aria-label={DAY_LABELS[day]} className={cx('rounded-xl p-2', day === today ? 'bg-brand-soft/60' : 'bg-subtle/50')}>
          <div className="mb-2 flex items-center justify-between px-1">
            <h3 className={cx('font-sans text-sm font-semibold', day === today && 'text-brand')}>
              {DAY_LABELS[day]}
              {day === today && <span className="ml-1.5 text-xs font-medium">Today</span>}
            </h3>
            <button type="button" onClick={() => onAdd(day)} className="rounded-md p-1 text-muted hover:bg-surface hover:text-ink" aria-label={`Add a class on ${DAY_LABELS[day]}`}>
              <Plus size={15} />
            </button>
          </div>
          <div className="space-y-2">
            {byDay[day].length === 0 ? (
              <p className="px-1 py-3 text-[13px] text-muted">No classes</p>
            ) : (
              byDay[day].map((entry) => <ClassBlock key={entry.id} entry={entry} onEdit={onEdit} />)
            )}
          </div>
        </section>
      ))}
    </div>
  );
}

function DayView({ entries, day, setDay, onEdit, onAdd }) {
  const list = entries.filter((e) => e.dayOfWeek === day);
  return (
    <div>
      <div className="mb-4 flex flex-wrap gap-1.5" role="group" aria-label="Day">
        {DAYS.map((d) => (
          <button
            key={d}
            type="button"
            aria-pressed={d === day}
            onClick={() => setDay(d)}
            className={cx('rounded-lg px-3 py-1.5 text-sm font-medium', d === day ? 'bg-brand text-white dark:text-[#14132a]' : 'bg-subtle text-muted hover:text-ink')}
          >
            {DAY_LABELS[d].slice(0, 3)}
          </button>
        ))}
      </div>
      {list.length === 0 ? (
        <EmptyState icon={CalendarDays} title={`No classes on ${DAY_LABELS[day]}`} action={<Button size="sm" variant="secondary" icon={Plus} onClick={() => onAdd(day)}>Add a class</Button>} />
      ) : (
        <div className="max-w-xl space-y-2">
          {list.map((entry) => (
            <ClassBlock key={entry.id} entry={entry} onEdit={onEdit} />
          ))}
        </div>
      )}
    </div>
  );
}

export default function TimetablePage() {
  const { user } = useAuth();
  const [searchParams, setSearchParams] = useSearchParams();
  const today = dayKeyOf(new Date());
  const [view, setView] = useState('week');
  const [day, setDay] = useState(today);
  const { subjects } = useSubjects();
  const classes = useApi(() => timetableApi.list(), []);

  const [weekStart, setWeekStart] = useState(() => startOfWeek(new Date()));
  const weekEnd = addDays(weekStart, 6);
  const sessions = useApi(
    () => studySessionsApi.list({ from: toISODate(weekStart), to: toISODate(weekEnd) }),
    [weekStart.getTime()],
  );

  const [classModal, setClassModal] = useState({ open: false, entry: null, day: null });
  const [sessionModal, setSessionModal] = useState({ open: false, session: null });
  const [completing, setCompleting] = useState(null);
  const [deleting, setDeleting] = useState(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    if (searchParams.get('new') === 'session') {
      setSessionModal({ open: true, session: null });
      searchParams.delete('new');
      setSearchParams(searchParams, { replace: true });
    }
  }, [searchParams, setSearchParams]);

  const sessionList = sessions.data ?? [];
  const minutesDone = sessionList.filter((s) => s.completed).reduce((sum, s) => sum + (s.actualMinutes ?? 0), 0);
  const goalHours = user?.weeklyStudyGoalHours ?? 10;
  const goalPercent = Math.round((minutesDone / (goalHours * 60)) * 100);
  const isCurrentWeek = toISODate(weekStart) === toISODate(startOfWeek(new Date()));

  async function reopen(session) {
    try {
      await studySessionsApi.setCompletion(session.id, { completed: false });
      toast.success('Session reopened');
      sessions.reload();
    } catch (err) {
      toast.error(getErrorMessage(err));
    }
  }

  async function confirmDelete() {
    setBusy(true);
    try {
      if (deleting.type === 'class') await timetableApi.remove(deleting.item.id);
      else await studySessionsApi.remove(deleting.item.id);
      toast.success(deleting.type === 'class' ? 'Class removed' : 'Study session deleted');
      setDeleting(null);
      setClassModal({ open: false, entry: null, day: null });
      (deleting.type === 'class' ? classes : sessions).reload();
    } catch (err) {
      toast.error(getErrorMessage(err));
    } finally {
      setBusy(false);
    }
  }

  const editClass = (entry) => setClassModal({ open: true, entry, day: entry.dayOfWeek });
  const addClass = (d) => setClassModal({ open: true, entry: null, day: d });

  return (
    <div className="space-y-8">
      <PageHeader
        title="Timetable"
        description="Your weekly classes, and the study time you plan around them."
        actions={
          <>
            <Button variant="secondary" icon={Plus} onClick={() => addClass(view === 'day' ? day : today)}>
              Add class
            </Button>
            <Button icon={BookOpen} onClick={() => setSessionModal({ open: true, session: null })}>
              Plan study session
            </Button>
          </>
        }
      />

      <section aria-labelledby="classes-heading">
        <div className="mb-4 flex items-center justify-between gap-3">
          <h2 id="classes-heading" className="text-lg font-semibold">
            Classes
          </h2>
          <Segmented label="Timetable view" value={view} onChange={setView} options={[{ value: 'week', label: 'Week' }, { value: 'day', label: 'Day' }]} />
        </div>
        {classes.error && <ErrorState className="mb-4" message={getErrorMessage(classes.error)} onRetry={classes.reload} />}
        {classes.loading && !classes.data ? (
          <SkeletonList rows={3} />
        ) : classes.data?.length === 0 ? (
          <Panel>
            <EmptyState
              icon={CalendarDays}
              title="Your timetable is empty"
              description="Add each weekly class once. It repeats every week and appears on your dashboard on the right days."
              action={<Button icon={Plus} onClick={() => addClass(today)}>Add a class</Button>}
            />
          </Panel>
        ) : view === 'week' ? (
          <WeekView entries={classes.data ?? []} today={today} onEdit={editClass} onAdd={addClass} />
        ) : (
          <DayView entries={classes.data ?? []} day={day} setDay={setDay} onEdit={editClass} onAdd={addClass} />
        )}
      </section>

      <Panel
        title="Study sessions"
        action={
          <div className="flex items-center gap-1">
            <Button variant="ghost" size="icon-sm" icon={ChevronLeft} aria-label="Previous week" onClick={() => setWeekStart(addDays(weekStart, -7))} />
            <span className="min-w-[150px] text-center text-sm font-medium tabular">
              {isCurrentWeek ? 'This week' : `${formatDate(toISODate(weekStart), { day: 'numeric', month: 'short' })} – ${formatDate(toISODate(weekEnd), { day: 'numeric', month: 'short' })}`}
            </span>
            <Button variant="ghost" size="icon-sm" icon={ChevronRight} aria-label="Next week" onClick={() => setWeekStart(addDays(weekStart, 7))} />
          </div>
        }
        bodyClassName="pt-3"
      >
        <div className="border-b border-line px-5 pb-4">
          <div className="flex items-baseline justify-between gap-3 text-sm">
            <p>
              <span className="text-lg font-semibold">{formatMinutes(minutesDone)}</span>{' '}
              <span className="text-muted">studied of your {goalHours} h weekly goal</span>
            </p>
            <span className="font-medium tabular">{goalPercent}%</span>
          </div>
          <ProgressBar className="mt-2" tone="accent" value={Math.min(goalPercent, 100)} label="Weekly study goal" />
        </div>

        {sessions.error && <ErrorState className="m-5" message={getErrorMessage(sessions.error)} onRetry={sessions.reload} />}
        {sessions.loading && !sessions.data ? (
          <div className="p-5">
            <SkeletonList rows={3} />
          </div>
        ) : sessionList.length === 0 ? (
          <EmptyState
            icon={BookOpen}
            title="No study sessions this week"
            description="Plan focused blocks of study. Mark them done to count towards your weekly goal."
            action={<Button size="sm" icon={Plus} onClick={() => setSessionModal({ open: true, session: null })}>Plan a session</Button>}
          />
        ) : (
          <ul className="divide-y divide-line">
            {sessionList.map((session) => (
              <li key={session.id} className="flex flex-wrap items-center gap-x-4 gap-y-2 px-5 py-3">
                <div className="w-36 shrink-0 text-sm">
                  <p className={cx('font-medium', session.sessionDate === todayISO() && 'text-brand')}>
                    {formatDate(session.sessionDate, { weekday: 'short', day: 'numeric', month: 'short' })}
                  </p>
                  <p className="text-[13px] text-muted">{formatTimeRange(session.startTime, session.endTime)}</p>
                </div>
                <div className="min-w-0 flex-1">
                  <p className={cx('truncate font-medium', session.completed && 'text-muted line-through')}>{session.title}</p>
                  <p className="truncate text-[13px] text-muted">
                    {session.subject?.name ?? 'General study'}, {formatMinutes(session.plannedMinutes)} planned
                  </p>
                </div>
                {session.completed ? (
                  <>
                    <Badge tone="success">Done, {formatMinutes(session.actualMinutes)}</Badge>
                    <Button variant="ghost" size="icon-sm" icon={Undo2} aria-label={`Reopen "${session.title}"`} onClick={() => reopen(session)} />
                  </>
                ) : (
                  <Button size="sm" variant="secondary" icon={Check} onClick={() => setCompleting(session)} disabled={session.sessionDate > todayISO()}>
                    Mark done
                  </Button>
                )}
                <Button variant="ghost" size="icon-sm" icon={Pencil} aria-label={`Edit "${session.title}"`} onClick={() => setSessionModal({ open: true, session })} />
                <Button variant="danger-ghost" size="icon-sm" icon={Trash2} aria-label={`Delete "${session.title}"`} onClick={() => setDeleting({ type: 'session', item: session })} />
              </li>
            ))}
          </ul>
        )}
      </Panel>

      <ClassFormModal
        open={classModal.open}
        entry={classModal.entry}
        defaultDay={classModal.day}
        subjects={subjects}
        onClose={() => setClassModal({ open: false, entry: null, day: null })}
        onDelete={(entry) => {
          setClassModal({ open: false, entry: null, day: null });
          setDeleting({ type: 'class', item: entry });
        }}
        onSaved={() => {
          setClassModal({ open: false, entry: null, day: null });
          classes.reload();
        }}
      />
      <StudySessionFormModal
        open={sessionModal.open}
        session={sessionModal.session}
        defaultDate={isCurrentWeek ? todayISO() : toISODate(weekStart)}
        subjects={subjects}
        onClose={() => setSessionModal({ open: false, session: null })}
        onSaved={() => {
          setSessionModal({ open: false, session: null });
          sessions.reload();
        }}
      />
      <CompleteSessionModal
        session={completing}
        onClose={() => setCompleting(null)}
        onSaved={() => {
          setCompleting(null);
          sessions.reload();
        }}
      />
      <ConfirmDialog
        open={Boolean(deleting)}
        title={deleting?.type === 'class' ? 'Remove this class?' : 'Delete this study session?'}
        message={
          deleting?.type === 'class'
            ? `${deleting.item.subject.name} on ${DAY_LABELS[deleting.item.dayOfWeek]}s will be removed from your timetable.`
            : deleting
              ? `"${deleting.item.title}" will be deleted.`
              : ''
        }
        confirmLabel={deleting?.type === 'class' ? 'Remove class' : 'Delete session'}
        loading={busy}
        onConfirm={confirmDelete}
        onCancel={() => setDeleting(null)}
      />
    </div>
  );
}
