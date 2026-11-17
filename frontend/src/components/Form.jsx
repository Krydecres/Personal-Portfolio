import { useId } from 'react';

export function Field({ label, error, hint, children, id: givenId }) {
  const autoId = useId();
  const id = givenId || autoId;
  const describedBy = [hint && `${id}-hint`, error && `${id}-error`].filter(Boolean).join(' ') || undefined;
  return (
    <div className={`field${error ? ' has-error' : ''}`}>
      <label htmlFor={id}>{label}</label>
      {children({ id, 'aria-describedby': describedBy, 'aria-invalid': error ? true : undefined })}
      {hint && (
        <p className="field-hint" id={`${id}-hint`}>
          {hint}
        </p>
      )}
      {error && (
        <p className="field-error" id={`${id}-error`}>
          {error}
        </p>
      )}
    </div>
  );
}

export function TextInput({ label, error, hint, ...props }) {
  return (
    <Field label={label} error={error} hint={hint}>
      {(a11y) => <input {...a11y} {...props} />}
    </Field>
  );
}

export function TextArea({ label, error, hint, ...props }) {
  return (
    <Field label={label} error={error} hint={hint}>
      {(a11y) => <textarea {...a11y} {...props} />}
    </Field>
  );
}

export function Notice({ kind = 'error', children }) {
  if (!children) return null;
  return (
    <div className={`notice notice-${kind}`} role={kind === 'error' ? 'alert' : 'status'}>
      {children}
    </div>
  );
}
