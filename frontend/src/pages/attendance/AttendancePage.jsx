import { useEffect, useMemo, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import toast from 'react-hot-toast';
import { CalendarCheck, Pencil, Plus, Trash2 } from 'lucide-react';
import { subjectsApi } from '../../api/services';
import Button from '../../components/ui/Button';
import ConfirmDialog from '../../components/ui/ConfirmDialog';
import EmptyState from '../../components/ui/EmptyState';
import ErrorState from '../../components/ui/ErrorState';
import { PageHeader, Panel } from '../../components/ui/Layout';
import { SkeletonList } from '../../components/ui/Skeleton';
import { useApi } from '../../hooks/useApi';
import { formatPercent } from '../../utils/attendance';
import { todayISO } from '../../utils/date';
import { getErrorMessage } from '../../utils/errors';
import AttendanceMeter from './AttendanceMeter';
import SubjectFormModal from './SubjectFormModal';

const QUICK_STATUSES = [
  { status: 'PRESENT', label: 'Present' },
  { status: 'ABSENT', label: 'Absent' },
  { status: 'CANCELLED', label: 'Cancelled' },
];

export default function AttendancePage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const [belowTargetOnly, setBelowTargetOnly] = useState(false);
  const { data: subjects, loading, error, reload } = useApi(() => subjectsApi.list({ belowTarget: belowTargetOnly || undefined }), [belowTargetOnly]);
  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState(null);
  const [deleting, setDeleting] = useState(null);
  const [deleteBusy, setDeleteBusy] = useState(false);
  const [logging, setLogging] = useState(null);

  useEffect(() => {
    if (searchParams.get('new') === '1') {
      setEditing(null);
      setFormOpen(true);
      searchParams.delete('new');
      setSearchParams(searchParams, { replace: true });
    }
  }, [searchParams, setSearchParams]);

  // Overall numbers pooled across the listed subjects (the API calculates each subject's stats).
  const overall = useMemo(() => {
    if (!subjects?.length) return null;
    const present = subjects.reduce((sum, s) => sum + s.attendance.present, 0);
    const total = subjects.reduce((sum, s) => sum + s.attendance.totalCounted, 0);
    const below = subjects.filter((s) => !s.attendance.meetsTarget).length;
    const percentage = total === 0 ? null : Math.round((present / total) * 10000) / 100;
    return { present, totalCounted: total, percentage, below };
  }, [subjects]);

  async function quickLog(subject, status, label) {
    setLogging(`${subject.id}-${status}`);
    try {
      await subjectsApi.recordAttendance(subject.id, { classDate: todayISO(), status });
      toast.success(`${subject.name}: marked ${label.toLowerCase()} for today`);
      reload();
    } catch (err) {
      toast.error(getErrorMessage(err));
    } finally {
      setLogging(null);
    }
  }

  async function confirmDelete() {
    setDeleteBusy(true);
    try {
      await subjectsApi.remove(deleting.id);
      toast.success(`${deleting.name} deleted`);
      setDeleting(null);
      reload();
    } catch (err) {
      toast.error(getErrorMessage(err));
    } finally {
      setDeleteBusy(false);
    }
  }

  const openCreate = () => {
    setEditing(null);
    setFormOpen(true);
  };

  return (
    <div>
      <PageHeader
        title="Attendance"
        description="Log each class with one tap. Cancelled classes don't count towards your percentage."
        actions={
          <Button icon={Plus} onClick={openCreate}>
            Add subject
          </Button>
        }
      />

      {overall && !belowTargetOnly && (
        <div className="mb-6 flex flex-wrap items-baseline gap-x-8 gap-y-2 rounded-2xl border border-line bg-surface px-5 py-4">
          <p>
            <span className="text-2xl font-semibold">{formatPercent(overall.percentage, 2)}</span>{' '}
            <span className="text-sm text-muted">overall</span>
          </p>
          <p className="text-sm text-muted">
            {overall.present} of {overall.totalCounted} classes attended across {subjects.length} subjects
          </p>
          <p className={`text-sm font-medium ${overall.below ? 'text-rose-700 dark:text-rose-300' : 'text-emerald-700 dark:text-emerald-300'}`}>
            {overall.below ? `${overall.below} below target` : 'Every subject on target'}
          </p>
        </div>
      )}

      <label className="mb-4 inline-flex cursor-pointer items-center gap-2 text-sm">
        <input
          type="checkbox"
          checked={belowTargetOnly}
          onChange={(e) => setBelowTargetOnly(e.target.checked)}
          className="h-4 w-4 accent-[var(--app-brand)]"
        />
        Show subjects below target only
      </label>

      {error && <ErrorState className="mb-4" message={getErrorMessage(error)} onRetry={reload} />}

      {loading && !subjects ? (
        <Panel bodyClassName="p-5">
          <SkeletonList rows={4} />
        </Panel>
      ) : subjects?.length === 0 ? (
        <Panel>
          {belowTargetOnly ? (
            <EmptyState icon={CalendarCheck} title="Every subject is on target" description="Nothing needs catching up right now." />
          ) : (
            <EmptyState
              icon={CalendarCheck}
              title="Add your first subject"
              description="Add the subjects you attend. Then log each class as present, absent or cancelled."
              action={<Button icon={Plus} onClick={openCreate}>Add subject</Button>}
            />
          )}
        </Panel>
      ) : (
        <ul className="grid gap-4 md:grid-cols-2">
          {subjects?.map((subject) => (
            <li key={subject.id} className="flex flex-col rounded-2xl border border-line bg-surface p-5">
              <div className="flex items-start justify-between gap-3">
                <Link to={`/attendance/${subject.id}`} className="group min-w-0">
                  <span className="flex items-center gap-2">
                    <span className="h-2.5 w-2.5 shrink-0 rounded-full" style={{ backgroundColor: subject.color }} aria-hidden="true" />
                    <span className="truncate font-semibold group-hover:text-brand">{subject.name}</span>
                  </span>
                  {subject.code && <span className="ml-[18px] text-[13px] text-muted">{subject.code}</span>}
                </Link>
                <div className="-mr-1.5 -mt-1 flex shrink-0">
                  <Button variant="ghost" size="icon-sm" icon={Pencil} aria-label={`Edit ${subject.name}`} onClick={() => { setEditing(subject); setFormOpen(true); }} />
                  <Button variant="danger-ghost" size="icon-sm" icon={Trash2} aria-label={`Delete ${subject.name}`} onClick={() => setDeleting(subject)} />
                </div>
              </div>

              <AttendanceMeter stats={subject.attendance} className="mt-4" />

              <div className="mt-5 flex flex-wrap items-center gap-2 border-t border-line pt-4">
                <span className="mr-1 text-[13px] text-muted">Today</span>
                {QUICK_STATUSES.map(({ status, label }) => (
                  <Button
                    key={status}
                    size="sm"
                    variant="secondary"
                    loading={logging === `${subject.id}-${status}`}
                    disabled={Boolean(logging)}
                    onClick={() => quickLog(subject, status, label)}
                  >
                    {label}
                  </Button>
                ))}
                <Link to={`/attendance/${subject.id}`} className="ml-auto text-[13px] font-medium text-brand hover:underline">
                  History
                </Link>
              </div>
            </li>
          ))}
        </ul>
      )}

      <SubjectFormModal
        open={formOpen}
        subject={editing}
        onClose={() => setFormOpen(false)}
        onSaved={() => {
          setFormOpen(false);
          reload();
        }}
      />
      <ConfirmDialog
        open={Boolean(deleting)}
        title={`Delete ${deleting?.name ?? 'subject'}?`}
        message="Its attendance history and timetable classes are deleted too. Assignments, exams, notes and study sessions are kept but no longer linked to a subject."
        confirmLabel="Delete subject"
        loading={deleteBusy}
        onConfirm={confirmDelete}
        onCancel={() => setDeleting(null)}
      />
    </div>
  );
}
