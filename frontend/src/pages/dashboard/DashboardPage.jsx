import { Link } from 'react-router-dom';
import toast from 'react-hot-toast';
import {
  BookOpen,
  CalendarCheck,
  CalendarDays,
  Check,
  ClipboardList,
  GraduationCap,
  MapPin,
  NotebookPen,
  Plus,
  Target,
  TriangleAlert,
} from 'lucide-react';
import { assignmentsApi, dashboardApi, subjectsApi } from '../../api/services';
import AssignmentStatusBar from '../../components/charts/AssignmentStatusBar';
import Badge, { SubjectTag } from '../../components/ui/Badge';
import Button from '../../components/ui/Button';
import DashboardCard from '../../components/ui/DashboardCard';
import EmptyState from '../../components/ui/EmptyState';
import ErrorState from '../../components/ui/ErrorState';
import { Panel } from '../../components/ui/Layout';
import ProgressBar from '../../components/ui/ProgressBar';
import Skeleton from '../../components/ui/Skeleton';
import { useApi } from '../../hooks/useApi';
import { formatPercent } from '../../utils/attendance';
import { labelOf, PREPARATION_STATUSES, PRIORITIES, toneOf } from '../../utils/constants';
import { countdown, describeDue, formatDate, formatTime, greetingFor } from '../../utils/date';
import { getErrorMessage } from '../../utils/errors';
import AttendanceMeter from '../attendance/AttendanceMeter';

const QUICK_ACTIONS = [
  { to: '/assignments?new=1', label: 'Assignment', icon: ClipboardList },
  { to: '/attendance', label: 'Attendance', icon: CalendarCheck },
  { to: '/timetable?new=session', label: 'Study session', icon: BookOpen },
  { to: '/exams?new=1', label: 'Exam', icon: GraduationCap },
  { to: '/notes?new=1', label: 'Note', icon: NotebookPen },
];

function DashboardSkeleton() {
  return (
    <div className="space-y-6" aria-busy="true">
      <Skeleton className="h-9 w-72" />
      <Skeleton className="h-32 w-full rounded-2xl" />
      <div className="grid gap-6 lg:grid-cols-3">
        <Skeleton className="h-64 rounded-2xl lg:col-span-2" />
        <Skeleton className="h-64 rounded-2xl" />
      </div>
    </div>
  );
}

function TodaySchedule({ classes, sessions }) {
  const items = [
    ...classes.map((c) => ({
      key: `c-${c.id}`,
      start: c.startTime,
      end: c.endTime,
      title: c.subject?.name,
      color: c.subject?.color,
      detail: c.location,
      kind: 'Class',
    })),
    ...sessions.map((s) => ({
      key: `s-${s.id}`,
      start: s.startTime,
      end: s.endTime,
      title: s.title,
      color: s.subject?.color ?? '#8B5CF6',
      detail: s.subject?.name,
      kind: 'Study session',
      done: s.completed,
    })),
  ].sort((a, b) => a.start.localeCompare(b.start));

  if (!items.length) {
    return (
      <EmptyState
        icon={CalendarDays}
        title="Nothing scheduled today"
        description="Add your weekly classes to the timetable and they'll show up here each day."
        action={
          <Link to="/timetable" className="text-sm font-medium text-brand hover:underline">
            Open timetable
          </Link>
        }
      />
    );
  }
  return (
    <ol className="divide-y divide-line">
      {items.map((item) => (
        <li key={item.key} className="flex items-stretch gap-4 px-5 py-3">
          <div className="w-[76px] shrink-0 pt-0.5 text-sm">
            <p className="font-medium">{formatTime(item.start)}</p>
            <p className="text-[13px] text-muted">{formatTime(item.end)}</p>
          </div>
          <span className="w-1 shrink-0 rounded-full" style={{ backgroundColor: item.color }} aria-hidden="true" />
          <div className="min-w-0 flex-1">
            <p className={`truncate font-medium ${item.done ? 'text-muted line-through' : ''}`}>{item.title}</p>
            <p className="flex min-w-0 items-center gap-1 text-[13px] text-muted">
              {item.kind === 'Class' ? (
                item.detail ? (
                  <>
                    <MapPin size={12} className="shrink-0" aria-hidden="true" />
                    <span className="truncate">{item.detail}</span>
                  </>
                ) : (
                  'Class'
                )
              ) : (
                <>
                  <BookOpen size={12} className="shrink-0" aria-hidden="true" />
                  <span className="truncate">
                    {item.kind}
                    {item.detail ? ` for ${item.detail}` : ''}
                  </span>
                </>
              )}
            </p>
          </div>
        </li>
      ))}
    </ol>
  );
}

