import type { Seat } from '../../lib/types'
import { cx } from '../../components/ui/cx'

export function SeatMap({ seats, selected, max, onToggle }: {
  seats: Seat[]; selected: string[]; max: number; onToggle: (seat: string) => void
}) {
  const rows = new Map<number, Seat[]>()
  seats.forEach((s) => {
    const row = parseInt(s.seatNumber, 10)
    rows.set(row, [...(rows.get(row) ?? []), s])
  })
  return (
    <div className="overflow-x-auto">
      <div className="mx-auto w-fit rounded-[2rem] border border-line bg-canvas/60 px-4 py-4">
        <div className="mb-2 grid grid-cols-[1.5rem_repeat(3,2rem)_1.25rem_repeat(3,2rem)] gap-1.5 text-center text-[10px] font-bold text-muted">
          <span />{['A', 'B', 'C'].map((c) => <span key={c}>{c}</span>)}<span />{['D', 'E', 'F'].map((c) => <span key={c}>{c}</span>)}
        </div>
        {[...rows.entries()].map(([row, list]) => (
          <div key={row} className="mb-1.5 grid grid-cols-[1.5rem_repeat(3,2rem)_1.25rem_repeat(3,2rem)] gap-1.5">
            <span className="grid place-items-center text-[10px] font-bold text-muted">{row}</span>
            {list.map((s, i) => {
              const isSel = selected.includes(s.seatNumber)
              const taken = s.status !== 'AVAILABLE'
              const disabled = taken || (!isSel && selected.length >= max)
              return (
                <span key={s.seatNumber} className="contents">
                  {i === 3 && <span />}
                  <button type="button" disabled={disabled} onClick={() => onToggle(s.seatNumber)} aria-pressed={isSel}
                    aria-label={`Орын ${s.seatNumber}${taken ? ' (бос емес)' : ''}`}
                    className={cx('size-8 rounded-lg text-[10px] font-bold transition',
                      isSel ? 'bg-primary text-white shadow-[0_6px_14px_-6px_rgba(108,99,255,0.9)]'
                        : taken ? 'cursor-not-allowed bg-line text-muted/60' : 'border border-line bg-white hover:border-primary hover:text-primary',
                      !isSel && !taken && disabled && 'opacity-50')}>
                    {s.seatNumber}
                  </button>
                </span>
              )
            })}
          </div>
        ))}
      </div>
      <div className="mt-3 flex justify-center gap-4 text-[11px] text-muted">
        <span className="flex items-center gap-1.5"><span className="size-3 rounded border border-line bg-white" />Бос</span>
        <span className="flex items-center gap-1.5"><span className="size-3 rounded bg-primary" />Таңдалған</span>
        <span className="flex items-center gap-1.5"><span className="size-3 rounded bg-line" />Бос емес</span>
      </div>
    </div>
  )
}
