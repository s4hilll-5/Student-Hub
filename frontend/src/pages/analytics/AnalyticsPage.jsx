import { useState } from 'react';
import { Link } from 'react-router-dom';
import {
  Bar,
  BarChart,
  CartesianGrid,
  Line,
  LineChart,
  ReferenceLine,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import { CircleAlert, CircleCheck, ClipboardList, GraduationCap } from 'lucide-react';
import { dashboardApi } from '../../api/services';
import AssignmentStatusBar from '../../components/charts/AssignmentStatusBar';
import ChartFrame, { ChartTable, ChartTooltip } from '../../components/charts/ChartFrame';
import { useChartColors } from '../../components/charts/useChartColors';
import Badge, { SubjectTag } from '../../components/ui/Badge';
import DashboardCard from '../../components/ui/DashboardCard';
import EmptyState from '../../components/ui/EmptyState';
import ErrorState from '../../components/ui/ErrorState';
import { PageHeader, Panel, Segmented } from '../../components/ui/Layout';
import ProgressBar from '../../components/ui/ProgressBar';
import Skeleton from '../../components/ui/Skeleton';
import { useApi } from '../../hooks/useApi';
import { formatPercent } from '../../utils/attendance';
import { countdown, describeDue, formatDate } from '../../utils/date';
import { getErrorMessage } from '../../utils/errors';

const RANGES = [
  { value: 4, label: '4 weeks' },
  { value: 8, label: '8 weeks' },
  { value: 12, label: '12 weeks' },
];

const weekLabel = (iso) => formatDate(iso, { day: 'numeric', month: 'short' });

function axisProps(colors) {
  return {
    tick: { fill: colors.axis, fontSize: 12 },
    tickLine: false,
    axisLine: { stroke: colors.grid },
  };
}

export default function AnalyticsPage() {
  const [weeks, setWeeks] = useState(8);
  const colors = useChartColors();
  const { data, loading, error, reload } = useApi(() => dashboardApi.analytics(weeks), [weeks]);

  if (error && !data) return <ErrorState message={getErrorMessage(error)} onRetry={reload} />;

  const trend = data?.assignments.weeklyTrend.map((w) => ({ ...w, label: weekLabel(w.weekStart) })) ?? [];
  const study = data?.weeklyStudy.map((w) => ({ ...w, hours: Number(w.hours), label: weekLabel(w.weekStart) })) ?? [];
  const goalHours = data?.studyGoal.goalHours ?? 0;
  const sessionsCompleted = study.reduce((sum, w) => sum + w.completedSessions, 0);
  const sessionsPlanned = study.reduce((sum, w) => sum + w.plannedSessions, 0);

  return (
    <div className={loading && data ? 'opacity-60 transition-opacity' : undefined}>
      <PageHeader
        title="Progress"
        description="Trends calculated from your assignments, attendance and study sessions."
        actions={<Segmented label="Time range" value={weeks} onChange={setWeeks} options={RANGES} />}
      />

      {!data ? (
        <div className="space-y-6">
          <Skeleton className="h-28 rounded-2xl" />
          <div className="grid gap-6 lg:grid-cols-2">
            <Skeleton className="h-80 rounded-2xl" />
            <Skeleton className="h-80 rounded-2xl" />
          </div>
        </div>
      ) : (
        <div className="space-y-6">
          <section aria-label="Summary" className="grid grid-cols-2 gap-px overflow-hidden rounded-2xl border border-line bg-line lg:grid-cols-4 [&>*]:bg-surface">
            <DashboardCard
              label="Completion rate"
              value={formatPercent(data.assignments.completionRate)}
              detail={`${data.assignments.completed} of ${data.assignments.total} assignments`}
            />
            <DashboardCard
              label="Overdue"
              value={data.assignments.overdue}
              tone={data.assignments.overdue ? 'danger' : 'default'}
              detail={`${data.assignments.open} still open`}
            />
            <DashboardCard label="Study this week" value={`${Number(data.studyGoal.hoursThisWeek)} h`}>
              <ProgressBar className="mt-3" size="sm" tone="accent" value={Math.min(Number(data.studyGoal.goalProgressPercent), 100)} label="Weekly study goal" />
              <p className="mt-2 text-[13px] text-muted">
                {Number(data.studyGoal.goalProgressPercent)}% of {goalHours} h goal
              </p>
            </DashboardCard>
            <DashboardCard
              label="Study sessions completed"
              value={sessionsCompleted}
              detail={`of ${sessionsPlanned} planned in ${weeks} weeks`}
            />
          </section>

          <div className="grid gap-6 lg:grid-cols-2">
            <ChartFrame
              title="Assignments added and completed"
              description="Per week, Monday to Sunday."
              legend={[
                { label: 'Added', color: colors.series1, line: true },
                { label: 'Completed', color: colors.series2, line: true },
              ]}
              table={
                <ChartTable
                  columns={[
                    { key: 'label', header: 'Week of' },
                    { key: 'created', header: 'Added', numeric: true },
                    { key: 'completed', header: 'Completed', numeric: true },
                  ]}
                  rows={trend}
                />
              }
            >
              <div className="h-60">
                <ResponsiveContainer width="100%" height="100%">
                  <LineChart data={trend} margin={{ top: 8, right: 12, bottom: 0, left: -18 }}>
                    <CartesianGrid vertical={false} stroke={colors.grid} />
                    <XAxis dataKey="label" {...axisProps(colors)} interval="preserveStartEnd" minTickGap={16} />
                    <YAxis allowDecimals={false} {...axisProps(colors)} axisLine={false} width={44} />
                    <Tooltip content={<ChartTooltip />} cursor={{ stroke: colors.grid }} />
                    <Line type="monotone" dataKey="created" name="Added" stroke={colors.series1} strokeWidth={2} dot={false}
                      activeDot={{ r: 4, strokeWidth: 2, stroke: colors.surface }} />
                    <Line type="monotone" dataKey="completed" name="Completed" stroke={colors.series2} strokeWidth={2} dot={false}
                      activeDot={{ r: 4, strokeWidth: 2, stroke: colors.surface }} />
                  </LineChart>
                </ResponsiveContainer>
              </div>
            </ChartFrame>

            <ChartFrame
              title="Study hours per week"
              description={`Completed sessions only. The line marks your ${goalHours} h goal.`}
              table={
                <ChartTable
                  columns={[
                    { key: 'label', header: 'Week of' },
                    { key: 'hours', header: 'Hours', numeric: true },
                    { key: 'completedSessions', header: 'Sessions done', numeric: true },
                    { key: 'plannedSessions', header: 'Planned', numeric: true },
                  ]}
                  rows={study}
                />
              }
            >
              <div className="h-60">
                <ResponsiveContainer width="100%" height="100%">
                  <BarChart data={study} margin={{ top: 16, right: 12, bottom: 0, left: -18 }}>
                    <CartesianGrid vertical={false} stroke={colors.grid} />
                    <XAxis dataKey="label" {...axisProps(colors)} interval="preserveStartEnd" minTickGap={16} />
                    <YAxis {...axisProps(colors)} axisLine={false} width={44} domain={[0, (max) => Math.max(Math.ceil(max), goalHours + 1)]} />
                    <Tooltip
                      cursor={{ fill: colors.grid, opacity: 0.4 }}
                      content={<ChartTooltip formatValue={(v, entry) => `${v} h, ${entry.payload.completedSessions} sessions`} />}
                    />
                    <ReferenceLine y={goalHours} stroke={colors.goal} strokeOpacity={0.55} ifOverflow="extendDomain"
                      label={{ value: `Goal ${goalHours} h`, position: 'insideTopRight', fill: colors.axis, fontSize: 12 }} />
                    <Bar dataKey="hours" name="Study hours" fill={colors.series1} radius={[4, 4, 0, 0]} maxBarSize={24} />
                  </BarChart>
                </ResponsiveContainer>
              </div>
            </ChartFrame>
          </div>

          <div className="grid gap-6 lg:grid-cols-2">
            <Panel title="Attendance by subject" bodyClassName="px-5 pb-5 pt-3">
              {data.subjectAttendance.length === 0 ? (
                <EmptyState title="No subjects yet" description="Add subjects on the Attendance page to compare them here." />
              ) : (
                <ul className="space-y-4">
                  {data.subjectAttendance.map((s) => (
                    <li key={s.subjectId}>
                      <div className="flex items-center justify-between gap-3 text-sm">
                        <SubjectTag subject={{ name: s.name, color: s.color }} className="font-medium" />
                        <span className="flex shrink-0 items-center gap-1.5">
                          <span className="font-semibold">{formatPercent(s.percentage, 1)}</span>
                          {s.totalCounted === 0 ? (
                            <span className="text-[13px] text-muted">no classes yet</span>
                          ) : s.meetsTarget ? (
                            <span className="flex items-center gap-1 text-[13px] text-emerald-700 dark:text-emerald-300">
                              <CircleCheck size={14} aria-hidden="true" /> On target
                            </span>
                          ) : (
                            <span className="flex items-center gap-1 text-[13px] text-rose-700 dark:text-rose-300">
                              <CircleAlert size={14} aria-hidden="true" /> Below {Number(s.target)}%
                            </span>
                          )}
                        </span>
                      </div>
                      <ProgressBar
                        className="mt-2"
                        size="sm"
                        value={s.percentage ?? 0}
                        marker={Number(s.target)}
                        tone={s.totalCounted === 0 ? 'neutral' : s.meetsTarget ? 'success' : 'danger'}
                        label={`${s.name} attendance ${formatPercent(s.percentage)} against ${Number(s.target)}% target`}
                      />
                    </li>
                  ))}
                </ul>
              )}
            </Panel>

            <Panel title="Assignments by status" bodyClassName="px-5 pb-5 pt-3">
              {data.assignments.total === 0 ? (
                <EmptyState title="No assignments yet" description="Your status breakdown appears once you add assignments." />
              ) : (
                <AssignmentStatusBar
                  counts={Object.fromEntries(
                    data.assignments.byStatus.map((s) => [
                      { PENDING: 'pending', IN_PROGRESS: 'inProgress', COMPLETED: 'completed' }[s.status],
                      s.count,
                    ]),
                  )}
                />
              )}
            </Panel>
          </div>

          <Panel title="Next 14 days" bodyClassName="pt-2 pb-1">
            {data.upcomingDeadlines.length === 0 ? (
              <EmptyState title="Nothing due in the next two weeks" description="Assignments and exams in the next 14 days will appear here." />
            ) : (
              <ul className="divide-y divide-line">
                {data.upcomingDeadlines.map((item) => (
                  <li key={`${item.type}-${item.id}`} className="flex items-center gap-3 px-5 py-3">
                    {item.type === 'EXAM' ? (
                      <GraduationCap size={18} className="shrink-0 text-brand" aria-hidden="true" />
                    ) : (
                      <ClipboardList size={18} className="shrink-0 text-muted" aria-hidden="true" />
                    )}
                    <div className="min-w-0 flex-1">
                      <p className="truncate font-medium">{item.title}</p>
                      <SubjectTag subject={item.subject} className="text-[13px] text-muted" />
                    </div>
                    <Badge tone={item.type === 'EXAM' ? 'brand' : 'neutral'}>{item.type === 'EXAM' ? 'Exam' : 'Assignment'}</Badge>
                    <span className="hidden w-48 shrink-0 text-right text-[13px] text-muted sm:block">
                      {item.type === 'EXAM' ? countdown(item.dueAt).label : describeDue(item.dueAt)}
                    </span>
                  </li>
                ))}
              </ul>
            )}
          </Panel>

          <p className="text-[13px] text-muted">
            Change your weekly study goal and attendance target in{' '}
            <Link to="/settings" className="font-medium text-brand hover:underline">
              Settings
            </Link>
            .
          </p>
        </div>
      )}
    </div>
  );
}
