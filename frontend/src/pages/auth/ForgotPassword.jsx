import { useState } from 'react';
import { Link } from 'react-router-dom';
import { Mail } from 'lucide-react';
import { authApi } from '../../api/services';
import Button from '../../components/ui/Button';
import ErrorState from '../../components/ui/ErrorState';
import FormInput from '../../components/ui/FormInput';
import { getErrorMessage, getFieldErrors } from '../../utils/errors';
import AuthLayout from './AuthLayout';

export default function ForgotPassword() {
  const [email, setEmail] = useState('');
  const [error, setError] = useState('');
  const [fieldError, setFieldError] = useState('');
  const [sentMessage, setSentMessage] = useState('');
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(event) {
    event.preventDefault();
    setError('');
    if (!/^\S+@\S+\.\S+$/.test(email.trim())) {
      setFieldError('Enter a valid email address');
      return;
    }
    setFieldError('');
    setSubmitting(true);
    try {
      const response = await authApi.forgotPassword(email.trim());
      setSentMessage(response.message);
    } catch (err) {
      setFieldError(getFieldErrors(err).email || '');
      setError(getErrorMessage(err));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <AuthLayout
      title="Reset your password"
      subtitle="Enter the email you signed up with and we'll send you a reset link."
      footer={
        <Link to="/login" className="font-medium text-brand hover:underline">
          Back to sign in
        </Link>
      }
    >
      {sentMessage ? (
        <div className="flex gap-3" role="status">
          <Mail size={20} className="mt-0.5 shrink-0 text-brand" aria-hidden="true" />
          <div>
            <p className="font-medium">Check your inbox</p>
            <p className="mt-1 text-sm text-muted">{sentMessage} The link expires in 30 minutes.</p>
          </div>
        </div>
      ) : (
        <form onSubmit={handleSubmit} noValidate className="space-y-4">
          {error && <ErrorState message={error} />}
          <FormInput
            label="Email"
            type="email"
            autoComplete="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            error={fieldError}
            required
          />
          <Button type="submit" className="w-full justify-center" loading={submitting}>
            Send reset link
          </Button>
        </form>
      )}
    </AuthLayout>
  );
}
