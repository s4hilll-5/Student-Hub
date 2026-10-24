import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import toast from 'react-hot-toast';
import { ArrowLeft, CalendarCheck, Pencil, Trash2 } from 'lucide-react';
import { attendanceApi, subjectsApi } from '../../api/services';
import Badge from '../../components/ui/Badge';
import Button from '../../components/ui/Button';
import ConfirmDialog from '../../components/ui/ConfirmDialog';
import DataTable from '../../components/ui/DataTable';
import EmptyState from '../../components/ui/EmptyState';
import ErrorState from '../../components/ui/ErrorState';
import FormInput from '../../components/ui/FormInput';
import { Panel, Segmented } from '../../components/ui/Layout';
import Modal, { ModalBody, ModalFooter } from '../../components/ui/Modal';
import Pagination from '../../components/ui/Pagination';
import Skeleton from '../../components/ui/Skeleton';
import { useApi } from '../../hooks/useApi';
import { ATTENDANCE_STATUSES, labelOf, toneOf } from '../../utils/constants';
import { formatDate, todayISO } from '../../utils/date';
import { getErrorMessage, getFieldErrors } from '../../utils/errors';
import AttendanceMeter from './AttendanceMeter';
import SubjectFormModal from './SubjectFormModal';

const STATUS_OPTIONS = ATTENDANCE_STATUSES.map(({ value, label }) => ({ value, label }));

/** Date + status + notes form, used inline to add and in a modal to edit. */
function RecordFields({ form, setForm, errors }) {
  return (
    <>
      <div className="grid gap-4 sm:grid-cols-[180px_1fr]">
        <FormInput
          label="Class date"
          type="date"
          max={todayISO()}
          value={form.classDate}
          onChange={(e) => setForm((f) => ({ ...f, classDate: e.target.value }))}
          error={errors.classDate}
          required
        />
        <div>
          <p className="mb-1.5 text-sm font-medium">Status</p>
          <Segmented label="Attendance status" options={STATUS_OPTIONS} value={form.status} onChange={(status) => setForm((f) => ({ ...f, status }))} />
        </div>
      </div>
      <FormInput
        label="Notes"
        value={form.notes}
        onChange={(e) => setForm((f) => ({ ...f, notes: e.target.value }))}
        error={errors.notes}
        maxLength={255}
        placeholder="Optional, e.g. lab session or substitute lecturer"
      />
    </>
  );
}

const emptyRecord = () => ({ classDate: todayISO(), status: 'PRESENT', notes: '' });

