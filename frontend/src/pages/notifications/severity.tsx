import { CircleAlert, CircleCheck, Info, TriangleAlert } from 'lucide-react'
import type { Severity } from '../../lib/types'
import { cx } from '../../components/ui/cx'
import { label } from '../../lib/i18n'

const map: Record<Severity, { icon: React.ReactNode; tone: string }> = {
  INFO: { icon: <Info className="size-4" />, tone: 'bg-sky-soft text-sky' },
  SUCCESS: { icon: <CircleCheck className="size-4" />, tone: 'bg-mint-soft text-mint' },
  WARNING: { icon: <TriangleAlert className="size-4" />, tone: 'bg-peach-soft text-[#C9731C]' },
  CRITICAL: { icon: <CircleAlert className="size-4" />, tone: 'bg-danger-soft text-danger' },
}

/** Severity is always icon + color (never color alone). */
export function SeverityDot({ severity }: { severity: Severity }) {
  const m = map[severity] ?? map.INFO
  return <span className={cx('grid size-8 shrink-0 place-items-center rounded-xl', m.tone)} aria-label={label(severity)} title={label(severity)}>{m.icon}</span>
}
