import type { ReactNode } from 'react'
import { cx } from './cx'
import { humanize } from '../../lib/format'

export type Tone = 'neutral' | 'violet' | 'sky' | 'teal' | 'mint' | 'peach' | 'rose' | 'danger'

const tones: Record<Tone, string> = {
  neutral: 'bg-canvas text-muted ring-line',
  violet: 'bg-primary-soft text-primary ring-primary/15',
  sky: 'bg-sky-soft text-sky ring-sky/15',
  teal: 'bg-teal-soft text-[#169C90] ring-teal/20',
  mint: 'bg-mint-soft text-mint ring-mint/20',
  peach: 'bg-peach-soft text-[#C9731C] ring-peach/30',
  rose: 'bg-rose-soft text-[#E0537F] ring-rose/25',
  danger: 'bg-danger-soft text-danger ring-danger/20',
}

export function Badge({ tone = 'neutral', children, dot, className }: {
  tone?: Tone; children: ReactNode; dot?: boolean; className?: string
}) {
  return (
    <span className={cx('inline-flex items-center gap-1.5 whitespace-nowrap rounded-full px-2.5 py-0.5 text-[11px] font-semibold ring-1 ring-inset', tones[tone], className)}>
      {dot && <span className="size-1.5 rounded-full bg-current" />}
      {children}
    </span>
  )
}

const statusTone: Record<string, Tone> = {
  // inventory
  IN_STOCK: 'mint', LOW_STOCK: 'peach', OUT_OF_STOCK: 'danger', OPEN: 'peach', REQUEST_CREATED: 'violet', RESOLVED: 'mint',
  RECEIPT: 'mint', WRITE_OFF: 'rose', ADJUSTMENT: 'sky', INITIAL: 'neutral',
  // clinic
  SCHEDULED: 'sky', CONFIRMED: 'mint', COMPLETED: 'violet', CANCELLED: 'neutral', NO_SHOW: 'rose',
  // flights
  PENDING: 'peach', EXPIRED: 'neutral',
  // workflow
  DRAFT: 'neutral', PENDING_APPROVAL: 'peach', IN_REVIEW: 'sky', APPROVED: 'mint', REJECTED: 'danger',
  // notifications
  INFO: 'sky', SUCCESS: 'mint', WARNING: 'peach', CRITICAL: 'danger', DELIVERED: 'mint', FAILED: 'danger', SKIPPED: 'neutral',
  FAILURE: 'danger',
}

export function StatusBadge({ status, label }: { status: string; label?: string }) {
  return <Badge tone={statusTone[status] ?? 'neutral'} dot>{label ?? humanize(status)}</Badge>
}
