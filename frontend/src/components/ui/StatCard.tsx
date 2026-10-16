import type { ReactNode } from 'react'
import { Link } from 'react-router'
import { accentStyles, type Accent } from './accents'
import { cx } from './cx'

export function StatCard({ icon, label, value, hint, accent, to }: {
  icon: ReactNode; label: string; value: ReactNode; hint?: ReactNode; accent: Accent; to?: string
}) {
  const a = accentStyles[accent]
  const body = (
    <div className="card group relative h-full overflow-hidden p-5 transition duration-200 hover:-translate-y-0.5 hover:shadow-pop">
      <div className={cx('pointer-events-none absolute -top-10 -right-10 size-28 rounded-full bg-gradient-to-br opacity-15 transition group-hover:opacity-25', a.gradient)} />
      <div className="relative flex items-start justify-between gap-3">
        <div className="min-w-0">
          <p className="text-xs font-semibold uppercase tracking-wide text-muted">{label}</p>
          <p className="mt-2 truncate font-display text-[26px] font-extrabold leading-none text-ink">{value}</p>
          {hint && <p className="mt-2 truncate text-xs text-muted">{hint}</p>}
        </div>
        <div className={cx('grid size-10 shrink-0 place-items-center rounded-xl', a.soft, a.text)}>{icon}</div>
      </div>
    </div>
  )
  return to ? <Link to={to} className="block h-full">{body}</Link> : body
}
