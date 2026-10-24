import { useId } from 'react';
import { ChevronDown } from 'lucide-react';
import { cx } from './cx';
import { FieldShell, describedBy, fieldClasses } from './FormInput';

/** Labelled native <select>. options: [{ value, label }]. */
export default function SelectInput({ id, label, options, error, hint, className, selectClassName, required, ...props }) {
  const generated = useId();
  const selectId = id || generated;
  return (
    <FieldShell id={selectId} label={label} required={required} error={error} hint={hint} className={className}>
      <div className="relative">
        <select
          id={selectId}
          required={required}
          aria-invalid={error ? true : undefined}
          aria-describedby={describedBy(selectId, error, hint)}
          className={cx(fieldClasses, 'h-10 appearance-none pr-9', error && 'border-rose-500', selectClassName)}
          {...props}
        >
          {options.map((option) => (
            <option key={option.value} value={option.value} disabled={option.disabled}>
              {option.label}
            </option>
          ))}
        </select>
        <ChevronDown size={16} className="pointer-events-none absolute right-3 top-1/2 -translate-y-1/2 text-muted" aria-hidden="true" />
      </div>
    </FieldShell>
  );
}
