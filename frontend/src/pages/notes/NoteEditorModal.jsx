import { useEffect, useState } from 'react';
import toast from 'react-hot-toast';
import { notesApi } from '../../api/services';
import Button from '../../components/ui/Button';
import ErrorState from '../../components/ui/ErrorState';
import FormInput from '../../components/ui/FormInput';
import Modal, { ModalBody, ModalFooter } from '../../components/ui/Modal';
import SelectInput from '../../components/ui/SelectInput';
import TextArea from '../../components/ui/TextArea';
import { subjectOptions } from '../../hooks/useSubjects';
import { getErrorMessage, getFieldErrors } from '../../utils/errors';

/** "Exam Prep, sql , joins" -> ["exam prep", "sql", "joins"] */
export function parseTags(text) {
  const seen = new Set();
  return text
    .split(',')
    .map((t) => t.trim().replace(/\s+/g, ' ').toLowerCase())
    .filter((t) => t && !seen.has(t) && seen.add(t));
}

export default function NoteEditorModal({ open, note, subjects, onClose, onSaved }) {
  const editing = Boolean(note);
  const [form, setForm] = useState({});
  const [errors, setErrors] = useState({});
  const [formError, setFormError] = useState('');
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (!open) return;
    setErrors({});
    setFormError('');
    setForm({
      title: note?.title ?? '',
      content: note?.content ?? '',
      subjectId: note?.subject ? String(note.subject.id) : '',
      tags: note?.tags?.join(', ') ?? '',
      pinned: note?.pinned ?? false,
    });
  }, [open, note]);

  const update = (field) => (event) => setForm((f) => ({ ...f, [field]: event.target.value }));

  async function handleSubmit(event) {
    event.preventDefault();
    const tags = parseTags(form.tags);
    const next = {};
    if (!form.title.trim()) next.title = 'Enter a title';
    if (!form.content.trim()) next.content = 'Write something in the note';
    if (tags.length > 10) next.tags = 'Use up to 10 tags';
    else if (tags.some((t) => t.length > 30)) next.tags = 'Each tag must be 30 characters or fewer';
    else if (tags.some((t) => !/^[\p{L}\p{N} _-]+$/u.test(t))) next.tags = 'Tags can use letters, numbers, spaces, - and _';
    setErrors(next);
    setFormError('');
    if (Object.keys(next).length) return;

    const payload = {
      title: form.title.trim(),
      content: form.content,
      subjectId: form.subjectId ? Number(form.subjectId) : null,
      tags,
      pinned: form.pinned,
    };
    setSaving(true);
    try {
      const saved = editing ? await notesApi.update(note.id, payload) : await notesApi.create(payload);
      toast.success(editing ? 'Note saved' : 'Note added');
      onSaved(saved);
    } catch (error) {
      const fields = getFieldErrors(error);
      const tagError = Object.entries(fields).find(([key]) => key.startsWith('tags'));
      setErrors({ ...fields, ...(tagError ? { tags: tagError[1] } : {}) });
      setFormError(getErrorMessage(error, "Couldn't save the note."));
    } finally {
      setSaving(false);
    }
  }

  return (
    <Modal open={open} onClose={saving ? undefined : onClose} title={editing ? 'Edit note' : 'New note'} size="lg">
      <form onSubmit={handleSubmit} noValidate>
        <ModalBody>
          {formError && <ErrorState message={formError} />}
          <FormInput label="Title" value={form.title ?? ''} onChange={update('title')} error={errors.title} maxLength={150} required />
          <div className="grid gap-4 sm:grid-cols-2">
            <SelectInput label="Subject" value={form.subjectId ?? ''} onChange={update('subjectId')} options={subjectOptions(subjects)} error={errors.subjectId} />
            <FormInput label="Tags" value={form.tags ?? ''} onChange={update('tags')} error={errors.tags} hint="Separate with commas" placeholder="exam prep, formulas" />
          </div>
          <TextArea label="Note" value={form.content ?? ''} onChange={update('content')} error={errors.content} rows={10} maxLength={50000} required />
          <label className="inline-flex cursor-pointer items-center gap-2 text-sm">
            <input
              type="checkbox"
              checked={Boolean(form.pinned)}
              onChange={(e) => setForm((f) => ({ ...f, pinned: e.target.checked }))}
              className="h-4 w-4 accent-[var(--app-brand)]"
            />
            Pin to the top of your notes
          </label>
        </ModalBody>
        <ModalFooter>
          <Button variant="secondary" onClick={onClose} disabled={saving}>
            Cancel
          </Button>
          <Button type="submit" loading={saving}>
            {editing ? 'Save note' : 'Add note'}
          </Button>
        </ModalFooter>
      </form>
    </Modal>
  );
}
