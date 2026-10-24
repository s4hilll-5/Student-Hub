import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import ConfirmDialog from '../components/ui/ConfirmDialog';
import DataTable from '../components/ui/DataTable';
import EmptyState from '../components/ui/EmptyState';
import FormInput from '../components/ui/FormInput';
import AttendanceMeter from '../pages/attendance/AttendanceMeter';

const authState = vi.hoisted(() => ({ value: { status: 'anonymous', user: null } }));
vi.mock('../context/AuthContext', () => ({ useAuth: () => authState.value }));

// Imported after the mock so it receives the mocked useAuth.
const { default: ProtectedRoute } = await import('../components/routing/ProtectedRoute');

function renderProtected(path = '/dashboard', roles) {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <Routes>
        <Route path="/login" element={<p>Sign-in page</p>} />
        <Route path="/dashboard" element={<p>Dashboard page</p>} />
        <Route element={<ProtectedRoute roles={roles} />}>
          <Route path="/secret" element={<p>Secret page</p>} />
        </Route>
      </Routes>
    </MemoryRouter>,
  );
}

describe('ProtectedRoute', () => {
  beforeEach(() => {
    authState.value = { status: 'anonymous', user: null };
  });

  it('sends anonymous visitors to the sign-in page', () => {
    renderProtected('/secret');
    expect(screen.getByText('Sign-in page')).toBeInTheDocument();
  });

  it('shows a loader while the session is being restored', () => {
    authState.value = { status: 'loading', user: null };
    renderProtected('/secret');
    expect(screen.getByRole('status')).toBeInTheDocument();
    expect(screen.queryByText('Sign-in page')).not.toBeInTheDocument();
  });

  it('renders the page for signed-in users', () => {
    authState.value = { status: 'authenticated', user: { role: 'STUDENT' } };
    renderProtected('/secret');
    expect(screen.getByText('Secret page')).toBeInTheDocument();
  });

  it('keeps students out of admin-only routes', () => {
    authState.value = { status: 'authenticated', user: { role: 'STUDENT' } };
    renderProtected('/secret', ['ADMIN']);
    expect(screen.getByText('Dashboard page')).toBeInTheDocument();
  });
});

describe('FormInput', () => {
  it('links its label and error message to the input', () => {
    render(<FormInput label="Title" error="Enter a title" value="" onChange={() => {}} />);
    const input = screen.getByLabelText('Title');
    expect(input).toHaveAttribute('aria-invalid', 'true');
    expect(input).toHaveAccessibleDescription('Enter a title');
  });
});

describe('ConfirmDialog', () => {
  it('confirms, cancels and closes on Escape', async () => {
    const user = userEvent.setup();
    const onConfirm = vi.fn();
    const onCancel = vi.fn();
    render(<ConfirmDialog open title="Delete note?" message="It will be gone." confirmLabel="Delete note" onConfirm={onConfirm} onCancel={onCancel} />);

    expect(screen.getByRole('dialog', { name: 'Delete note?' })).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Delete note' }));
    expect(onConfirm).toHaveBeenCalledTimes(1);
    await user.click(screen.getByRole('button', { name: 'Cancel' }));
    await user.keyboard('{Escape}');
    expect(onCancel).toHaveBeenCalledTimes(2);
  });
});

describe('DataTable', () => {
  it('renders rows, or the empty state when there are none', () => {
    const columns = [{ key: 'title', header: 'Title' }];
    const { rerender } = render(<DataTable columns={columns} rows={[{ id: 1, title: 'Lab report' }]} empty={<EmptyState title="Nothing here" />} />);
    expect(screen.getByRole('cell', { name: 'Lab report' })).toBeInTheDocument();
    rerender(<DataTable columns={columns} rows={[]} empty={<EmptyState title="Nothing here" />} />);
    expect(screen.getByText('Nothing here')).toBeInTheDocument();
  });
});

describe('AttendanceMeter', () => {
  it('shows the percentage, the target notch and a plain-language verdict', () => {
    render(
      <AttendanceMeter
        stats={{ present: 13, totalCounted: 16, percentage: 81.25, target: 75, meetsTarget: true, targetReachable: true, classesNeededToReachTarget: 0, classesYouCanMiss: 1 }}
      />,
    );
    expect(screen.getByText('81.25%')).toBeInTheDocument();
    expect(screen.getByText('You can miss 1 class and stay on target')).toBeInTheDocument();
    expect(screen.getByRole('progressbar')).toHaveAttribute('aria-valuenow', '81.25');
  });
});
