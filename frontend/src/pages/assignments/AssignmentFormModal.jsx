import { useEffect, useState } from 'react';
import toast from 'react-hot-toast';
import { assignmentsApi } from '../../api/services';
import Button from '../../components/ui/Button';
import ErrorState from '../../components/ui/ErrorState';
import FormInput from '../../components/ui/FormInput';
import Modal, { ModalBody, ModalFooter } from '../../components/ui/Modal';
import SelectInput from '../../components/ui/SelectInput';
import TextArea from '../../components/ui/TextArea';
import { subjectOptions } from '../../hooks/useSubjects';
import { ASSIGNMENT_STATUSES, PRIORITIES } from '../../utils/constants';
import { fromLocalInputValue, toISODate, toLocalInputValue } from '../../utils/date';
import { getErrorMessage, getFieldErrors } from '../../utils/errors';

function emptyForm() {
  // Default due date: tomorrow at 23:59, the most common hand-in time.
  const tomorrow = new Date();
  tomorrow.setDate(tomorrow.getDate() + 1);
  return {
    title: '',
    description: '',
    subjectId: '',
    dueAt: `${toISODate(tomorrow)}T23:59`,
    priority: 'MEDIUM',
    status: 'PENDING',
  };
}

export default function AssignmentFormModal({ open, assignment, subjects, onClose, onSaved }) {
  const editing = Boolean(assignment);
  const [form, setForm] = useState(emptyForm);
  const [errors, setErrors] = useState({});
  const [formError, setFormError] = useState('');
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (!open) return;
    setErrors({});
    setFormError('');
    setForm(
      assignment
        ? {
            title: assignment.title,
            description: assignment.description ?? '',
            subjectId: assignment.subject ? String(assignment.subject.id) : '',
            dueAt: toLocalInputValue(assignment.dueAt),
            priority: assignment.priority,
            status: assignment.status,
          }
        : emptyForm(),
    );
  }, [open, assignment]);

  const update = (field) => (event) => setForm((f) => ({ ...f, [field]: event.target.value }));

  async function handleSubmit(event) {
    event.preventDefault();
    const next = {};
    if (!form.title.trim()) next.title = 'Enter a title';
    if (!form.dueAt) next.dueAt = 'Choose a due date and time';
    setErrors(next);
    setFormError('');
    if (Object.keys(next).length) return;

    const payload = {
      title: form.title.trim(),
      description: form.description.trim() || null,
      subjectId: form.subjectId ? Number(form.subjectId) : null,
      dueAt: fromLocalInputValue(form.dueAt),
      priority: form.priority,
      status: form.status,
    };
    setSaving(true);
    try {
      const saved = editing ? await assignmentsApi.update(assignment.id, payload) : await assignmentsApi.create(payload);
      toast.success(editing ? 'Assignment updated' : 'Assignment added');
      onSaved(saved);
    } catch (error) {
      setErrors(getFieldErrors(error));
      setFormError(getErrorMessage(error, "Couldn't save the assignment."));
    } finally {
      setSaving(false);
    }
  }

  return (
    <Modal open={open} onClose={saving ? undefined : onClose} title={editing ? 'Edit assignment' : 'New assignment'}>
      <form onSubmit={handleSubmit} noValidate>
        <ModalBody>
          {formError && <ErrorState message={formError} />}
          <FormInput label="Title" value={form.title} onChange={update('title')} error={errors.title} maxLength={150} required />
          <div className="grid gap-4 sm:grid-cols-2">
            <SelectInput
              label="Subject"
              value={form.subjectId}
              onChange={update('subjectId')}
              options={subjectOptions(subjects)}
              error={errors.subjectId}
            />
            <FormInput
              label="Due"
              type="datetime-local"
              value={form.dueAt}
              onChange={update('dueAt')}
              error={errors.dueAt}
              required
            />
          </div>
          <div className="grid gap-4 sm:grid-cols-2">
            <SelectInput label="Priority" value={form.priority} onChange={update('priority')} options={PRIORITIES} error={errors.priority} required />
            <SelectInput label="Status" value={form.status} onChange={update('status')} options={ASSIGNMENT_STATUSES} error={errors.status} />
          </div>
          <TextArea
            label="Description"
            value={form.description}
            onChange={update('description')}
            error={errors.description}
            maxLength={5000}
            rows={4}
            placeholder="Instructions, page numbers, submission link"
          />
        </ModalBody>
        <ModalFooter>
          <Button variant="secondary" onClick={onClose} disabled={saving}>
            Cancel
          </Button>
          <Button type="submit" loading={saving}>
            {editing ? 'Save changes' : 'Add assignment'}
          </Button>
        </ModalFooter>
      </form>
    </Modal>
  );
}
