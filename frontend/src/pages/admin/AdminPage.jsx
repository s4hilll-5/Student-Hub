import { useEffect, useMemo, useState } from 'react';
import toast from 'react-hot-toast';
import { Users } from 'lucide-react';
import { adminApi } from '../../api/services';
import Badge from '../../components/ui/Badge';
import Button from '../../components/ui/Button';
import DashboardCard from '../../components/ui/DashboardCard';
import DataTable from '../../components/ui/DataTable';
import EmptyState from '../../components/ui/EmptyState';
import ErrorState from '../../components/ui/ErrorState';
import { PageHeader, Panel, SearchInput } from '../../components/ui/Layout';
import Pagination from '../../components/ui/Pagination';
import { useAuth } from '../../context/AuthContext';
import { useApi } from '../../hooks/useApi';
import { useDebounce } from '../../hooks/useDebounce';
import { formatDate } from '../../utils/date';
import { getErrorMessage } from '../../utils/errors';

/** Admins see account metadata and platform totals, never students' academic content. */
export default function AdminPage() {
  const { user } = useAuth();
  const [search, setSearch] = useState('');
  const [page, setPage] = useState(0);
  const debounced = useDebounce(search.trim(), 300);
  useEffect(() => setPage(0), [debounced]);

  const stats = useApi(() => adminApi.stats(), []);
  const params = useMemo(() => ({ search: debounced, page, size: 20 }), [debounced, page]);
  const users = useApi(() => adminApi.users(params), [params]);
  const [busyId, setBusyId] = useState(null);

  async function toggle(account) {
    setBusyId(account.id);
    try {
      await adminApi.setEnabled(account.id, !account.enabled);
      toast.success(account.enabled ? `${account.fullName} disabled and signed out` : `${account.fullName} enabled`);
      users.reload();
      stats.reload();
    } catch (err) {
      toast.error(getErrorMessage(err));
    } finally {
      setBusyId(null);
    }
  }

  return (
    <div className="space-y-6">
      <PageHeader title="Admin" description="Accounts and platform totals." />
      {stats.data && (
        <section aria-label="Platform totals" className="grid grid-cols-2 gap-px overflow-hidden rounded-2xl border border-line bg-line lg:grid-cols-4 [&>*]:bg-surface">
          <DashboardCard label="Accounts" value={stats.data.totalUsers} detail={`${stats.data.admins} admin, ${stats.data.disabledUsers} disabled`} />
          <DashboardCard label="Subjects" value={stats.data.subjects} />
          <DashboardCard label="Assignments" value={stats.data.assignments} />
          <DashboardCard label="Notes and exams" value={stats.data.notes + stats.data.examinations} />
        </section>
      )}
      {users.error && <ErrorState message={getErrorMessage(users.error)} onRetry={users.reload} />}
      <SearchInput value={search} onChange={setSearch} placeholder="Search by name or email" label="Search accounts" className="max-w-md" />
      <Panel>
        <DataTable
          caption="Accounts"
          rows={users.data?.content ?? []}
          loading={users.loading && !users.data}
          columns={[
            {
              key: 'name',
              header: 'Name',
              render: (u) => (
                <div>
                  <p className="font-medium">{u.fullName}</p>
                  <p className="text-[13px] text-muted">{u.email}</p>
                </div>
              ),
            },
            { key: 'role', header: 'Role', render: (u) => <Badge tone={u.role === 'ADMIN' ? 'brand' : 'neutral'}>{u.role === 'ADMIN' ? 'Admin' : 'Student'}</Badge> },
            { key: 'college', header: 'College', render: (u) => <span className="text-muted">{u.college || '—'}</span> },
            { key: 'createdAt', header: 'Joined', render: (u) => formatDate(u.createdAt) },
            { key: 'status', header: 'Status', render: (u) => <Badge tone={u.enabled ? 'success' : 'danger'}>{u.enabled ? 'Active' : 'Disabled'}</Badge> },
            {
              key: 'actions',
              header: <span className="sr-only">Actions</span>,
              className: 'text-right',
              render: (u) =>
                u.id === user.id ? (
                  <span className="text-[13px] text-muted">You</span>
                ) : (
                  <Button size="sm" variant={u.enabled ? 'danger-ghost' : 'secondary'} loading={busyId === u.id} onClick={() => toggle(u)}>
                    {u.enabled ? 'Disable' : 'Enable'}
                  </Button>
                ),
            },
          ]}
          empty={<EmptyState icon={Users} title="No accounts found" />}
        />
        {users.data && (
          <Pagination page={users.data.page} size={users.data.size} totalElements={users.data.totalElements} totalPages={users.data.totalPages} onPageChange={setPage} />
        )}
      </Panel>
    </div>
  );
}
