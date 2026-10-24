import { useId } from 'react';
import { cx } from './cx';

export const fieldClasses =
  'w-full rounded-[10px] border border-line bg-surface px-3 text-[15px] text-ink placeholder:text-muted/70 ' +
  'outline-none transition focus:border-brand focus:ring-4 focus:ring-brand/15 disabled:opacity-60';

export function FieldShell({ id, label, required, error, hint, className, children }) {
  return (
    <div className={className}>
      {label && (
        <label htmlFor={id} className="mb-1.5 block text-sm font-medium text-ink">
          {label}
          {required && <span className="text-rose-600" aria-hidden="true"> *</span>}
        </label>
      )}
      {children}
      {error ? (
        <p id={`${id}-error`} className="mt-1.5 text-[13px] text-rose-600 dark:text-rose-400">
          {error}
        </p>
      ) : (
        hint && (
          <p id={`${id}-hint`} className="mt-1.5 text-[13px] text-muted">
            {hint}
          </p>
        )
      )}
    </div>
  );
}

export function describedBy(id, error, hint) {
  if (error) return `${id}-error`;
  if (hint) return `${id}-hint`;
  return undefined;
}

/** Labelled text input with inline error/hint. Accepts every native <input> prop. */
export default function FormInput({ id, label, error, hint, className, inputClassName, required, ...props }) {
  const generated = useId();
  const inputId = id || generated;
  return (
    <FieldShell id={inputId} label={label} required={required} error={error} hint={hint} className={className}>
      <input
        id={inputId}
        required={required}
        aria-invalid={error ? true : undefined}
        aria-describedby={describedBy(inputId, error, hint)}
        className={cx(fieldClasses, 'h-10', error && 'border-rose-500 focus:border-rose-500 focus:ring-rose-500/15', inputClassName)}
        {...props}
      />
    </FieldShell>
  );
}
