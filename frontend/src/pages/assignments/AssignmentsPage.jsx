import { useEffect, useMemo, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import toast from 'react-hot-toast';
import { Check, ClipboardList, Pencil, Plus, Trash2 } from 'lucide-react';
import { assignmentsApi } from '../../api/services';
import Badge, { SubjectTag } from '../../components/ui/Badge';
import Button from '../../components/ui/Button';
import ConfirmDialog from '../../components/ui/ConfirmDialog';
import DataTable from '../../components/ui/DataTable';
import EmptyState from '../../components/ui/EmptyState';
import ErrorState from '../../components/ui/ErrorState';
import { PageHeader, Panel, SearchInput } from '../../components/ui/Layout';
import Pagination from '../../components/ui/Pagination';
import SelectInput from '../../components/ui/SelectInput';
import { cx } from '../../components/ui/cx';
import { useApi } from '../../hooks/useApi';
import { useDebounce } from '../../hooks/useDebounce';
import { subjectOptions, useSubjects } from '../../hooks/useSubjects';
import { ASSIGNMENT_STATUSES, labelOf, PRIORITIES, toneOf } from '../../utils/constants';
import { describeDue, formatDateTime } from '../../utils/date';
import { getErrorMessage } from '../../utils/errors';
import AssignmentDetailModal from './AssignmentDetailModal';
import AssignmentFormModal from './AssignmentFormModal';

const SORTS = [
  { value: 'dueAt,asc', label: 'Due soonest' },
  { value: 'dueAt,desc', label: 'Due latest' },
  { value: 'priority,desc', label: 'Highest priority' },
  { value: 'createdAt,desc', label: 'Recently added' },
  { value: 'title,asc', label: 'Title A–Z' },
];

const PAGE_SIZE = 10;

export default function AssignmentsPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const { subjects } = useSubjects();
  const [search, setSearch] = useState('');
  const [filters, setFilters] = useState({
    subjectId: '',
    priority: '',
    status: '',
    overdue: searchParams.get('overdue') === 'true',
    sort: 'dueAt,asc',
  });
  const [page, setPage] = useState(0);
  const debouncedSearch = useDebounce(search.trim(), 300);

  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState(null);
  const [viewing, setViewing] = useState(null);
  const [deleting, setDeleting] = useState(null);
  const [busyId, setBusyId] = useState(null);
  const [deletingBusy, setDeletingBusy] = useState(false);

  // Dashboard quick actions link here with ?new=1.
  useEffect(() => {
    if (searchParams.get('new') === '1') {
      setEditing(null);
      setFormOpen(true);
      searchParams.delete('new');
      setSearchParams(searchParams, { replace: true });
    }
  }, [searchParams, setSearchParams]);

  useEffect(() => setPage(0), [debouncedSearch, filters]);

  const params = useMemo(
    () => ({
      search: debouncedSearch,
      subjectId: filters.subjectId,
      priority: filters.priority,
      status: filters.status,
      overdue: filters.overdue ? true : undefined,
      sort: filters.sort,
      page,
      size: PAGE_SIZE,
    }),
    [debouncedSearch, filters, page],
  );
  const { data, loading, error, reload } = useApi(() => assignmentsApi.list(params), [params]);
  const rows = data?.content ?? [];
  const hasFilters = Boolean(debouncedSearch || filters.subjectId || filters.priority || filters.status || filters.overdue);

  const setFilter = (field) => (event) => setFilters((f) => ({ ...f, [field]: event.target.value }));

  async function toggleComplete(assignment) {
    const next = assignment.status === 'COMPLETED' ? 'PENDING' : 'COMPLETED';
    setBusyId(assignment.id);
    try {
      const updated = await assignmentsApi.setStatus(assignment.id, next);
      toast.success(next === 'COMPLETED' ? 'Marked as completed' : 'Moved back to to-do');
      if (viewing?.id === assignment.id) setViewing(updated);
      reload();
    } catch (err) {
      toast.error(getErrorMessage(err));
    } finally {
      setBusyId(null);
    }
  }

  async function confirmDelete() {
    setDeletingBusy(true);
    try {
      await assignmentsApi.remove(deleting.id);
      toast.success('Assignment deleted');
      setDeleting(null);
      if (rows.length === 1 && page > 0) setPage(page - 1);
      else reload();
    } catch (err) {
      toast.error(getErrorMessage(err));
    } finally {
      setDeletingBusy(false);
    }
  }

  const columns = [
    {
      key: 'done',
      header: <span className="sr-only">Completed</span>,
      className: 'w-12',
      render: (row) => {
        const done = row.status === 'COMPLETED';
        return (
          <button
            type="button"
            onClick={() => toggleComplete(row)}
            disabled={busyId === row.id}
            aria-pressed={done}
            aria-label={done ? `Mark "${row.title}" as not done` : `Mark "${row.title}" as completed`}
            className={cx(
              'grid h-6 w-6 place-items-center rounded-full border-2 transition',
              done ? 'border-brand bg-brand text-white dark:text-[#14132a]' : 'border-line text-transparent hover:border-brand hover:text-brand',
            )}
          >
            <Check size={14} strokeWidth={3} />
          </button>
        );
      },
    },
    {
      key: 'title',
      header: 'Assignment',
      render: (row) => (
        <button type="button" onClick={() => setViewing(row)} className="max-w-[320px] text-left">
          <span className={cx('block truncate font-medium hover:text-brand', row.status === 'COMPLETED' && 'text-muted line-through')}>
            {row.title}
          </span>
          {row.description && <span className="block truncate text-[13px] text-muted">{row.description}</span>}
        </button>
      ),
    },
    { key: 'subject', header: 'Subject', render: (row) => <SubjectTag subject={row.subject} className="max-w-[180px]" /> },
    {
      key: 'dueAt',
      header: 'Due',
      render: (row) => (
        <div className="whitespace-nowrap">
          <p>{formatDateTime(row.dueAt)}</p>
          <p className={cx('text-[13px]', row.overdue ? 'font-medium text-rose-600 dark:text-rose-400' : 'text-muted')}>
            {row.status === 'COMPLETED' ? 'Completed' : describeDue(row.dueAt)}
          </p>
        </div>
      ),
    },
    {
      key: 'priority',
      header: 'Priority',
      render: (row) => <Badge tone={toneOf(PRIORITIES, row.priority)}>{labelOf(PRIORITIES, row.priority)}</Badge>,
    },
    {
      key: 'status',
      header: 'Status',
      render: (row) =>
        row.overdue ? (
          <Badge tone="danger">Overdue</Badge>
        ) : (
          <Badge tone={toneOf(ASSIGNMENT_STATUSES, row.status)}>{labelOf(ASSIGNMENT_STATUSES, row.status)}</Badge>
        ),
    },
    {
      key: 'actions',
      header: <span className="sr-only">Actions</span>,
      className: 'w-24 text-right',
      render: (row) => (
        <div className="flex justify-end gap-1">
          <Button variant="ghost" size="icon-sm" icon={Pencil} aria-label={`Edit "${row.title}"`} onClick={() => { setEditing(row); setFormOpen(true); }} />
          <Button variant="danger-ghost" size="icon-sm" icon={Trash2} aria-label={`Delete "${row.title}"`} onClick={() => setDeleting(row)} />
        </div>
      ),
    },
  ];

  return (
    <div>
      <PageHeader
        title="Assignments"
        description="Everything you need to hand in, with overdue work flagged."
        actions={
          <Button icon={Plus} onClick={() => { setEditing(null); setFormOpen(true); }}>
            New assignment
          </Button>
        }
      />

      <div className="mb-4 grid gap-3 sm:grid-cols-2 lg:grid-cols-[1.4fr_1fr_0.8fr_0.9fr_1fr]">
        <SearchInput value={search} onChange={setSearch} placeholder="Search titles and descriptions" label="Search assignments" className="sm:col-span-2 lg:col-span-1" />
        <SelectInput aria-label="Subject" value={filters.subjectId} onChange={setFilter('subjectId')} options={subjectOptions(subjects, { noneLabel: 'All subjects' })} />
        <SelectInput aria-label="Priority" value={filters.priority} onChange={setFilter('priority')} options={[{ value: '', label: 'Any priority' }, ...PRIORITIES]} />
        <SelectInput aria-label="Status" value={filters.status} onChange={setFilter('status')} options={[{ value: '', label: 'Any status' }, ...ASSIGNMENT_STATUSES]} />
        <SelectInput aria-label="Sort by" value={filters.sort} onChange={setFilter('sort')} options={SORTS} />
      </div>
      <label className="mb-4 inline-flex cursor-pointer items-center gap-2 text-sm">
        <input
          type="checkbox"
          checked={filters.overdue}
          onChange={(e) => setFilters((f) => ({ ...f, overdue: e.target.checked }))}
          className="h-4 w-4 rounded border-line accent-[var(--app-brand)]"
        />
        Show overdue only
      </label>

      {error && <ErrorState className="mb-4" message={getErrorMessage(error)} onRetry={reload} />}

      <Panel>
        <DataTable
          caption="Assignments"
          columns={columns}
          rows={rows}
          loading={loading && !data}
          rowClassName={(row) => (row.overdue ? 'bg-rose-50/70 shadow-[inset_3px_0_0_#e11d48] dark:bg-rose-500/[0.06]' : undefined)}
          empty={
            hasFilters ? (
              <EmptyState icon={ClipboardList} title="No assignments match" description="Try a different search or clear the filters." />
            ) : (
              <EmptyState
                icon={ClipboardList}
                title="No assignments yet"
                description="Add your first assignment and StudentHub will keep track of when it's due."
                action={<Button icon={Plus} onClick={() => { setEditing(null); setFormOpen(true); }}>New assignment</Button>}
              />
            )
          }
        />
        {data && (
          <Pagination page={data.page} size={data.size} totalElements={data.totalElements} totalPages={data.totalPages} onPageChange={setPage} />
        )}
      </Panel>

      <AssignmentFormModal
        open={formOpen}
        assignment={editing}
        subjects={subjects}
        onClose={() => setFormOpen(false)}
        onSaved={(saved) => {
          setFormOpen(false);
          if (viewing?.id === saved.id) setViewing(saved);
          reload();
        }}
      />
      <AssignmentDetailModal
        assignment={viewing}
        busy={busyId === viewing?.id}
        onClose={() => setViewing(null)}
        onEdit={(row) => { setViewing(null); setEditing(row); setFormOpen(true); }}
        onToggleComplete={toggleComplete}
      />
      <ConfirmDialog
        open={Boolean(deleting)}
        title="Delete this assignment?"
        message={deleting ? `"${deleting.title}" will be removed permanently.` : ''}
        confirmLabel="Delete assignment"
        loading={deletingBusy}
        onConfirm={confirmDelete}
        onCancel={() => setDeleting(null)}
      />
    </div>
  );
}
