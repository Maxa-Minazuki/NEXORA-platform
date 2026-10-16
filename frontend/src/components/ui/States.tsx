import type { ReactNode } from 'react'
import { CircleAlert, Inbox, LoaderCircle, RefreshCw } from 'lucide-react'
import { Button } from './Button'
import { cx } from './cx'
import { errorMessage } from '../../lib/api'

export function EmptyState({ icon, title, description, action, compact }: {
  icon?: ReactNode; title: string; description?: string; action?: ReactNode; compact?: boolean
}) {
  return (
    <div className={cx('flex flex-col items-center justify-center text-center', compact ? 'py-8' : 'py-14')}>
      <div className="mb-3 grid size-12 place-items-center rounded-2xl bg-primary-soft text-primary">
        {icon ?? <Inbox className="size-6" />}
      </div>
      <p className="font-display text-sm font-bold text-ink">{title}</p>
      {description && <p className="mt-1 max-w-sm text-sm text-muted">{description}</p>}
      {action && <div className="mt-4">{action}</div>}
    </div>
  )
}

export function ErrorState({ error, onRetry }: { error: unknown; onRetry?: () => void }) {
  return (
    <div className="flex flex-col items-center justify-center py-12 text-center">
      <div className="mb-3 grid size-12 place-items-center rounded-2xl bg-danger-soft text-danger">
        <CircleAlert className="size-6" />
      </div>
      <p className="font-display text-sm font-bold">Қате орын алды</p>
      <p className="mt-1 max-w-md text-sm text-muted">{errorMessage(error)}</p>
      {onRetry && <Button className="mt-4" variant="secondary" size="sm" icon={<RefreshCw className="size-3.5" />} onClick={onRetry}>Қайталау</Button>}
    </div>
  )
}

export function Spinner({ label = 'Жүктелуде' }: { label?: string }) {
  return (
    <div className="flex items-center justify-center gap-2 py-12 text-sm text-muted" role="status">
      <LoaderCircle className="size-5 animate-spin text-primary" /> {label}...
    </div>
  )
}

export function Skeleton({ className }: { className?: string }) {
  return <div className={cx('animate-pulse rounded-xl bg-line/70', className)} />
}

export function SkeletonRows({ rows = 5 }: { rows?: number }) {
  return (
    <div className="space-y-3 p-5" role="status" aria-label="Жүктелуде">
      {Array.from({ length: rows }).map((_, i) => <Skeleton key={i} className="h-10 w-full" />)}
    </div>
  )
}
