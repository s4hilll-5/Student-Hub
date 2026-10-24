import { useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { CircleCheck } from 'lucide-react';
import { authApi } from '../../api/services';
import Button from '../../components/ui/Button';
import ErrorState from '../../components/ui/ErrorState';
import { getErrorMessage, getFieldErrors } from '../../utils/errors';
import AuthLayout from './AuthLayout';
import PasswordInput from './PasswordInput';
import { validatePassword } from './Register';

export default function ResetPassword() {
  const [params] = useSearchParams();
  const token = params.get('token') || '';
  const [form, setForm] = useState({ password: '', confirm: '' });
  const [errors, setErrors] = useState({});
  const [error, setError] = useState('');
  const [done, setDone] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(event) {
    event.preventDefault();
    const next = {};
    const problem = validatePassword(form.password);
    if (problem) next.password = problem;
    if (form.confirm !== form.password) next.confirm = "Passwords don't match";
    setErrors(next);
    setError('');
    if (Object.keys(next).length) return;

    setSubmitting(true);
    try {
      await authApi.resetPassword(token, form.password);
      setDone(true);
    } catch (err) {
      const fields = getFieldErrors(err);
      setErrors({ password: fields.newPassword });
      setError(getErrorMessage(err));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <AuthLayout title="Choose a new password" subtitle="This signs you out on every other device.">
      {!token ? (
        <ErrorState message="This reset link is incomplete. Request a new one from the sign-in page." />
      ) : done ? (
        <div role="status">
          <div className="flex gap-3">
            <CircleCheck size={20} className="mt-0.5 shrink-0 text-emerald-600" aria-hidden="true" />
            <div>
              <p className="font-medium">Password changed</p>
              <p className="mt-1 text-sm text-muted">Sign in with your new password.</p>
            </div>
          </div>
          <Link
            to="/login"
            className="mt-5 inline-flex h-10 w-full items-center justify-center rounded-[10px] bg-brand text-sm font-medium text-white hover:bg-brand-strong dark:text-[#14132a]"
          >
            Go to sign in
          </Link>
        </div>
      ) : (
        <form onSubmit={handleSubmit} noValidate className="space-y-4">
          {error && <ErrorState message={error} />}
          <PasswordInput
            label="New password"
            autoComplete="new-password"
            value={form.password}
            onChange={(e) => setForm((f) => ({ ...f, password: e.target.value }))}
            error={errors.password}
            hint="At least 8 characters, with a letter and a number."
            required
          />
          <PasswordInput
            label="Confirm new password"
            autoComplete="new-password"
            value={form.confirm}
            onChange={(e) => setForm((f) => ({ ...f, confirm: e.target.value }))}
            error={errors.confirm}
            required
          />
          <Button type="submit" className="w-full justify-center" loading={submitting}>
            Change password
          </Button>
          <p className="text-center text-sm">
            <Link to="/forgot-password" className="font-medium text-brand hover:underline">
              Request a new link
            </Link>
          </p>
        </form>
      )}
    </AuthLayout>
  );
}