export default function DashboardPage() {
  const { data, loading, error, reload } = useApi(
    () => Promise.all([dashboardApi.summary(), subjectsApi.list()]).then(([summary, subjects]) => ({ summary, subjects })),
    [],
  );

  async function markDone(assignment) {
    try {
      await assignmentsApi.setStatus(assignment.id, 'COMPLETED');
      toast.success(`Marked "${assignment.title}" as completed`);
      reload();
    } catch (err) {
      toast.error(getErrorMessage(err));
    }
  }

  if (loading && !data) return <DashboardSkeleton />;
  if (error && !data) return <ErrorState message={getErrorMessage(error)} onRetry={reload} />;

  const { summary, subjects } = data;
  const { assignments, attendance, study } = summary;
  const open = assignments.pending + assignments.inProgress;
  const attendanceTone =
    attendance.overallPercentage === null ? 'default' : attendance.subjectsBelowTarget > 0 ? 'warning' : 'success';

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <p className="text-sm text-muted">{formatDate(summary.today, { weekday: 'long', day: 'numeric', month: 'long' })}</p>
          <h1 className="mt-1 text-[30px] font-semibold leading-tight">
            {greetingFor()}, {summary.firstName}
          </h1>
        </div>
        <div className="flex flex-wrap gap-2" aria-label="Quick add">
          {QUICK_ACTIONS.map(({ to, label, icon: Icon }) => (
            <Link
              key={to}
              to={to}
              className="inline-flex h-9 items-center gap-1.5 rounded-[10px] border border-line bg-surface px-3 text-sm font-medium hover:bg-subtle"
            >
              <Plus size={15} className="text-brand" aria-hidden="true" />
              <Icon size={15} className="text-muted" aria-hidden="true" />
              {label}
            </Link>
          ))}
        </div>
      </div>

      <section
        aria-label="Summary"
        className="grid grid-cols-2 gap-px overflow-hidden rounded-2xl border border-line bg-line lg:grid-cols-4 [&>*]:bg-surface"
      >
        <DashboardCard
          to="/assignments"
          icon={ClipboardList}
          label="Open assignments"
          value={open}
          detail={`${assignments.completed} of ${assignments.total} completed`}
        />
        <DashboardCard
          to="/assignments?overdue=true"
          icon={TriangleAlert}
          label="Overdue"
          value={assignments.overdue}
          tone={assignments.overdue > 0 ? 'danger' : 'default'}
          detail={assignments.overdue > 0 ? 'Past due and not completed' : 'Nothing overdue'}
        />
        <DashboardCard
          to="/attendance"
          icon={CalendarCheck}
          label="Overall attendance"
          value={formatPercent(attendance.overallPercentage)}
          tone={attendanceTone}
          detail={
            attendance.subjects === 0
              ? 'Add subjects to start tracking'
              : attendance.subjectsBelowTarget > 0
                ? `${attendance.subjectsBelowTarget} of ${attendance.subjects} subjects below target`
                : `All ${attendance.subjects} subjects on target`
          }
        />
        <DashboardCard to="/timetable" icon={Target} label="Study this week" value={`${Number(study.hoursThisWeek)} h`}>
          <ProgressBar
            className="mt-3"
            size="sm"
            tone="accent"
            value={Math.min(Number(study.goalProgressPercent), 100)}
            label={`Study goal progress ${study.goalProgressPercent}%`}
          />
          <p className="mt-2 text-[13px] text-muted">
            {Number(study.goalProgressPercent)}% of your {study.goalHours} h goal
          </p>
        </DashboardCard>
      </section>

      <div className="grid gap-6 lg:grid-cols-3">
        <Panel
          title="Today"
          className="lg:col-span-2"
          bodyClassName="pt-2 pb-1"
          action={
            <Link to="/timetable" className="text-sm font-medium text-brand hover:underline">
              Timetable
            </Link>
          }
        >
          <TodaySchedule classes={summary.todaysClasses} sessions={summary.todaysStudySessions} />
        </Panel>

        <Panel
          title="Upcoming exams"
          bodyClassName="pt-2 pb-1"
          action={
            <Link to="/exams" className="text-sm font-medium text-brand hover:underline">
              All exams
            </Link>
          }
        >
          {summary.upcomingExams.length === 0 ? (
            <EmptyState icon={GraduationCap} title="No exams coming up" description="Add exam dates to get a countdown here." />
          ) : (
            <ul className="divide-y divide-line">
              {summary.upcomingExams.map((exam) => (
                <li key={exam.id} className="px-5 py-3">
                  <div className="flex items-start justify-between gap-3">
                    <p className="font-medium">{exam.title}</p>
                    <span className="shrink-0 text-sm font-semibold text-brand">{countdown(exam.examAt).label}</span>
                  </div>
                  <div className="mt-1 flex items-center justify-between gap-3">
                    <SubjectTag subject={exam.subject} className="text-muted" />
                    <Badge tone={toneOf(PREPARATION_STATUSES, exam.preparationStatus)}>
                      {labelOf(PREPARATION_STATUSES, exam.preparationStatus)}
                    </Badge>
                  </div>
                </li>
              ))}
            </ul>
          )}
        </Panel>
      </div>

      <div className="grid gap-6 lg:grid-cols-3">
        <Panel
          title="Upcoming deadlines"
          className="lg:col-span-2"
          bodyClassName="pt-2 pb-1"
          action={
            <Link to="/assignments" className="text-sm font-medium text-brand hover:underline">
              All assignments
            </Link>
          }
        >
          {summary.upcomingDeadlines.length === 0 ? (
            <EmptyState
              icon={ClipboardList}
              title="No upcoming deadlines"
              description="Assignments you add will be listed here in the order they're due."
              action={
                <Link to="/assignments?new=1" className="text-sm font-medium text-brand hover:underline">
                  Add an assignment
                </Link>
              }
            />
          ) : (
            <ul className="divide-y divide-line">
              {summary.upcomingDeadlines.map((assignment) => (
                <li key={assignment.id} className="flex items-center gap-3 px-5 py-3">
                  <div className="min-w-0 flex-1">
                    <p className="truncate font-medium">{assignment.title}</p>
                    <div className="mt-0.5 flex flex-wrap items-center gap-x-3 gap-y-1 text-[13px] text-muted">
                      <span>{describeDue(assignment.dueAt)}</span>
                      <SubjectTag subject={assignment.subject} className="text-[13px]" />
                    </div>
                  </div>
                  <Badge tone={toneOf(PRIORITIES, assignment.priority)}>{labelOf(PRIORITIES, assignment.priority)}</Badge>
                  <Button size="sm" variant="secondary" icon={Check} onClick={() => markDone(assignment)}>
                    Done
                  </Button>
                </li>
              ))}
            </ul>
          )}
        </Panel>

        <Panel title="Assignment progress" bodyClassName="px-5 pb-5 pt-3">
          {assignments.total === 0 ? (
            <p className="text-sm text-muted">Your completion breakdown appears once you add assignments.</p>
          ) : (
            <>
              <p className="text-sm text-muted">
                <span className="text-2xl font-semibold text-ink">
                  {Math.round((assignments.completed / assignments.total) * 100)}%
                </span>{' '}
                completed
              </p>
              <div className="mt-4">
                <AssignmentStatusBar counts={assignments} />
              </div>
            </>
          )}
        </Panel>
      </div>

      <Panel
        title="Attendance by subject"
        bodyClassName="px-5 pb-5 pt-3"
        action={
          <Link to="/attendance" className="text-sm font-medium text-brand hover:underline">
            Record attendance
          </Link>
        }
      >
        {subjects.length === 0 ? (
          <EmptyState
            icon={CalendarCheck}
            title="No subjects yet"
            description="Add the subjects you attend to see how your attendance compares with each target."
            action={
              <Link to="/attendance?new=1" className="text-sm font-medium text-brand hover:underline">
                Add a subject
              </Link>
            }
          />
        ) : (
          <ul className="grid gap-x-10 gap-y-6 md:grid-cols-2">
            {subjects.map((subject) => (
              <li key={subject.id}>
                <Link to={`/attendance/${subject.id}`} className="group block">
                  <SubjectTag subject={subject} className="mb-2 font-medium group-hover:text-brand" />
                  <AttendanceMeter stats={subject.attendance} compact />
                </Link>
              </li>
            ))}
          </ul>
        )}
      </Panel>
    </div>
  );
}
