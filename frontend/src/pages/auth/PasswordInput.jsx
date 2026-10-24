import { useState } from 'react';
import { Eye, EyeOff } from 'lucide-react';
import FormInput from '../../components/ui/FormInput';
import { cx } from '../../components/ui/cx';

/** Password field with a show/hide toggle. */
export default function PasswordInput({ className, ...props }) {
  const [visible, setVisible] = useState(false);
  return (
    <div className={cx('relative', className)}>
      <FormInput {...props} type={visible ? 'text' : 'password'} inputClassName="pr-11" />
      <button
        type="button"
        onClick={() => setVisible((v) => !v)}
        className="absolute right-1.5 top-[30px] rounded-md p-2 text-muted hover:text-ink"
        aria-label={visible ? 'Hide password' : 'Show password'}
        aria-pressed={visible}
      >
        {visible ? <EyeOff size={17} /> : <Eye size={17} />}
      </button>
    </div>
  );
}
