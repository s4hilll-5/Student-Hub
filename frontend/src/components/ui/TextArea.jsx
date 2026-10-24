import { useId } from 'react';
import { cx } from './cx';
import { FieldShell, describedBy, fieldClasses } from './FormInput';

export default function TextArea({ id, label, error, hint, className, rows = 4, required, ...props }) {
  const generated = useId();
  const areaId = id || generated;
  return (
    <FieldShell id={areaId} label={label} required={required} error={error} hint={hint} className={className}>
      <textarea
        id={areaId}
        rows={rows}
        required={required}
        aria-invalid={error ? true : undefined}
        aria-describedby={describedBy(areaId, error, hint)}
        className={cx(fieldClasses, 'resize-y py-2.5 leading-relaxed', error && 'border-rose-500')}
        {...props}
      />
    </FieldShell>
  );
}
