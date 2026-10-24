import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import toast from 'react-hot-toast';
import Button from '../../components/ui/Button';
import ErrorState from '../../components/ui/ErrorState';
import FormInput from '../../components/ui/FormInput';
import { useAuth } from '../../context/AuthContext';
import { getErrorMessage, getFieldErrors } from '../../utils/errors';
import AuthLayout from './AuthLayout';
import PasswordInput from './PasswordInput';

export function validatePassword(password) {
  if (password.length < 8) return 'Use at least 8 characters';
  if (!/\p{L}/u.test(password) || !/\p{N}/u.test(password)) return 'Include at least one letter and one number';
  if (new TextEncoder().encode(password).length > 72) return 'Password is too long (maximum 72 characters)';
  return null;
}

function browserTimezone() {
  try {
    return Intl.DateTimeFormat().resolvedOptions().timeZone || 'UTC';
  } catch {
    return 'UTC';
  }
}

export default function Register() {
  const { register } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState({ fullName: '', email: '', password: '', confirm: '' });
  const [errors, setErrors] = useState({});
  const [formError, setFormError] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const timezone = browserTimezone();

  const update = (field) => (event) => setForm((f) => ({ ...f, [field]: event.target.value }));

  function validate() {
    const next = {};
    if (!form.fullName.trim()) next.fullName = 'Enter your name';
    if (!/^\S+@\S+\.\S+$/.test(form.email.trim())) next.email = 'Enter a valid email address';
    const passwordProblem = validatePassword(form.password);
    if (passwordProblem) next.password = passwordProblem;
    if (form.confirm !== form.password) next.confirm = "Passwords don't match";
    return next;
  }

  async function handleSubmit(event) {
    event.preventDefault();
    const next = validate();
    setErrors(next);
    setFormError('');
    if (Object.keys(next).length) return;

    setSubmitting(true);
    try {
      const user = await register({
        fullName: form.fullName.trim(),
        email: form.email.trim(),
        password: form.password,
        timezone,
      });
      toast.success(`Welcome, ${user.fullName.split(' ')[0]}`);
      navigate('/dashboard', { replace: true });
    } catch (error) {
      setErrors(getFieldErrors(error));
      setFormError(getErrorMessage(error, "Couldn't create your account. Try again."));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <AuthLayout
      title="Create your account"
      subtitle="Free, and your data stays yours."
      footer={
        <>
          Already have an account?{' '}
          <Link to="/login" className="font-medium text-brand hover:underline">
            Sign in
          </Link>
        </>
      }
    >
      <form onSubmit={handleSubmit} noValidate className="space-y-4">
        {formError && <ErrorState message={formError} />}
        <FormInput label="Full name" autoComplete="name" value={form.fullName} onChange={update('fullName')} error={errors.fullName} required />
        <FormInput label="Email" type="email" autoComplete="email" value={form.email} onChange={update('email')} error={errors.email} required />
        <PasswordInput
          label="Password"
          autoComplete="new-password"
          value={form.password}
          onChange={update('password')}
          error={errors.password}
          hint="At least 8 characters, with a letter and a number."
          required
        />
        <PasswordInput
          label="Confirm password"
          autoComplete="new-password"
          value={form.confirm}
          onChange={update('confirm')}
          error={errors.confirm}
          required
        />
        <p className="text-[13px] text-muted">
          Dates will use your timezone, {timezone}. You can change it later in Settings.
        </p>
        <Button type="submit" className="w-full justify-center" loading={submitting}>
          Create account
        </Button>
      </form>
    </AuthLayout>
  );
}
