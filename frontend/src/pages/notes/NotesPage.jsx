import { useEffect, useMemo, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import toast from 'react-hot-toast';
import { NotebookPen, Pencil, Pin, PinOff, Plus, Trash2 } from 'lucide-react';
import { notesApi } from '../../api/services';
import { SubjectTag } from '../../components/ui/Badge';
import Button from '../../components/ui/Button';
import ConfirmDialog from '../../components/ui/ConfirmDialog';
import EmptyState from '../../components/ui/EmptyState';
import ErrorState from '../../components/ui/ErrorState';
import { PageHeader, Panel, SearchInput } from '../../components/ui/Layout';
import Modal, { ModalBody, ModalFooter } from '../../components/ui/Modal';
import Pagination from '../../components/ui/Pagination';
import SelectInput from '../../components/ui/SelectInput';
import Skeleton from '../../components/ui/Skeleton';
import { cx } from '../../components/ui/cx';
import { useApi } from '../../hooks/useApi';
import { useDebounce } from '../../hooks/useDebounce';
import { subjectOptions, useSubjects } from '../../hooks/useSubjects';
import { formatDate, formatDateTime } from '../../utils/date';
import { getErrorMessage } from '../../utils/errors';
import NoteEditorModal from './NoteEditorModal';

const PAGE_SIZE = 12;

function TagList({ tags, activeTag, onSelect }) {
  if (!tags?.length) return null;
  return (
    <ul className="flex flex-wrap gap-1.5">
      {tags.map((tag) => (
        <li key={tag}>
          {onSelect ? (
            <button
              type="button"
              onClick={() => onSelect(tag)}
              className={cx('rounded-md px-2 py-0.5 text-xs font-medium', tag === activeTag ? 'bg-brand text-white dark:text-[#14132a]' : 'bg-subtle text-muted hover:text-ink')}
            >
              #{tag}
            </button>
          ) : (
            <span className="rounded-md bg-subtle px-2 py-0.5 text-xs text-muted">#{tag}</span>
          )}
        </li>
      ))}
    </ul>
  );
}

export default function NotesPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const { subjects } = useSubjects();
  const [search, setSearch] = useState('');
  const [subjectId, setSubjectId] = useState('');
  const [tag, setTag] = useState('');
  const [pinnedOnly, setPinnedOnly] = useState(false);
  const [page, setPage] = useState(0);
  const debouncedSearch = useDebounce(search.trim(), 300);

  const [editor, setEditor] = useState({ open: false, note: null });
  const [viewing, setViewing] = useState(null);
  const [deleting, setDeleting] = useState(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    if (searchParams.get('new') === '1') {
      setEditor({ open: true, note: null });
      searchParams.delete('new');
      setSearchParams(searchParams, { replace: true });
    }
  }, [searchParams, setSearchParams]);

  useEffect(() => setPage(0), [debouncedSearch, subjectId, tag, pinnedOnly]);

  const params = useMemo(
    () => ({ search: debouncedSearch, subjectId, tag, pinned: pinnedOnly || undefined, page, size: PAGE_SIZE }),
    [debouncedSearch, subjectId, tag, pinnedOnly, page],
  );
  const notes = useApi(() => notesApi.list(params), [params]);
  const tags = useApi(() => notesApi.tags(), []);
  const list = notes.data?.content ?? [];
  const filtered = Boolean(debouncedSearch || subjectId || tag || pinnedOnly);

  const refresh = () => {
    notes.reload();
    tags.reload();
  };

  async function togglePin(note) {
    try {
      const updated = await notesApi.setPinned(note.id, !note.pinned);
      toast.success(updated.pinned ? 'Pinned' : 'Unpinned');
      if (viewing?.id === note.id) setViewing(updated);
      notes.reload();
    } catch (err) {
      toast.error(getErrorMessage(err));
    }
  }

  async function confirmDelete() {
    setBusy(true);
    try {
      await notesApi.remove(deleting.id);
      toast.success('Note deleted');
      setDeleting(null);
      setViewing(null);
      if (list.length === 1 && page > 0) setPage(page - 1);
      refresh();
    } catch (err) {
      toast.error(getErrorMessage(err));
    } finally {
      setBusy(false);
    }
  }

  return (
    <div>
      <PageHeader
        title="Notes"
        description="Searchable notes for every subject. Pin the ones you keep coming back to."
        actions={
          <Button icon={Plus} onClick={() => setEditor({ open: true, note: null })}>
            New note
          </Button>
        }
      />

      <div className="mb-3 grid gap-3 sm:grid-cols-[1.6fr_1fr]">
        <SearchInput value={search} onChange={setSearch} placeholder="Search titles and note text" label="Search notes" />
        <SelectInput aria-label="Subject" value={subjectId} onChange={(e) => setSubjectId(e.target.value)} options={subjectOptions(subjects, { noneLabel: 'All subjects' })} />
      </div>
      <div className="mb-5 flex flex-wrap items-center gap-x-5 gap-y-3">
        <label className="inline-flex cursor-pointer items-center gap-2 text-sm">
          <input type="checkbox" checked={pinnedOnly} onChange={(e) => setPinnedOnly(e.target.checked)} className="h-4 w-4 accent-[var(--app-brand)]" />
          Pinned only
        </label>
        <TagList tags={tags.data} activeTag={tag} onSelect={(t) => setTag(t === tag ? '' : t)} />
      </div>

      {notes.error && <ErrorState className="mb-4" message={getErrorMessage(notes.error)} onRetry={notes.reload} />}

      {notes.loading && !notes.data ? (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {Array.from({ length: 6 }, (_, i) => (
            <Skeleton key={i} className="h-44 rounded-2xl" />
          ))}
        </div>
      ) : list.length === 0 ? (
        <Panel>
          {filtered ? (
            <EmptyState icon={NotebookPen} title="No notes match" description="Try another search, subject or tag." />
          ) : (
            <EmptyState
              icon={NotebookPen}
              title="No notes yet"
              description="Write down formulas, definitions and lecture summaries so they're one search away."
              action={<Button icon={Plus} onClick={() => setEditor({ open: true, note: null })}>New note</Button>}
            />
          )}
        </Panel>
      ) : (
        <>
          <ul className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {list.map((note) => (
              <li key={note.id} className={cx('relative flex flex-col rounded-2xl border bg-surface p-5', note.pinned ? 'border-brand/40' : 'border-line')}>
                <button
                  type="button"
                  onClick={() => togglePin(note)}
                  className={cx('absolute right-3 top-3 rounded-lg p-1.5 hover:bg-subtle', note.pinned ? 'text-brand' : 'text-muted')}
                  aria-label={note.pinned ? `Unpin "${note.title}"` : `Pin "${note.title}"`}
                  aria-pressed={note.pinned}
                >
                  {note.pinned ? <Pin size={16} fill="currentColor" /> : <Pin size={16} />}
                </button>
                <button type="button" onClick={() => setViewing(note)} className="flex flex-1 flex-col text-left">
                  <h3 className="pr-8 font-sans text-base font-semibold hover:text-brand">{note.title}</h3>
                  <SubjectTag subject={note.subject} className="mt-1 text-[13px] text-muted" fallback="" />
                  <p className="mt-3 line-clamp-5 flex-1 whitespace-pre-line text-sm text-muted">{note.content}</p>
                </button>
                <div className="mt-4 space-y-2">
                  <TagList tags={note.tags} />
                  <p className="text-xs text-muted">Edited {formatDate(note.updatedAt)}</p>
                </div>
              </li>
            ))}
          </ul>
          {notes.data && (
            <div className="mt-4 rounded-2xl border border-line bg-surface">
              <Pagination page={notes.data.page} size={notes.data.size} totalElements={notes.data.totalElements} totalPages={notes.data.totalPages} onPageChange={setPage} />
            </div>
          )}
        </>
      )}

      <Modal open={Boolean(viewing)} onClose={() => setViewing(null)} title={viewing?.title ?? ''} size="lg">
        {viewing && (
          <>
            <ModalBody>
              <div className="flex flex-wrap items-center gap-x-4 gap-y-2 text-[13px] text-muted">
                <SubjectTag subject={viewing.subject} className="text-[13px]" />
                <span>Created {formatDateTime(viewing.createdAt)}</span>
                <span>Edited {formatDateTime(viewing.updatedAt)}</span>
              </div>
              <TagList tags={viewing.tags} />
              <div className="whitespace-pre-wrap text-[15px] leading-relaxed">{viewing.content}</div>
            </ModalBody>
            <ModalFooter>
              <Button variant="danger-ghost" icon={Trash2} onClick={() => { setDeleting(viewing); setViewing(null); }} className="sm:mr-auto">
                Delete
              </Button>
              <Button variant="secondary" icon={viewing.pinned ? PinOff : Pin} onClick={() => togglePin(viewing)}>
                {viewing.pinned ? 'Unpin' : 'Pin'}
              </Button>
              <Button icon={Pencil} onClick={() => { setEditor({ open: true, note: viewing }); setViewing(null); }}>
                Edit
              </Button>
            </ModalFooter>
          </>
        )}
      </Modal>

      <NoteEditorModal
        open={editor.open}
        note={editor.note}
        subjects={subjects}
        onClose={() => setEditor({ open: false, note: null })}
        onSaved={() => {
          setEditor({ open: false, note: null });
          refresh();
        }}
      />
      <ConfirmDialog
        open={Boolean(deleting)}
        title="Delete this note?"
        message={deleting ? `"${deleting.title}" will be deleted permanently.` : ''}
        confirmLabel="Delete note"
        loading={busy}
        onConfirm={confirmDelete}
        onCancel={() => setDeleting(null)}
      />
    </div>
  );
}
