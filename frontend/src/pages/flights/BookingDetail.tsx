import { useEffect, useState } from 'react'
import { Link } from 'react-router'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { Ban, CircleCheck, FileText, Network, Ticket, Timer } from 'lucide-react'
import { api, errorMessage } from '../../lib/api'
import type { Booking } from '../../lib/types'
import { dateTime, duration, money, weekdayDate } from '../../lib/format'
import { Modal } from '../../components/ui/Modal'
import { Button } from '../../components/ui/Button'
import { StatusBadge } from '../../components/ui/Badge'
import { Field } from '../../components/ui/Field'
import { useToast } from '../../components/ui/Toast'
import { arrTime, depTime } from './flightFormat'

const ACTIONS: Record<string, string> = { confirm: 'растау', cancel: 'болдырмау' }

function useCountdown(until: string, active: boolean) {
  const [now, setNow] = useState(() => Date.now())
  useEffect(() => {
    if (!active) return
    const id = setInterval(() => setNow(Date.now()), 1000)
    return () => clearInterval(id)
  }, [active])
  const ms = Math.max(0, new Date(until).getTime() - now)
  return `${Math.floor(ms / 60000)}:${String(Math.floor((ms % 60000) / 1000)).padStart(2, '0')}`
}

export function BookingDetail({ booking, onClose, onChange }: { booking: Booking | null; onClose: () => void; onChange: (b: Booking) => void }) {
  const qc = useQueryClient()
  const toast = useToast()
  const [cancelling, setCancelling] = useState(false)
  const [reason, setReason] = useState('')
  const countdown = useCountdown(booking?.expiresAt ?? new Date().toISOString(), booking?.status === 'PENDING')
  useEffect(() => { setCancelling(false); setReason('') }, [booking?.id])

  const after = (title: string) => (b: Booking) => {
    toast.success(title, b.reference)
    qc.invalidateQueries({ queryKey: ['bookings'] })
    qc.invalidateQueries({ queryKey: ['flights'] })
    qc.invalidateQueries({ queryKey: ['dashboard'] })
    setCancelling(false)
    onChange(b)
  }
  const confirm = useMutation({ mutationFn: () => api.post<Booking>(`/bookings/${booking!.id}/confirm`), onSuccess: after('Брондау расталды'), onError: (e) => toast.error('Расталмады', errorMessage(e)) })
  const cancel = useMutation({ mutationFn: () => api.post<Booking>(`/bookings/${booking!.id}/cancel`, { reason: reason.trim() }), onSuccess: after('Брондау болдырылмады'), onError: (e) => toast.error('Болдырылмады', errorMessage(e)) })

  const b = booking
  return (
    <Modal open={!!b} onClose={onClose} size="lg" title={b ? `Брондау ${b.reference}` : ''} subtitle="NEXORA ішіндегі демо-брондау — әуе компаниясындағы нақты брондау емес"
      icon={<span className="grid size-10 place-items-center rounded-xl bg-primary-soft text-primary"><Ticket className="size-5" /></span>}
      footer={b && <>
        {b.allowedActions.includes('cancel') && !cancelling && <Button variant="secondary" icon={<Ban className="size-4" />} onClick={() => setCancelling(true)}>Брондауды болдырмау</Button>}
        {b.allowedActions.includes('confirm') && <Button icon={<CircleCheck className="size-4" />} loading={confirm.isPending} onClick={() => confirm.mutate()}>Растау (демо, төлемсіз)</Button>}
        {!b.allowedActions.length && <Button variant="secondary" onClick={onClose}>Жабу</Button>}
      </>}>
      {b && (
        <div className="space-y-5">
          <div className="flex flex-wrap items-center gap-3">
            <StatusBadge status={b.status} />
            {b.status === 'PENDING' && <span className="inline-flex items-center gap-1.5 rounded-full bg-peach-soft px-2.5 py-0.5 text-xs font-semibold text-[#C9731C]"><Timer className="size-3.5" />Ұстау мерзімі аяқталғанша: {countdown}</span>}
            <span className="text-sm text-muted">State объектісі рұқсат етеді: <b>{b.allowedActions.length ? b.allowedActions.map((a) => ACTIONS[a] ?? a).join(', ') : 'ештеңе (соңғы күй)'}</b></span>
          </div>
          <div className="rounded-2xl border border-line p-4">
            <p className="text-xs font-semibold text-muted">{b.flight.airline} {b.flight.flightNumber} &middot; {weekdayDate(b.flight.departureTime, b.flight.origin.timezone)}</p>
            <div className="mt-2 flex items-center gap-3">
              <div><p className="font-display text-xl font-bold">{depTime(b.flight)}</p><p className="text-xs text-muted">{b.flight.origin.city} ({b.flight.origin.code})</p></div>
              <div className="flex-1 text-center text-[11px] text-muted">{duration(b.flight.durationMinutes)}<div className="mt-1 h-px bg-line" /></div>
              <div className="text-right"><p className="font-display text-xl font-bold">{arrTime(b.flight)}</p><p className="text-xs text-muted">{b.flight.destination.city} ({b.flight.destination.code})</p></div>
            </div>
            <div className="mt-4 grid gap-2 sm:grid-cols-2">
              {b.passengers.map((p) => (
                <div key={p.fullName + p.seatNumber} className="flex items-center justify-between rounded-xl bg-canvas px-3 py-2 text-sm">
                  <span className="truncate font-medium">{p.fullName}</span><span className="font-semibold text-primary">{p.seatNumber}</span>
                </div>
              ))}
            </div>
            <div className="mt-4 flex justify-between text-sm"><span className="text-muted">Барлығы (демо баға)</span><span className="font-bold">{money(b.totalPrice)}</span></div>
          </div>
          {cancelling && (
            <div className="rounded-2xl border border-danger/30 bg-danger-soft/40 p-4">
              <Field label="Болдырмау себебі" htmlFor="creason"><input id="creason" className="input" value={reason} onChange={(e) => setReason(e.target.value)} placeholder="мысалы: жоспар өзгерді" /></Field>
              <div className="mt-3 flex justify-end gap-2">
                <Button size="sm" variant="secondary" onClick={() => setCancelling(false)}>Брондауды сақтау</Button>
                <Button size="sm" variant="danger" disabled={reason.trim().length < 3} loading={cancel.isPending} onClick={() => cancel.mutate()}>Брондауды болдырмау</Button>
              </div>
            </div>
          )}
          {b.cancelReason && <p className="text-sm text-muted">Жабылды {dateTime(b.cancelledAt)}: {b.cancelReason}</p>}
          {b.confirmation && (
            <div className="rounded-2xl border border-mint/30 bg-mint-soft/40 p-4">
              <div className="flex flex-wrap items-center justify-between gap-2">
                <p className="flex items-center gap-2 text-sm font-bold"><FileText className="size-4 text-mint" /> {b.confirmation.documentNumber}</p>
                {b.confirmation.workflowDocumentId && <Link to={`/documents/${b.confirmation.workflowDocumentId}`} className="text-xs font-semibold text-primary hover:underline">Құжат айналымындағы құжатты ашу</Link>}
              </div>
              <pre className="mt-2 font-sans text-xs whitespace-pre-wrap text-muted">{b.confirmation.content}</pre>
            </div>
          )}
          {b.coordinationLog.length > 0 && (
            <details className="rounded-2xl border border-line p-4" open>
              <summary className="flex cursor-pointer items-center gap-2 text-sm font-bold"><Network className="size-4 text-primary" /> Mediator үйлестіру журналы</summary>
              <ol className="mt-3 space-y-1 font-mono text-[11px] text-muted">
                {b.coordinationLog.map((l, i) => <li key={i} className={l.startsWith('Mediator') ? 'text-primary' : ''}>{l}</li>)}
              </ol>
            </details>
          )}
        </div>
      )}
    </Modal>
  )
}
