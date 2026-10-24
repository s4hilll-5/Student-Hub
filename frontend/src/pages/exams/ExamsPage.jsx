import { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import toast from 'react-hot-toast';
import { Clock, GraduationCap, MapPin, Pencil, Plus } from 'lucide-react';
import { examsApi } from '../../api/services';
import Badge, { SubjectTag } from '../../components/ui/Badge';
import Button from '../../components/ui/Button';
import ConfirmDialog from '../../components/ui/ConfirmDialog';
import EmptyState from '../../components/ui/EmptyState';
import ErrorState from '../../components/ui/ErrorState';
import { PageHeader, Panel, Segmented } from '../../components/ui/Layout';
import SelectInput from '../../components/ui/SelectInput';
import { SkeletonList } from '../../components/ui/Skeleton';
import { cx } from '../../components/ui/cx';
import { useApi } from '../../hooks/useApi';
import { useSubjects } from '../../hooks/useSubjects';
import { EXAM_TYPES, labelOf, PREPARATION_STATUSES } from '../../utils/constants';
import { calendarDaysUntil, countdown, formatDateTime } from '../../utils/date';
import { getErrorMessage } from '../../utils/errors';
import ExamCalendar from './ExamCalendar';
import ExamFormModal, { examPayload } from './ExamFormModal';

function CountdownBlock({ examAt }) {
  const days = calendarDaysUntil(examAt);
  const soon = days <= 3;
  return (
    <div className={cx('w-[72px] shrink-0 rounded-xl px-2 py-2 text-center', soon ? 'bg-rose-50 dark:bg-rose-500/10' : 'bg-brand-soft')}>
      <p className={cx('text-2xl font-semibold leading-none', soon ? 'text-rose-700 dark:text-rose-300' : 'text-brand')}>{Math.max(days, 0)}</p>
      <p className="mt-1 text-xs text-muted">{days === 1 ? 'day' : 'days'}</p>
    </div>
  );
}

function ExamRow({ exam, past, onEdit, onPrepChange }) {
  return (
    <li className="flex flex-col gap-3 px-5 py-4 sm:flex-row sm:items-center">
      {!past && <CountdownBlock examAt={exam.examAt} />}
      <div className="min-w-0 flex-1">
        <div className="flex flex-wrap items-center gap-2">
          <p className="font-semibold">{exam.title}</p>
          <Badge>{labelOf(EXAM_TYPES, exam.examType)}</Badge>
        </div>
        <div className="mt-1 flex flex-wrap items-center gap-x-4 gap-y-1 text-[13px] text-muted">
          <SubjectTag subject={exam.subject} className="text-[13px]" />
          <span className="flex items-center gap-1">
            <Clock size={13} aria-hidden="true" />
            {formatDateTime(exam.examAt)}
            {!past && <span className="font-medium text-ink">, {countdown(exam.examAt).label.toLowerCase()}</span>}
          </span>
          {exam.location && (
            <span className="flex items-center gap-1">
              <MapPin size={13} aria-hidden="true" />
              {exam.location}
            </span>
          )}
        </div>
        {exam.syllabusNotes && <p className="mt-2 line-clamp-2 max-w-2xl text-sm text-muted">{exam.syllabusNotes}</p>}
      </div>
      <div className="flex items-center gap-2">
        <SelectInput
          aria-label={`Preparation for ${exam.title}`}
          value={exam.preparationStatus}
          onChange={(e) => onPrepChange(exam, e.target.value)}
          options={PREPARATION_STATUSES}
          className="w-36"
        />
        <Button variant="ghost" size="icon-sm" icon={Pencil} aria-label={`Edit ${exam.title}`} onClick={() => onEdit(exam)} />
      </div>
    </li>
  );
}

export default function ExamsPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const { subjects } = useSubjects();
  const [view, setView] = useState('list');
  const [showPast, setShowPast] = useState(false);
  const [refreshKey, setRefreshKey] = useState(0);
  const upcoming = useApi(() => examsApi.list({ upcoming: true, size: 50, sort: 'examAt,asc' }), [refreshKey]);
  const past = useApi(() => (showPast ? examsApi.list({ upcoming: false, size: 50, sort: 'examAt,desc' }) : Promise.resolve(null)), [showPast, refreshKey]);

  const [modal, setModal] = useState({ open: false, exam: null, date: null });
  const [deleting, setDeleting] = useState(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    if (searchParams.get('new') === '1') {
      setModal({ open: true, exam: null, date: null });
      searchParams.delete('new');
      setSearchParams(searchParams, { replace: true });
    }
  }, [searchParams, setSearchParams]);

  const refresh = () => setRefreshKey((k) => k + 1);

  async function changePreparation(exam, preparationStatus) {
    try {
      await examsApi.update(exam.id, examPayload(exam, { preparationStatus }));
      toast.success(`${exam.title}: ${labelOf(PREPARATION_STATUSES, preparationStatus).toLowerCase()}`);
      refresh();
    } catch (err) {
      toast.error(getErrorMessage(err));
    }
  }

  async function confirmDelete() {
    setBusy(true);
    try {
      await examsApi.remove(deleting.id);
      toast.success('Exam deleted');
      setDeleting(null);
      refresh();
    } catch (err) {
      toast.error(getErrorMessage(err));
    } finally {
      setBusy(false);
    }
  }

  const openEdit = (exam) => setModal({ open: true, exam, date: null });
  const list = upcoming.data?.content ?? [];

  return (
    <div className="space-y-6">
      <PageHeader
        title="Exams"
        description="Countdowns to every paper, with your preparation status at a glance."
        actions={
          <Button icon={Plus} onClick={() => setModal({ open: true, exam: null, date: null })}>
            Add exam
          </Button>
        }
      />
      <Segmented label="Exam view" value={view} onChange={setView} options={[{ value: 'list', label: 'Upcoming' }, { value: 'calendar', label: 'Calendar' }]} />

      {view === 'calendar' ? (
        <ExamCalendar refreshKey={refreshKey} onOpenExam={openEdit} onAddOnDate={(date) => setModal({ open: true, exam: null, date })} />
      ) : (
        <>
          {upcoming.error && <ErrorState message={getErrorMessage(upcoming.error)} onRetry={upcoming.reload} />}
          <Panel>
            {upcoming.loading && !upcoming.data ? (
              <div className="p-5">
                <SkeletonList rows={3} />
              </div>
            ) : list.length === 0 ? (
              <EmptyState
                icon={GraduationCap}
                title="No upcoming exams"
                description="Add your exam dates to get countdowns on your dashboard."
                action={<Button icon={Plus} onClick={() => setModal({ open: true, exam: null, date: null })}>Add exam</Button>}
              />
            ) : (
              <ul className="divide-y divide-line">
                {list.map((exam) => (
                  <ExamRow key={exam.id} exam={exam} onEdit={openEdit} onPrepChange={changePreparation} />
                ))}
              </ul>
            )}
          </Panel>

          <div>
            <Button variant="ghost" onClick={() => setShowPast((v) => !v)} aria-expanded={showPast}>
              {showPast ? 'Hide past exams' : 'Show past exams'}
            </Button>
            {showPast && (
              <Panel className="mt-3">
                {past.loading && !past.data ? (
                  <div className="p-5">
                    <SkeletonList rows={2} />
                  </div>
                ) : (past.data?.content ?? []).length === 0 ? (
                  <p className="px-5 py-6 text-sm text-muted">No past exams yet.</p>
                ) : (
                  <ul className="divide-y divide-line">
                    {past.data.content.map((exam) => (
                      <ExamRow key={exam.id} exam={exam} past onEdit={openEdit} onPrepChange={changePreparation} />
                    ))}
                  </ul>
                )}
              </Panel>
            )}
          </div>
        </>
      )}

      <ExamFormModal
        open={modal.open}
        exam={modal.exam}
        defaultDate={modal.date}
        subjects={subjects}
        onClose={() => setModal({ open: false, exam: null, date: null })}
        onDelete={(exam) => {
          setModal({ open: false, exam: null, date: null });
          setDeleting(exam);
        }}
        onSaved={() => {
          setModal({ open: false, exam: null, date: null });
          refresh();
        }}
      />
      <ConfirmDialog
        open={Boolean(deleting)}
        title="Delete this exam?"
        message={deleting ? `"${deleting.title}" will be removed from your exams and calendar.` : ''}
        confirmLabel="Delete exam"
        loading={busy}
        onConfirm={confirmDelete}
        onCancel={() => setDeleting(null)}
      />
    </div>
  );
}
