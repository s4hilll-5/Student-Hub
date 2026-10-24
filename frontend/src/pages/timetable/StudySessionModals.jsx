import { useEffect, useState } from 'react';
import toast from 'react-hot-toast';
import { studySessionsApi } from '../../api/services';
import Button from '../../components/ui/Button';
import ErrorState from '../../components/ui/ErrorState';
import FormInput from '../../components/ui/FormInput';
import Modal, { ModalBody, ModalFooter } from '../../components/ui/Modal';
import SelectInput from '../../components/ui/SelectInput';
import TextArea from '../../components/ui/TextArea';
import { subjectOptions } from '../../hooks/useSubjects';
import { formatMinutes, minutesBetween, todayISO, toTimeInput } from '../../utils/date';
import { getErrorMessage, getFieldErrors } from '../../utils/errors';
import { validateTimes } from './ClassFormModal';

export function StudySessionFormModal({ open, session, defaultDate, subjects, onClose, onSaved }) {
  const editing = Boolean(session);
  const [form, setForm] = useState({});
  const [errors, setErrors] = useState({});
  const [formError, setFormError] = useState('');
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (!open) return;
    setErrors({});
    setFormError('');
    setForm({
      title: session?.title ?? '',
      subjectId: session?.subject ? String(session.subject.id) : '',
      sessionDate: session?.sessionDate ?? defaultDate ?? todayISO(),
      startTime: toTimeInput(session?.startTime) || '18:00',
      endTime: toTimeInput(session?.endTime) || '19:00',
      notes: session?.notes ?? '',
    });
  }, [open, session, defaultDate]);

  const update = (field) => (event) => setForm((f) => ({ ...f, [field]: event.target.value }));

  async function handleSubmit(event) {
    event.preventDefault();
    const next = validateTimes(form.startTime, form.endTime);
    if (!form.title.trim()) next.title = "Enter what you'll study";
    if (!form.sessionDate) next.sessionDate = 'Choose a date';
    setErrors(next);
    setFormError('');
    if (Object.keys(next).length) return;
    const payload = {
      title: form.title.trim(),
      subjectId: form.subjectId ? Number(form.subjectId) : null,
      sessionDate: form.sessionDate,
      startTime: form.startTime,
      endTime: form.endTime,
      notes: form.notes.trim() || null,
    };
    setSaving(true);
    try {
      const saved = editing ? await studySessionsApi.update(session.id, payload) : await studySessionsApi.create(payload);
      toast.success(editing ? 'Study session updated' : 'Study session planned');
      onSaved(saved);
    } catch (error) {
      setErrors(getFieldErrors(error));
      setFormError(getErrorMessage(error, "Couldn't save the study session."));
    } finally {
      setSaving(false);
    }
  }

  const planned = form.startTime && form.endTime && form.endTime > form.startTime ? minutesBetween(form.startTime, form.endTime) : 0;

  return (
    <Modal open={open} onClose={saving ? undefined : onClose} title={editing ? 'Edit study session' : 'Plan a study session'}>
      <form onSubmit={handleSubmit} noValidate>
        <ModalBody>
          {formError && <ErrorState message={formError} />}
          <FormInput label="What will you study?" value={form.title ?? ''} onChange={update('title')} error={errors.title} maxLength={150} placeholder="Revise sorting algorithms" required />
          <div className="grid gap-4 sm:grid-cols-2">
            <SelectInput label="Subject" value={form.subjectId ?? ''} onChange={update('subjectId')} options={subjectOptions(subjects)} error={errors.subjectId} />
            <FormInput label="Date" type="date" value={form.sessionDate ?? ''} onChange={update('sessionDate')} error={errors.sessionDate} required />
          </div>
          <div className="grid grid-cols-2 gap-4">
            <FormInput label="Starts" type="time" value={form.startTime ?? ''} onChange={update('startTime')} error={errors.startTime} required />
            <FormInput label="Ends" type="time" value={form.endTime ?? ''} onChange={update('endTime')} error={errors.endTime} hint={planned ? `${formatMinutes(planned)} planned` : undefined} required />
          </div>
          <TextArea label="Notes" value={form.notes ?? ''} onChange={update('notes')} error={errors.notes} maxLength={500} rows={2} />
        </ModalBody>
        <ModalFooter>
          <Button variant="secondary" onClick={onClose} disabled={saving}>
            Cancel
          </Button>
          <Button type="submit" loading={saving}>
            {editing ? 'Save changes' : 'Plan session'}
          </Button>
        </ModalFooter>
      </form>
    </Modal>
  );
}

/** Asks how long the student actually studied (defaults to the planned length). */
export function CompleteSessionModal({ session, onClose, onSaved }) {
  const [minutes, setMinutes] = useState('');
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (session) {
      setMinutes(String(session.plannedMinutes));
      setError('');
    }
  }, [session]);

  async function handleSubmit(event) {
    event.preventDefault();
    const value = Number(minutes);
    if (!Number.isInteger(value) || value < 1 || value > 1440) {
      setError('Enter between 1 and 1440 minutes');
      return;
    }
    setSaving(true);
    try {
      const saved = await studySessionsApi.setCompletion(session.id, { completed: true, actualMinutes: value });
      toast.success(`Logged ${formatMinutes(value)} of study`);
      onSaved(saved);
    } catch (err) {
      setError(getFieldErrors(err).actualMinutes || getErrorMessage(err));
    } finally {
      setSaving(false);
    }
  }

  return (
    <Modal open={Boolean(session)} onClose={saving ? undefined : onClose} title="Mark session as done" description={session?.title} size="sm">
      <form onSubmit={handleSubmit} noValidate>
        <ModalBody>
          <FormInput
            label="Minutes actually studied"
            type="number"
            min={1}
            max={1440}
            value={minutes}
            onChange={(e) => setMinutes(e.target.value)}
            error={error}
            hint={session ? `Planned: ${formatMinutes(session.plannedMinutes)}` : undefined}
            required
          />
        </ModalBody>
        <ModalFooter>
          <Button variant="secondary" onClick={onClose} disabled={saving}>
            Cancel
          </Button>
          <Button type="submit" loading={saving}>
            Log study time
          </Button>
        </ModalFooter>
      </form>
    </Modal>
  );
}
