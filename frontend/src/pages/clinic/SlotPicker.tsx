import { useQuery } from '@tanstack/react-query'
import { CalendarX } from 'lucide-react'
import { api } from '../../lib/api'
import type { DaySlots } from '../../lib/types'
import { time } from '../../lib/format'
import { cx } from '../../components/ui/cx'
import { Skeleton } from '../../components/ui/States'

export function SlotPicker({ doctorId, date, value, onChange }: {
  doctorId: number | null; date: string; value: string | null; onChange: (start: string) => void
}) {
  const slots = useQuery({
    queryKey: ['clinic', 'slots', doctorId, date],
    queryFn: () => api.get<DaySlots>(`/clinic/doctors/${doctorId}/slots`, { date }),
    enabled: !!doctorId && !!date,
  })
  if (!doctorId) return <p className="rounded-xl bg-canvas px-4 py-6 text-center text-sm text-muted">Бос уақытты көру үшін дәрігерді таңдаңыз.</p>
  if (slots.isLoading) return <div className="grid grid-cols-4 gap-2 sm:grid-cols-6">{Array.from({ length: 12 }).map((_, i) => <Skeleton key={i} className="h-9" />)}</div>
  if (!slots.data?.workingDay) {
    return (
      <div className="flex items-center gap-2 rounded-xl bg-canvas px-4 py-5 text-sm text-muted">
        <CalendarX className="size-4" /> Дәрігер бұл күні жұмыс істемейді. Басқа күнді таңдаңыз.
      </div>
    )
  }
  const free = slots.data.slots.filter((s) => s.available).length
  return (
    <div>
      <p className="mb-2 text-xs text-muted">Бос уақыт: {free} &middot; әрқайсысы {slots.data.slotMinutes} мин &middot; уақыт белдеуі {slots.data.timezone}</p>
      <div className="grid grid-cols-4 gap-2 sm:grid-cols-6" role="radiogroup" aria-label="Уақыт аралықтары">
        {slots.data.slots.map((s) => (
          <button key={s.start} type="button" role="radio" aria-checked={value === s.start} disabled={!s.available} onClick={() => onChange(s.start)}
            className={cx('h-9 rounded-xl border text-sm font-semibold tabular-nums transition',
              value === s.start ? 'border-mint bg-mint text-white shadow-[0_6px_16px_-8px_rgba(34,176,125,0.9)]'
                : s.available ? 'border-line bg-white hover:border-mint hover:text-mint' : 'cursor-not-allowed border-transparent bg-canvas text-muted/50 line-through')}>
            {time(s.start)}
          </button>
        ))}
      </div>
    </div>
  )
}