export default function SubjectDetailPage() {
  const { subjectId } = useParams();
  const [page, setPage] = useState(0);
  const subjectQuery = useApi(() => subjectsApi.get(subjectId), [subjectId]);
  const historyQuery = useApi(() => subjectsApi.attendance(subjectId, { page, size: 10 }), [subjectId, page]);
  const subject = subjectQuery.data;

  const [form, setForm] = useState(emptyRecord);
  const [errors, setErrors] = useState({});
  const [saving, setSaving] = useState(false);
  const [editingRecord, setEditingRecord] = useState(null);
  const [editForm, setEditForm] = useState(emptyRecord);
  const [editErrors, setEditErrors] = useState({});
  const [deletingRecord, setDeletingRecord] = useState(null);
  const [busy, setBusy] = useState(false);
  const [subjectFormOpen, setSubjectFormOpen] = useState(false);

  const refresh = () => {
    subjectQuery.reload();
    historyQuery.reload();
  };

  async function addRecord(event) {
    event.preventDefault();
    setErrors({});
    if (!form.classDate) {
      setErrors({ classDate: 'Choose the class date' });
      return;
    }
    setSaving(true);
    try {
      await subjectsApi.recordAttendance(subjectId, { ...form, notes: form.notes.trim() || null });
      toast.success(`Recorded ${labelOf(ATTENDANCE_STATUSES, form.status).toLowerCase()} for ${formatDate(form.classDate)}`);
      setForm(emptyRecord());
      if (page !== 0) setPage(0);
      refresh();
    } catch (err) {
      setErrors(getFieldErrors(err));
      toast.error(getErrorMessage(err));
    } finally {
      setSaving(false);
    }
  }

  function startEdit(record) {
    setEditErrors({});
    setEditForm({ classDate: record.classDate, status: record.status, notes: record.notes ?? '' });
    setEditingRecord(record);
  }

  async function saveEdit(event) {
    event.preventDefault();
    setBusy(true);
    try {
      await attendanceApi.update(editingRecord.id, { ...editForm, notes: editForm.notes.trim() || null });
      toast.success('Record updated');
      setEditingRecord(null);
      refresh();
    } catch (err) {
      setEditErrors(getFieldErrors(err));
      toast.error(getErrorMessage(err));
    } finally {
      setBusy(false);
    }
  }

  async function confirmDelete() {
    setBusy(true);
    try {
      await attendanceApi.remove(deletingRecord.id);
      toast.success('Record deleted');
      setDeletingRecord(null);
      refresh();
    } catch (err) {
      toast.error(getErrorMessage(err));
    } finally {
      setBusy(false);
    }
  }

  if (subjectQuery.error && !subject) {
    return (
      <div className="space-y-4">
        <Link to="/attendance" className="inline-flex items-center gap-1.5 text-sm text-muted hover:text-ink">
          <ArrowLeft size={16} /> Attendance
        </Link>
        <ErrorState message={getErrorMessage(subjectQuery.error)} onRetry={subjectQuery.reload} />
      </div>
    );
  }

  const stats = subject?.attendance;
  const history = historyQuery.data;

  return (
    <div className="space-y-6">
      <Link to="/attendance" className="inline-flex items-center gap-1.5 text-sm text-muted hover:text-ink">
        <ArrowLeft size={16} aria-hidden="true" /> All subjects
      </Link>

      {!subject ? (
        <Skeleton className="h-48 rounded-2xl" />
      ) : (
        <>
          <div className="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
            <div>
              <div className="flex items-center gap-2.5">
                <span className="h-3 w-3 rounded-full" style={{ backgroundColor: subject.color }} aria-hidden="true" />
                <h1 className="text-[28px] font-semibold leading-tight">{subject.name}</h1>
              </div>
              {subject.code && <p className="ml-[22px] text-muted">{subject.code}</p>}
            </div>
            <Button variant="secondary" icon={Pencil} onClick={() => setSubjectFormOpen(true)}>
              Edit subject
            </Button>
          </div>

          <Panel bodyClassName="grid gap-6 p-5 lg:grid-cols-[1.3fr_1fr]">
            <AttendanceMeter stats={stats} />
            <dl className="grid grid-cols-3 gap-4 text-sm">
              {[
                ['Present', stats.present],
                ['Absent', stats.absent],
                ['Cancelled', stats.cancelled],
                ['Counted', stats.totalCounted],
                ['Can miss', stats.meetsTarget ? stats.classesYouCanMiss : 0],
                ['Needed', stats.targetReachable ? stats.classesNeededToReachTarget : '—'],
              ].map(([label, value]) => (
                <div key={label}>
                  <dt className="text-muted">{label}</dt>
                  <dd className="mt-0.5 text-xl font-semibold">{value}</dd>
                </div>
              ))}
            </dl>
            {(subject.priorTotal > 0) && (
              <p className="text-[13px] text-muted lg:col-span-2">
                Includes {subject.priorAttended} of {subject.priorTotal} classes attended before you started tracking.
              </p>
            )}
          </Panel>
        </>
      )}

      <Panel title="Record a class" bodyClassName="p-5 pt-3">
        <form onSubmit={addRecord} noValidate className="space-y-4">
          <RecordFields form={form} setForm={setForm} errors={errors} />
          <Button type="submit" loading={saving} icon={CalendarCheck}>
            Record attendance
          </Button>
        </form>
      </Panel>

      <Panel title="History" bodyClassName="pt-2">
        {historyQuery.error && <ErrorState className="m-5" message={getErrorMessage(historyQuery.error)} onRetry={historyQuery.reload} />}
        <DataTable
          caption="Attendance history"
          rows={history?.content ?? []}
          loading={historyQuery.loading && !history}
          columns={[
            { key: 'classDate', header: 'Date', render: (r) => formatDate(r.classDate, { weekday: 'short', day: 'numeric', month: 'short', year: 'numeric' }) },
            { key: 'status', header: 'Status', render: (r) => <Badge tone={toneOf(ATTENDANCE_STATUSES, r.status)}>{labelOf(ATTENDANCE_STATUSES, r.status)}</Badge> },
            { key: 'notes', header: 'Notes', render: (r) => <span className="text-muted">{r.notes || '—'}</span> },
            {
              key: 'actions',
              header: <span className="sr-only">Actions</span>,
              className: 'w-24 text-right',
              render: (r) => (
                <div className="flex justify-end gap-1">
                  <Button variant="ghost" size="icon-sm" icon={Pencil} aria-label={`Edit record for ${r.classDate}`} onClick={() => startEdit(r)} />
                  <Button variant="danger-ghost" size="icon-sm" icon={Trash2} aria-label={`Delete record for ${r.classDate}`} onClick={() => setDeletingRecord(r)} />
                </div>
              ),
            },
          ]}
          empty={<EmptyState icon={CalendarCheck} title="No classes recorded yet" description="Use the form above after each class." />}
        />
        {history && <Pagination page={history.page} size={history.size} totalElements={history.totalElements} totalPages={history.totalPages} onPageChange={setPage} />}
      </Panel>

      <Modal open={Boolean(editingRecord)} onClose={busy ? undefined : () => setEditingRecord(null)} title="Edit attendance record">
        <form onSubmit={saveEdit} noValidate>
          <ModalBody>
            <RecordFields form={editForm} setForm={setEditForm} errors={editErrors} />
          </ModalBody>
          <ModalFooter>
            <Button variant="secondary" onClick={() => setEditingRecord(null)} disabled={busy}>
              Cancel
            </Button>
            <Button type="submit" loading={busy}>
              Save changes
            </Button>
          </ModalFooter>
        </form>
      </Modal>
      <ConfirmDialog
        open={Boolean(deletingRecord)}
        title="Delete this record?"
        message={deletingRecord ? `The ${labelOf(ATTENDANCE_STATUSES, deletingRecord.status).toLowerCase()} record for ${formatDate(deletingRecord.classDate)} will be removed and your percentage recalculated.` : ''}
        confirmLabel="Delete record"
        loading={busy}
        onConfirm={confirmDelete}
        onCancel={() => setDeletingRecord(null)}
      />
      <SubjectFormModal
        open={subjectFormOpen}
        subject={subject}
        onClose={() => setSubjectFormOpen(false)}
        onSaved={() => {
          setSubjectFormOpen(false);
          subjectQuery.reload();
        }}
      />
    </div>
  );
}
