import type { ReactNode } from 'react'
import { accentStyles, type Accent } from './accents'
import { cx } from './cx'

export interface HeaderStat { label: string; value: ReactNode }

export function PageHeader({ icon, title, description, accent, actions, stats }: {
  icon: ReactNode; title: string; description: string; accent: Accent; actions?: ReactNode; stats?: HeaderStat[]
}) {
  const a = accentStyles[accent]
  return (
    <section className="card relative overflow-hidden">
      <div className={cx('pointer-events-none absolute -top-24 -right-16 size-72 rounded-full bg-gradient-to-br opacity-[0.13] blur-2xl', a.gradient)} />
      <div className={cx('pointer-events-none absolute -bottom-28 left-1/3 size-60 rounded-full bg-gradient-to-tr opacity-[0.08] blur-2xl', a.gradient)} />
      <div className="relative flex flex-col gap-5 p-5 sm:p-6 lg:flex-row lg:items-center lg:justify-between">
        <div className="flex min-w-0 items-start gap-4">
          <div className={cx('grid size-12 shrink-0 place-items-center rounded-2xl bg-gradient-to-br text-white shadow-lg', a.gradient)}>
            {icon}
          </div>
          <div className="min-w-0">
            <h1 className="text-2xl font-extrabold tracking-tight sm:text-[26px]">{title}</h1>
            <p className="mt-1 max-w-2xl text-sm text-muted">{description}</p>
          </div>
        </div>
        {actions && <div className="flex flex-wrap gap-2">{actions}</div>}
      </div>
      {stats && stats.length > 0 && (
        <div className="relative grid grid-cols-2 gap-px border-t border-line bg-line sm:grid-cols-4">
          {stats.map((s) => (
            <div key={s.label} className="bg-white/90 px-5 py-3.5">
              <p className="text-[11px] font-semibold uppercase tracking-wide text-muted">{s.label}</p>
              <p className="mt-0.5 font-display text-lg font-bold text-ink">{s.value}</p>
            </div>
          ))}
        </div>
      )}
    </section>
  )
}
