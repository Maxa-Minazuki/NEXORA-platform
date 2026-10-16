import type { HTMLAttributes, ReactNode } from 'react'
import { cx } from './cx'

export function Card({ className, ...rest }: HTMLAttributes<HTMLDivElement>) {
  return <div className={cx('card', className)} {...rest} />
}

export function CardHeader({ title, subtitle, action, icon }: {
  title: ReactNode; subtitle?: ReactNode; action?: ReactNode; icon?: ReactNode
}) {
  return (
    <div className="flex flex-wrap items-start justify-between gap-3 px-5 pt-5 pb-3">
      <div className="flex min-w-0 items-center gap-3">
        {icon}
        <div className="min-w-0">
          <h3 className="truncate text-[15px] font-bold text-ink">{title}</h3>
          {subtitle && <p className="mt-0.5 text-xs text-muted">{subtitle}</p>}
        </div>
      </div>
      {action}
    </div>
  )
}
