import { CircleCheck, CircleX, Plus, SkipForward, Target } from 'lucide-react'
import type { ChainResult } from '../../lib/types'
import { cx } from '../../components/ui/cx'
import { label } from '../../lib/i18n'

const icons = {
  PASSED: <CircleCheck className="size-4 text-mint" />,
  STEP_ADDED: <Plus className="size-4 text-primary" />,
  SKIPPED: <SkipForward className="size-4 text-muted" />,
  FAILED: <CircleX className="size-4 text-danger" />,
  DECIDED: <Target className="size-4 text-sky" />,
}

/** Visualises the Chain of Responsibility trace returned by the backend. */
export function ChainPreview({ result, loading }: { result?: ChainResult; loading?: boolean }) {
  if (!result) return <p className="text-sm text-muted">{loading ? 'Тексерулер орындалуда...' : 'Тексеруді іске қосу үшін форманы толтырыңыз.'}</p>
  const reached = new Set(result.trace.map((t) => t.handler))
  return (
    <div className={cx('space-y-3', loading && 'opacity-60')}>
      <ol className="space-y-1.5">
        {result.handlers.map((h) => {
          const entries = result.trace.filter((t) => t.handler === h)
          return (
            <li key={h} className={cx('rounded-xl border px-3 py-2 text-xs', reached.has(h) ? 'border-line bg-white' : 'border-dashed border-line bg-canvas/40 text-muted')}>
              <p className="flex items-center gap-2 font-semibold">{entries[0] ? icons[entries[0].outcome] : <span className="size-4 rounded-full border border-line" />}{h}</p>
              {entries.map((e, i) => <p key={i} className="mt-0.5 pl-6 text-muted">{e.message}</p>)}
              {!reached.has(h) && <p className="mt-0.5 pl-6">жетпеді — тізбек ертерек тоқтады</p>}
            </li>
          )
        })}
      </ol>
      <div className={cx('rounded-xl px-3 py-2 text-xs font-semibold', result.passed ? 'bg-mint-soft text-mint' : 'bg-danger-soft text-danger')}>
        {result.passed ? `Маршрут: ${result.route.map((r) => `${r.name} (${label(r.requiredRole)})`).join(' → ') || 'автобекіту'}` : `Тоқтады: ${result.failure}`}
      </div>
    </div>
  )
}
