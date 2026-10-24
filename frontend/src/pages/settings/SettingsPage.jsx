import { useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import toast from 'react-hot-toast';
import { LogOut, Trash2 } from 'lucide-react';
import { usersApi } from '../../api/services';
import Button from '../../components/ui/Button';
import ConfirmDialog from '../../components/ui/ConfirmDialog';
import ErrorState from '../../components/ui/ErrorState';
import FormInput from '../../components/ui/FormInput';
import { PageHeader, Panel, Segmented } from '../../components/ui/Layout';
import SelectInput from '../../components/ui/SelectInput';
import { useAuth } from '../../context/AuthContext';
import { useTheme } from '../../context/ThemeContext';
import { getErrorMessage, getFieldErrors } from '../../utils/errors';
import PasswordInput from '../auth/PasswordInput';
import { validatePassword } from '../auth/Register';

function deviceTimezone() {
  try {
    return Intl.DateTimeFormat().resolvedOptions().timeZone;
  } catch {
    return 'UTC';
  }
}

function timezoneOptions(current) {
  let zones = [];
  try {
    zones = Intl.supportedValuesOf('timeZone');
  } catch {
    zones = ['UTC', 'Asia/Kolkata', 'Europe/London', 'America/New_York'];
  }
  const set = new Set(['UTC', ...zones, current].filter(Boolean));
  return [...set].map((zone) => ({ value: zone, label: zone.replaceAll('_', ' ') }));
}

function Section({ title, description, children }) {
  return (
    <Panel bodyClassName="grid gap-6 p-5 md:grid-cols-[240px_1fr] md:p-6">
      <div>
        <h2 className="font-sans text-base font-semibold">{title}</h2>
        {description && <p className="mt-1 text-sm text-muted">{description}</p>}
      </div>
      <div>{children}</div>
    </Panel>
  );
}

function ProfileForm() {
  const { user, setUser } = useAuth();
  const [form, setForm] = useState(() => ({
    fullName: user.fullName ?? '',
    college: user.college ?? '',
    course: user.course ?? '',
    semester: user.semester ? String(user.semester) : '',
    timezone: user.timezone ?? 'UTC',
    attendanceTarget: String(Number(user.attendanceTarget ?? 75)),
    weeklyStudyGoalHours: String(user.weeklyStudyGoalHours ?? 10),
  }));
  const [errors, setErrors] = useState({});
  const [saving, setSaving] = useState(false);
  const zones = useMemo(() => timezoneOptions(form.timezone), [form.timezone]);
  const device = deviceTimezone();

  const update = (field) => (event) => setForm((f) => ({ ...f, [field]: event.target.value }));

  async function handleSubmit(event) {
    event.preventDefault();
    const next = {};
    if (!form.fullName.trim()) next.fullName = 'Enter your name';
    const target = Number(form.attendanceTarget);
    if (Number.isNaN(target) || target < 1 || target > 100) next.attendanceTarget = 'Target must be between 1 and 100';
    const goal = Number(form.weeklyStudyGoalHours);
    if (!Number.isInteger(goal) || goal < 1 || goal > 112) next.weeklyStudyGoalHours = 'Goal must be between 1 and 112 hours';
    setErrors(next);
    if (Object.keys(next).length) return;
    setSaving(true);
    try {
      const updated = await usersApi.update({
        fullName: form.fullName.trim(),
        college: form.college.trim() || null,
        course: form.course.trim() || null,
        semester: form.semester ? Number(form.semester) : null,
        timezone: form.timezone,
        attendanceTarget: target,
        weeklyStudyGoalHours: goal,
      });
      setUser(updated);
      toast.success('Profile saved');
    } catch (error) {
      setErrors(getFieldErrors(error));
      toast.error(getErrorMessage(error, "Couldn't save your profile."));
    } finally {
      setSaving(false);
    }
  }

  return (
    <form onSubmit={handleSubmit} noValidate className="space-y-6">
      <Section title="Profile" description="How you appear in StudentHub.">
        <div className="grid gap-4 sm:grid-cols-2">
          <FormInput label="Full name" value={form.fullName} onChange={update('fullName')} error={errors.fullName} maxLength={100} required className="sm:col-span-2" />
          <FormInput label="Email" value={user.email} disabled hint="Your email is your sign-in and can't be changed here." className="sm:col-span-2" />
          <FormInput label="College" value={form.college} onChange={update('college')} error={errors.college} maxLength={150} className="sm:col-span-2" />
          <FormInput label="Course" value={form.course} onChange={update('course')} error={errors.course} maxLength={100} placeholder="B.Sc Computer Science" />
          <SelectInput
            label="Semester"
            value={form.semester}
            onChange={update('semester')}
            error={errors.semester}
            options={[{ value: '', label: 'Not set' }, ...Array.from({ length: 12 }, (_, i) => ({ value: String(i + 1), label: `Semester ${i + 1}` }))]}
          />
        </div>
      </Section>

      <Section title="Goals and time" description="Used for attendance warnings, your study goal and what counts as today.">
        <div className="grid gap-4 sm:grid-cols-2">
          <FormInput
            label="Default attendance target (%)"
            type="number"
            min={1}
            max={100}
            step="0.01"
            value={form.attendanceTarget}
            onChange={update('attendanceTarget')}
            error={errors.attendanceTarget}
            hint="New subjects start with this target."
          />
          <FormInput
            label="Weekly study goal (hours)"
            type="number"
            min={1}
            max={112}
            value={form.weeklyStudyGoalHours}
            onChange={update('weeklyStudyGoalHours')}
            error={errors.weeklyStudyGoalHours}
          />
          <div className="sm:col-span-2">
            <SelectInput label="Timezone" value={form.timezone} onChange={update('timezone')} options={zones} error={errors.timezone} />
            {device && device !== form.timezone && (
              <button type="button" onClick={() => setForm((f) => ({ ...f, timezone: device }))} className="mt-1.5 text-[13px] font-medium text-brand hover:underline">
                Use this device's timezone ({device})
              </button>
            )}
          </div>
        </div>
        <div className="mt-6 flex justify-end">
          <Button type="submit" loading={saving}>
            Save profile
          </Button>
        </div>
      </Section>
    </form>
  );
}

function PasswordForm() {
  const { startSession } = useAuth();
  const empty = { currentPassword: '', newPassword: '', confirm: '' };
  const [form, setForm] = useState(empty);
  const [errors, setErrors] = useState({});
  const [saving, setSaving] = useState(false);

  async function handleSubmit(event) {
    event.preventDefault();
    const next = {};
    if (!form.currentPassword) next.currentPassword = 'Enter your current password';
    const problem = validatePassword(form.newPassword);
    if (problem) next.newPassword = problem;
    if (form.confirm !== form.newPassword) next.confirm = "Passwords don't match";
    setErrors(next);
    if (Object.keys(next).length) return;
    setSaving(true);
    try {
      const session = await usersApi.changePassword({ currentPassword: form.currentPassword, newPassword: form.newPassword });
      startSession(session);
      setForm(empty);
      toast.success('Password changed. Other devices have been signed out.');
    } catch (error) {
      setErrors(getFieldErrors(error));
      toast.error(getErrorMessage(error, "Couldn't change your password."));
    } finally {
      setSaving(false);
    }
  }

  return (
    <Section title="Password" description="Changing it signs you out everywhere else.">
      <form onSubmit={handleSubmit} noValidate className="grid gap-4 sm:grid-cols-2">
        <PasswordInput label="Current password" autoComplete="current-password" value={form.currentPassword}
          onChange={(e) => setForm((f) => ({ ...f, currentPassword: e.target.value }))} error={errors.currentPassword} className="sm:col-span-2" />
        <PasswordInput label="New password" autoComplete="new-password" value={form.newPassword}
          onChange={(e) => setForm((f) => ({ ...f, newPassword: e.target.value }))} error={errors.newPassword} />
        <PasswordInput label="Confirm new password" autoComplete="new-password" value={form.confirm}
          onChange={(e) => setForm((f) => ({ ...f, confirm: e.target.value }))} error={errors.confirm} />
        <div className="flex justify-end sm:col-span-2">
          <Button type="submit" loading={saving}>
            Change password
          </Button>
        </div>
      </form>
    </Section>
  );
}

function AccountSection() {
  const { logout, forgetSession } = useAuth();
  const navigate = useNavigate();
  const [open, setOpen] = useState(false);
  const [password, setPassword] = useState('');
  const [confirmation, setConfirmation] = useState('');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  async function deleteAccount() {
    setBusy(true);
    setError('');
    try {
      await usersApi.deleteAccount({ password, confirmation });
      forgetSession();
      toast.success('Your account and all its data have been deleted.');
      navigate('/', { replace: true });
    } catch (err) {
      setError(getFieldErrors(err).password || getErrorMessage(err));
    } finally {
      setBusy(false);
    }
  }

  return (
    <Section title="Account" description="Sign out of this device, or delete everything.">
      <div className="flex flex-col gap-4">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <p className="text-sm text-muted">Sign out of StudentHub on this device.</p>
          <Button variant="secondary" icon={LogOut} onClick={async () => { await logout(); navigate('/login'); }}>
            Sign out
          </Button>
        </div>
        <div className="flex flex-wrap items-center justify-between gap-3 rounded-xl border border-rose-200 p-4 dark:border-rose-500/30">
          <div>
            <p className="font-medium">Delete account</p>
            <p className="text-sm text-muted">Permanently removes your account and every assignment, subject, exam and note.</p>
          </div>
          <Button variant="danger" icon={Trash2} onClick={() => { setOpen(true); setPassword(''); setConfirmation(''); setError(''); }}>
            Delete account
          </Button>
        </div>
      </div>
      <ConfirmDialog
        open={open}
        title="Delete your account?"
        message="This can't be undone. All of your StudentHub data will be deleted."
        confirmLabel="Delete my account"
        loading={busy}
        confirmDisabled={!password || confirmation !== 'DELETE'}
        onConfirm={deleteAccount}
        onCancel={() => setOpen(false)}
      >
        {error && <ErrorState message={error} />}
        <PasswordInput label="Password" autoComplete="current-password" value={password} onChange={(e) => setPassword(e.target.value)} />
        <FormInput label="Type DELETE to confirm" value={confirmation} onChange={(e) => setConfirmation(e.target.value)} autoComplete="off" />
      </ConfirmDialog>
    </Section>
  );
}

export default function SettingsPage() {
  const { theme, toggleTheme } = useTheme();
  return (
    <div className="space-y-6">
      <PageHeader title="Settings" description="Your profile, goals, password and account." />
      <ProfileForm />
      <PasswordForm />
      <Section title="Appearance" description="Saved on this device.">
        <Segmented
          label="Theme"
          value={theme}
          onChange={(value) => value !== theme && toggleTheme()}
          options={[{ value: 'light', label: 'Light' }, { value: 'dark', label: 'Dark' }]}
        />
      </Section>
      <AccountSection />
    </div>
  );
}
