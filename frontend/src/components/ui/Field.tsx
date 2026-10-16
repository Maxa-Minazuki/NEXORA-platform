import type { ReactNode } from 'react'

export function Field({ label, error, hint, children, htmlFor }: {
  label: string; error?: string; hint?: ReactNode; children: ReactNode; htmlFor?: string
}) {
  return (
    <div>
      <label className="label" htmlFor={htmlFor}>{label}</label>
      {children}
      {error ? <p className="mt-1 text-xs font-medium text-danger" role="alert">{error}</p>
        : hint ? <p className="mt-1 text-xs text-muted">{hint}</p> : null}
    </div>
  )
}
