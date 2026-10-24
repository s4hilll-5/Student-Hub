import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import toast from 'react-hot-toast';
import { Trash2 } from 'lucide-react';
import { timetableApi } from '../../api/services';
import Button from '../../components/ui/Button';
import ErrorState from '../../components/ui/ErrorState';
import FormInput from '../../components/ui/FormInput';
import Modal, { ModalBody, ModalFooter } from '../../components/ui/Modal';
import SelectInput from '../../components/ui/SelectInput';
import { subjectOptions } from '../../hooks/useSubjects';
import { DAY_LABELS, DAYS, toTimeInput } from '../../utils/date';
import { getErrorMessage, getFieldErrors } from '../../utils/errors';

export function validateTimes(start, end) {
  const errors = {};
  if (!start) errors.startTime = 'Choose a start time';
  if (!end) errors.endTime = 'Choose an end time';
  if (start && end && end <= start) errors.endTime = 'End time must be after start time';
  return errors;
}

export default function ClassFormModal({ open, entry, defaultDay, subjects, onClose, onSaved, onDelete }) {
  const editing = Boolean(entry);
  const [form, setForm] = useState({});
  const [errors, setErrors] = useState({});
  const [formError, setFormError] = useState('');
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (!open) return;
    setErrors({});
    setFormError('');
    setForm({
      subjectId: entry ? String(entry.subject.id) : subjects[0] ? String(subjects[0].id) : '',
      dayOfWeek: entry?.dayOfWeek ?? defaultDay ?? 'MONDAY',
      startTime: toTimeInput(entry?.startTime) || '09:00',
      endTime: toTimeInput(entry?.endTime) || '10:00',
      location: entry?.location ?? '',
    });
  }, [open, entry, defaultDay, subjects]);

  const update = (field) => (event) => setForm((f) => ({ ...f, [field]: event.target.value }));

  async function handleSubmit(event) {
    event.preventDefault();
    const next = validateTimes(form.startTime, form.endTime);
    if (!form.subjectId) next.subjectId = 'Choose a subject';
    setErrors(next);
    setFormError('');
    if (Object.keys(next).length) return;
    const payload = {
      subjectId: Number(form.subjectId),
      dayOfWeek: form.dayOfWeek,
      startTime: form.startTime,
      endTime: form.endTime,
      location: form.location.trim() || null,
    };
    setSaving(true);
    try {
      const saved = editing ? await timetableApi.update(entry.id, payload) : await timetableApi.create(payload);
      toast.success(editing ? 'Class updated' : 'Class added to your timetable');
      onSaved(saved);
    } catch (error) {
      setErrors(getFieldErrors(error));
      setFormError(getErrorMessage(error, "Couldn't save the class."));
    } finally {
      setSaving(false);
    }
  }

  const noSubjects = subjects.length === 0;

  return (
    <Modal open={open} onClose={saving ? undefined : onClose} title={editing ? 'Edit class' : 'Add a class'}>
      <form onSubmit={handleSubmit} noValidate>
        <ModalBody>
          {noSubjects ? (
            <p className="text-sm text-muted">
              Classes belong to a subject.{' '}
              <Link to="/attendance?new=1" className="font-medium text-brand hover:underline">
                Add a subject first
              </Link>
              .
            </p>
          ) : (
            <>
              {formError && <ErrorState message={formError} />}
              <SelectInput label="Subject" value={form.subjectId ?? ''} onChange={update('subjectId')} options={subjectOptions(subjects, { includeNone: false })} error={errors.subjectId} required />
              <SelectInput label="Day" value={form.dayOfWeek ?? 'MONDAY'} onChange={update('dayOfWeek')} options={DAYS.map((d) => ({ value: d, label: DAY_LABELS[d] }))} required />
              <div className="grid grid-cols-2 gap-4">
                <FormInput label="Starts" type="time" value={form.startTime ?? ''} onChange={update('startTime')} error={errors.startTime} required />
                <FormInput label="Ends" type="time" value={form.endTime ?? ''} onChange={update('endTime')} error={errors.endTime} required />
              </div>
              <FormInput label="Room or location" value={form.location ?? ''} onChange={update('location')} error={errors.location} maxLength={100} placeholder="Room 204, Lab 3" />
            </>
          )}
        </ModalBody>
        <ModalFooter>
          {editing && onDelete && (
            <Button variant="danger-ghost" icon={Trash2} onClick={() => onDelete(entry)} disabled={saving} className="sm:mr-auto">
              Remove class
            </Button>
          )}
          <Button variant="secondary" onClick={onClose} disabled={saving}>
            Cancel
          </Button>
          <Button type="submit" loading={saving} disabled={noSubjects}>
            {editing ? 'Save changes' : 'Add class'}
          </Button>
        </ModalFooter>
      </form>
    </Modal>
  );
}
