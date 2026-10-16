import { useEffect, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Armchair, PlaneTakeoff } from 'lucide-react'
import { z } from 'zod'
import { api, errorMessage } from '../../lib/api'
import type { Booking, Flight, Seat } from '../../lib/types'
import { duration, money, weekdayDate } from '../../lib/format'
import { Modal } from '../../components/ui/Modal'
import { Field } from '../../components/ui/Field'
import { Button } from '../../components/ui/Button'
import { useToast } from '../../components/ui/Toast'
import { useAuth } from '../../auth/AuthContext'
import { SeatMap } from './SeatMap'
import { arrTime, depTime } from './flightFormat'

const emailSchema = z.email()

export function BookingDialog({ flight, passengers, onClose, onBooked }: {
  flight: Flight | null; passengers: number; onClose: () => void; onBooked: (b: Booking) => void
}) {
  const { user } = useAuth()
  const qc = useQueryClient()
  const toast = useToast()
  const [names, setNames] = useState<string[]>([])
  const [seats, setSeats] = useState<string[]>([])
  const [email, setEmail] = useState('')
  const [touched, setTouched] = useState(false)

  useEffect(() => {
    if (flight) {
      setNames(Array.from({ length: passengers }, (_, i) => (i === 0 ? user?.fullName ?? '' : '')))
      setSeats([]); setEmail(user?.email ?? ''); setTouched(false)
    }
  }, [flight, passengers, user])

  const seatMap = useQuery({
    queryKey: ['flights', 'seats', flight?.id],
    queryFn: () => api.get<Seat[]>(`/flights/${flight!.id}/seats`),
    enabled: !!flight,
  })
  const create = useMutation({
    mutationFn: () => api.post<Booking>('/bookings', {
      flightId: flight!.id,
      contactEmail: email.trim(),
      passengers: names.map((n, i) => ({ fullName: n.trim(), seatNumber: seats[i] ?? '' })),
    }),
    onSuccess: (b) => {
      toast.success(`Орындар ұсталды: ${b.reference}`, 'Ұстау мерзімі өткенше брондауды растаңыз.')
      qc.invalidateQueries({ queryKey: ['flights'] })
      qc.invalidateQueries({ queryKey: ['bookings'] })
      onBooked(b)
    },
    onError: (e) => {
      toast.error('Брондау орындалмады', errorMessage(e))
      qc.invalidateQueries({ queryKey: ['flights', 'seats', flight?.id] })
    },
  })

  const nameErrors = names.map((n) => (touched && n.trim().length < 2 ? 'Жолаушының атын енгізіңіз' : undefined))
  const emailError = touched && !emailSchema.safeParse(email.trim()).success ? 'Дұрыс e-mail енгізіңіз' : undefined
  const valid = names.every((n) => n.trim().length >= 2) && !emailSchema.safeParse(email.trim()).error

  return (
    <Modal open={!!flight} onClose={onClose} size="xl" title="Рейсті брондау" subtitle="Оқу демонстрациясы — төлем де, нақты брондау да жоқ"
      icon={<span className="grid size-10 place-items-center rounded-xl bg-primary-soft text-primary"><PlaneTakeoff className="size-5" /></span>}
      footer={<>
        <span className="mr-auto self-center text-sm text-muted">Барлығы <b className="text-ink">{flight ? money(flight.price * passengers) : '-'}</b></span>
        <Button variant="secondary" onClick={onClose}>Болдырмау</Button>
        <Button loading={create.isPending} onClick={() => { setTouched(true); if (valid) create.mutate() }}>Орындарды ұстау</Button>
      </>}>
      {flight && (
        <div className="grid gap-6 lg:grid-cols-[1fr_auto]">
          <div className="space-y-4">
            <div className="rounded-2xl bg-gradient-to-br from-[#4385FF]/10 to-[#6C63FF]/10 p-4">
              <p className="text-xs font-semibold text-muted">{flight.airline} &middot; {flight.flightNumber} &middot; {weekdayDate(flight.departureTime, flight.origin.timezone)}</p>
              <div className="mt-2 flex items-center gap-3">
                <div><p className="font-display text-xl font-bold">{depTime(flight)}</p><p className="text-xs text-muted">{flight.origin.code} {flight.origin.city}</p></div>
                <div className="flex-1 text-center text-[11px] text-muted">{duration(flight.durationMinutes)}<div className="mt-1 h-px bg-line" /></div>
                <div className="text-right"><p className="font-display text-xl font-bold">{arrTime(flight)}</p><p className="text-xs text-muted">{flight.destination.code} {flight.destination.city}</p></div>
              </div>
            </div>
            {names.map((n, i) => (
              <Field key={i} label={`${i + 1}-жолаушы${seats[i] ? ` — орын ${seats[i]}` : ' — орын автоматты беріледі'}`} error={nameErrors[i]} htmlFor={`pax-${i}`}>
                <input id={`pax-${i}`} className="input" value={n} maxLength={120}
                  onChange={(e) => setNames((list) => list.map((x, j) => (j === i ? e.target.value : x)))} placeholder="Төлқұжаттағыдай толық аты-жөні" />
              </Field>
            ))}
            <Field label="Байланыс e-mail" error={emailError} htmlFor="contact">
              <input id="contact" className="input" value={email} onChange={(e) => setEmail(e.target.value)} />
            </Field>
            <p className="rounded-xl bg-peach-soft px-3 py-2 text-xs text-[#9A5A16]">
              Орындар серверде құлыпталады (рейс жолын бұғаттау), сондықтан соңғы орынды екі адам бір уақытта ала алмайды.
            </p>
          </div>
          <div>
            <p className="label flex items-center gap-1.5"><Armchair className="size-3.5" /> Орын таңдау (міндетті емес) — {seats.length}/{passengers}</p>
            {seatMap.data ? (
              <SeatMap seats={seatMap.data} selected={seats} max={passengers}
                onToggle={(s) => setSeats((list) => (list.includes(s) ? list.filter((x) => x !== s) : [...list, s]))} />
            ) : <p className="text-sm text-muted">Орындар картасы жүктелуде...</p>}
          </div>
        </div>
      )}
    </Modal>
  )
}
