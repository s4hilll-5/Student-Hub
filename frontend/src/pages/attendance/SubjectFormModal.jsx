import { useEffect, useState } from 'react';
import toast from 'react-hot-toast';
import { Check } from 'lucide-react';
import { subjectsApi } from '../../api/services';
import Button from '../../components/ui/Button';
import ErrorState from '../../components/ui/ErrorState';
import FormInput from '../../components/ui/FormInput';
import Modal, { ModalBody, ModalFooter } from '../../components/ui/Modal';
import { useAuth } from '../../context/AuthContext';
import { SUBJECT_COLORS } from '../../utils/constants';
import { getErrorMessage, getFieldErrors } from '../../utils/errors';

export default function SubjectFormModal({ open, subject, onClose, onSaved }) {
  const { user } = useAuth();
  const editing = Boolean(subject);
  const [form, setForm] = useState({});
  const [errors, setErrors] = useState({});
  const [formError, setFormError] = useState('');
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (!open) return;
    setErrors({});
    setFormError('');
    setForm({
      name: subject?.name ?? '',
      code: subject?.code ?? '',
      color: subject?.color ?? SUBJECT_COLORS[0],
      targetPercentage: String(Number(subject?.targetPercentage ?? user?.attendanceTarget ?? 75)),
      priorAttended: String(subject?.priorAttended ?? 0),
      priorTotal: String(subject?.priorTotal ?? 0),
    });
  }, [open, subject, user]);

  const update = (field) => (event) => setForm((f) => ({ ...f, [field]: event.target.value }));

  function validate() {
    const next = {};
    if (!form.name.trim()) next.name = 'Enter a subject name';
    const target = Number(form.targetPercentage);
    if (!form.targetPercentage || Number.isNaN(target) || target < 1 || target > 100) next.targetPercentage = 'Target must be between 1 and 100';
    const attended = Number(form.priorAttended || 0);
    const total = Number(form.priorTotal || 0);
    if (!Number.isInteger(attended) || attended < 0) next.priorAttended = 'Use a whole number, 0 or more';
    if (!Number.isInteger(total) || total < 0) next.priorTotal = 'Use a whole number, 0 or more';
    if (!next.priorAttended && !next.priorTotal && attended > total) next.priorAttended = 'Classes attended cannot be more than classes held';
    return next;
  }

  async function handleSubmit(event) {
    event.preventDefault();
    const next = validate();
    setErrors(next);
    setFormError('');
    if (Object.keys(next).length) return;
    const payload = {
      name: form.name.trim(),
      code: form.code.trim() || null,
      color: form.color,
      targetPercentage: Number(form.targetPercentage),
      priorAttended: Number(form.priorAttended || 0),
      priorTotal: Number(form.priorTotal || 0),
    };
    setSaving(true);
    try {
      const saved = editing ? await subjectsApi.update(subject.id, payload) : await subjectsApi.create(payload);
      toast.success(editing ? 'Subject updated' : `${saved.name} added`);
      onSaved(saved);
    } catch (error) {
      setErrors(getFieldErrors(error));
      setFormError(getErrorMessage(error, "Couldn't save the subject."));
    } finally {
      setSaving(false);
    }
  }

  return (
    <Modal open={open} onClose={saving ? undefined : onClose} title={editing ? 'Edit subject' : 'Add a subject'}>
      <form onSubmit={handleSubmit} noValidate>
        <ModalBody>
          {formError && <ErrorState message={formError} />}
          <div className="grid gap-4 sm:grid-cols-[1fr_140px]">
            <FormInput label="Subject name" value={form.name ?? ''} onChange={update('name')} error={errors.name} maxLength={100} required />
            <FormInput label="Code" value={form.code ?? ''} onChange={update('code')} error={errors.code} maxLength={20} placeholder="CS-201" />
          </div>

          <fieldset>
            <legend className="mb-2 text-sm font-medium">Colour</legend>
            <div className="flex flex-wrap gap-2">
              {SUBJECT_COLORS.map((color) => (
                <label key={color} className="cursor-pointer">
                  <input
                    type="radio"
                    name="subject-color"
                    value={color}
                    checked={form.color === color}
                    onChange={update('color')}
                    className="peer sr-only"
                  />
                  <span
                    className="grid h-8 w-8 place-items-center rounded-full text-white ring-offset-2 ring-offset-surface peer-checked:ring-2 peer-checked:ring-ink peer-focus-visible:ring-2 peer-focus-visible:ring-brand"
                    style={{ backgroundColor: color }}
                  >
                    {form.color === color && <Check size={15} strokeWidth={3} aria-hidden="true" />}
                    <span className="sr-only">{color}</span>
                  </span>
                </label>
              ))}
            </div>
          </fieldset>

          <FormInput
            label="Attendance target (%)"
            type="number"
            min={1}
            max={100}
            step="0.01"
            value={form.targetPercentage ?? ''}
            onChange={update('targetPercentage')}
            error={errors.targetPercentage}
            hint="Most colleges require 75%."
            required
          />

          <fieldset className="rounded-xl border border-line p-4">
            <legend className="px-1 text-sm font-medium">Classes before you started tracking</legend>
            <p className="mb-3 text-[13px] text-muted">
              Optional. If the term is already under way, enter the totals so far and StudentHub will include them.
            </p>
            <div className="grid grid-cols-2 gap-4">
              <FormInput label="Attended" type="number" min={0} step={1} value={form.priorAttended ?? ''} onChange={update('priorAttended')} error={errors.priorAttended} />
              <FormInput label="Held" type="number" min={0} step={1} value={form.priorTotal ?? ''} onChange={update('priorTotal')} error={errors.priorTotal} />
            </div>
          </fieldset>
        </ModalBody>
        <ModalFooter>
          <Button variant="secondary" onClick={onClose} disabled={saving}>
            Cancel
          </Button>
          <Button type="submit" loading={saving}>
            {editing ? 'Save changes' : 'Add subject'}
          </Button>
        </ModalFooter>
      </form>
    </Modal>
  );
}
