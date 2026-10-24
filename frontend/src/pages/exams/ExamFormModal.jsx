import { useEffect, useState } from 'react';
import toast from 'react-hot-toast';
import { Trash2 } from 'lucide-react';
import { examsApi } from '../../api/services';
import Button from '../../components/ui/Button';
import ErrorState from '../../components/ui/ErrorState';
import FormInput from '../../components/ui/FormInput';
import Modal, { ModalBody, ModalFooter } from '../../components/ui/Modal';
import SelectInput from '../../components/ui/SelectInput';
import TextArea from '../../components/ui/TextArea';
import { subjectOptions } from '../../hooks/useSubjects';
import { EXAM_TYPES, PREPARATION_STATUSES } from '../../utils/constants';
import { addDays, fromLocalInputValue, toISODate, toLocalInputValue } from '../../utils/date';
import { getErrorMessage, getFieldErrors } from '../../utils/errors';

export function examPayload(exam, overrides = {}) {
  return {
    title: exam.title,
    subjectId: exam.subject?.id ?? null,
    examAt: exam.examAt,
    durationMinutes: exam.durationMinutes,
    examType: exam.examType,
    preparationStatus: exam.preparationStatus,
    location: exam.location,
    syllabusNotes: exam.syllabusNotes,
    ...overrides,
  };
}

export default function ExamFormModal({ open, exam, defaultDate, subjects, onClose, onSaved, onDelete }) {
  const editing = Boolean(exam);
  const [form, setForm] = useState({});
  const [errors, setErrors] = useState({});
  const [formError, setFormError] = useState('');
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (!open) return;
    setErrors({});
    setFormError('');
    const date = defaultDate ?? toISODate(addDays(new Date(), 7));
    setForm({
      title: exam?.title ?? '',
      subjectId: exam?.subject ? String(exam.subject.id) : '',
      examType: exam?.examType ?? 'MIDTERM',
      examAt: exam ? toLocalInputValue(exam.examAt) : `${date}T10:00`,
      durationMinutes: exam?.durationMinutes ? String(exam.durationMinutes) : '180',
      location: exam?.location ?? '',
      preparationStatus: exam?.preparationStatus ?? 'NOT_STARTED',
      syllabusNotes: exam?.syllabusNotes ?? '',
    });
  }, [open, exam, defaultDate]);

  const update = (field) => (event) => setForm((f) => ({ ...f, [field]: event.target.value }));

  async function handleSubmit(event) {
    event.preventDefault();
    const next = {};
    if (!form.title.trim()) next.title = 'Enter the exam name';
    if (!form.examAt) next.examAt = 'Choose the date and time';
    const duration = form.durationMinutes ? Number(form.durationMinutes) : null;
    if (duration !== null && (!Number.isInteger(duration) || duration < 5 || duration > 600)) {
      next.durationMinutes = 'Duration must be between 5 and 600 minutes';
    }
    setErrors(next);
    setFormError('');
    if (Object.keys(next).length) return;

    const payload = {
      title: form.title.trim(),
      subjectId: form.subjectId ? Number(form.subjectId) : null,
      examType: form.examType,
      examAt: fromLocalInputValue(form.examAt),
      durationMinutes: duration,
      location: form.location.trim() || null,
      preparationStatus: form.preparationStatus,
      syllabusNotes: form.syllabusNotes.trim() || null,
    };
    setSaving(true);
    try {
      const saved = editing ? await examsApi.update(exam.id, payload) : await examsApi.create(payload);
      toast.success(editing ? 'Exam updated' : 'Exam added');
      onSaved(saved);
    } catch (error) {
      setErrors(getFieldErrors(error));
      setFormError(getErrorMessage(error, "Couldn't save the exam."));
    } finally {
      setSaving(false);
    }
  }

  return (
    <Modal open={open} onClose={saving ? undefined : onClose} title={editing ? 'Edit exam' : 'Add an exam'} size="lg">
      <form onSubmit={handleSubmit} noValidate>
        <ModalBody>
          {formError && <ErrorState message={formError} />}
          <FormInput label="Exam" value={form.title ?? ''} onChange={update('title')} error={errors.title} maxLength={150} placeholder="Database Systems midterm" required />
          <div className="grid gap-4 sm:grid-cols-2">
            <SelectInput label="Subject" value={form.subjectId ?? ''} onChange={update('subjectId')} options={subjectOptions(subjects)} error={errors.subjectId} />
            <SelectInput label="Type" value={form.examType ?? 'MIDTERM'} onChange={update('examType')} options={EXAM_TYPES} error={errors.examType} required />
          </div>
          <div className="grid gap-4 sm:grid-cols-[1.4fr_1fr]">
            <FormInput label="Date and time" type="datetime-local" value={form.examAt ?? ''} onChange={update('examAt')} error={errors.examAt} required />
            <FormInput label="Duration (minutes)" type="number" min={5} max={600} value={form.durationMinutes ?? ''} onChange={update('durationMinutes')} error={errors.durationMinutes} />
          </div>
          <div className="grid gap-4 sm:grid-cols-2">
            <FormInput label="Location" value={form.location ?? ''} onChange={update('location')} error={errors.location} maxLength={100} placeholder="Exam hall B" />
            <SelectInput label="Preparation" value={form.preparationStatus ?? 'NOT_STARTED'} onChange={update('preparationStatus')} options={PREPARATION_STATUSES} />
          </div>
          <TextArea
            label="Syllabus notes"
            value={form.syllabusNotes ?? ''}
            onChange={update('syllabusNotes')}
            error={errors.syllabusNotes}
            maxLength={5000}
            rows={4}
            placeholder="Units 1 to 4, normalisation, SQL joins"
          />
        </ModalBody>
        <ModalFooter>
          {editing && onDelete && (
            <Button variant="danger-ghost" icon={Trash2} onClick={() => onDelete(exam)} disabled={saving} className="sm:mr-auto">
              Delete exam
            </Button>
          )}
          <Button variant="secondary" onClick={onClose} disabled={saving}>
            Cancel
          </Button>
          <Button type="submit" loading={saving}>
            {editing ? 'Save changes' : 'Add exam'}
          </Button>
        </ModalFooter>
      </form>
    </Modal>
  );
}
