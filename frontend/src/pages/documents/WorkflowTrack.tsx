import { Check, CircleDot, RotateCcw, SkipForward, X } from 'lucide-react'
import type { TrackItem } from '../../lib/types'
import { dateTime } from '../../lib/format'
import { cx } from '../../components/ui/cx'

const look: Record<TrackItem['state'], { dot: string; icon: React.ReactNode; line: string }> = {
  DONE: { dot: 'bg-mint text-white', icon: <Check className="size-4" />, line: 'bg-mint' },
  CURRENT: { dot: 'bg-primary text-white ring-4 ring-primary/20 animate-pulse', icon: <CircleDot className="size-4" />, line: 'bg-line' },
  PENDING: { dot: 'bg-white text-muted border border-line', icon: <CircleDot className="size-4" />, line: 'bg-line' },
  FAILED: { dot: 'bg-danger text-white', icon: <X className="size-4" />, line: 'bg-danger/40' },
  RETURNED: { dot: 'bg-peach text-white', icon: <RotateCcw className="size-4" />, line: 'bg-line' },
  SKIPPED: { dot: 'bg-canvas text-muted border border-dashed border-line', icon: <SkipForward className="size-4" />, line: 'bg-line' },
}

/** Жасалды -> Тексеру -> бекіту қадамдары -> Аяқталды; ағымдағы кезеңнің нақты қадамдарынан құрылады. */
export function WorkflowTrack({ items }: { items: TrackItem[] }) {
  return (
    <ol className="flex flex-col gap-4 md:flex-row md:gap-0">
      {items.map((item, i) => {
        const l = look[item.state]
        return (
          <li key={item.key} className="relative flex gap-3 md:flex-1 md:flex-col md:items-center md:text-center">
            {i < items.length - 1 && <span className={cx('absolute top-9 left-4 h-[calc(100%-1rem)] w-0.5 md:top-4 md:left-[calc(50%+1.25rem)] md:h-0.5 md:w-[calc(100%-2.5rem)]', l.line)} />}
            <span className={cx('relative z-10 grid size-8 shrink-0 place-items-center rounded-full', l.dot)}>{l.icon}</span>
            <div className="min-w-0 md:mt-2 md:px-2">
              <p className="text-sm font-bold">{item.label}</p>
              {item.actor && <p className="truncate text-xs text-muted">{item.actor}</p>}
              {item.at && <p className="text-[11px] text-muted">{dateTime(item.at)}</p>}
              {item.note && <p className="mt-0.5 line-clamp-2 text-[11px] text-muted italic">{item.note}</p>}
            </div>
          </li>
        )
      })}
    </ol>
  )
}
