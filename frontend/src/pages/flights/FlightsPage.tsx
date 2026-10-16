import { useEffect, useState } from 'react'
import { useSearchParams } from 'react-router'
import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { ArrowDownUp, Clock, Info, Plane, PlaneLanding, PlaneTakeoff, Search, Ticket, Wallet } from 'lucide-react'
import { api } from '../../lib/api'
import type { Airport, Booking, BookingStatus, Flight, Page, SearchResult } from '../../lib/types'
import { dateTime, duration, humanize, isoDate, money, weekdayDate } from '../../lib/format'
import { t } from '../../lib/i18n'
import { PageHeader } from '../../components/ui/PageHeader'
import { Tabs } from '../../components/ui/Tabs'
import { Card } from '../../components/ui/Card'
import { Button } from '../../components/ui/Button'
import { Badge, StatusBadge } from '../../components/ui/Badge'
import { Field } from '../../components/ui/Field'
import { SearchInput } from '../../components/ui/SearchInput'
import { Pagination } from '../../components/ui/Pagination'
import { EmptyState, ErrorState, Skeleton, SkeletonRows } from '../../components/ui/States'
import { useTab } from '../../hooks/useTab'
import { useDebounced } from '../../hooks/useDebounced'
import { useAuth } from '../../auth/AuthContext'
import { cx } from '../../components/ui/cx'
import { BookingDialog } from './BookingDialog'
import { BookingDetail } from './BookingDetail'
import { arrTime, dayShift, depTime } from './flightFormat'

const SORTS = [
  { key: 'price', label: 'Ең арзан', icon: <Wallet className="size-3.5" /> },
  { key: 'duration', label: 'Ең жылдам', icon: <Clock className="size-3.5" /> },
  { key: 'departure', label: 'Ең ерте', icon: <PlaneTakeoff className="size-3.5" /> },
]

export function FlightsPage() {
  const [tab, setTab] = useTab('search')
  const [params] = useSearchParams()
  const [detail, setDetail] = useState<Booking | null>(null)
  const bookings = useQuery({ queryKey: ['bookings', 'mine-count'], queryFn: () => api.get<Page<Booking>>('/bookings', { size: 1 }) })
  const airports = useQuery({ queryKey: ['flights', 'airports'], queryFn: () => api.get<Airport[]>('/flights/airports') })
  return (
    <div className="space-y-6">
      <PageHeader accent="sky" icon={<Plane className="size-6" />} title={t('nav.flights')}
        description="Ойдан шығарылған демо рейстерді іздеу, орындарды ұстау және брондауды растау. Сұрыптау — Strategy, брондаудың өмірлік циклі — State, ал BookingMediator орындарды, жазбаларды, құжаттар мен оқиғаларды үйлестіреді."
        stats={[
          { label: 'Әуежайлар', value: airports.data?.length ?? '-' },
          { label: 'Менің брондауларым', value: bookings.data?.totalElements ?? '-' },
          { label: 'Сұрыптау стратегиялары', value: 3 },
          { label: 'Орынды ұстау', value: '15 мин' },
        ]} />
      <div className="flex items-start gap-3 rounded-2xl border border-sky/20 bg-sky-soft/60 px-4 py-3 text-sm text-[#2A5FC4]">
        <Info className="mt-0.5 size-4 shrink-0" />
        <p>Оқу демонстрациясы: әуе компаниялары, кестелер мен бағалар ойдан шығарылған. Нақты брондау жасалмайды және төлем деректері ешқашан сұралмайды.</p>
      </div>
      <Tabs value={tab} onChange={setTab} items={[
        { key: 'search', label: 'Рейстерді іздеу', icon: <Search className="size-4" /> },
        { key: 'bookings', label: 'Брондаулар', icon: <Ticket className="size-4" />, count: bookings.data?.totalElements },
      ]} />
      {tab === 'search' ? <SearchTab airports={airports.data ?? []} onBooked={(b) => setDetail(b)} />
        : <BookingsTab initialQuery={params.get('q') ?? ''} onOpen={setDetail} />}
      <BookingDetail booking={detail} onClose={() => setDetail(null)} onChange={setDetail} />
    </div>
  )
}

function SearchTab({ airports, onBooked }: { airports: Airport[]; onBooked: (b: Booking) => void }) {
  const [from, setFrom] = useState('ALA')
  const [to, setTo] = useState('NQZ')
  const [date, setDate] = useState(isoDate(1))
  const [passengers, setPassengers] = useState(1)
  const [sort, setSort] = useState('price')
  const [submitted, setSubmitted] = useState({ from: 'ALA', to: 'NQZ', date: isoDate(1), passengers: 1 })
  const [selected, setSelected] = useState<Flight | null>(null)
  const sameAirport = from === to

  const result = useQuery({
    queryKey: ['flights', 'search', submitted, sort],
    queryFn: () => api.get<SearchResult>('/flights/search', { ...submitted, sort }),
    placeholderData: keepPreviousData,
  })
  const cheapest = result.data?.flights.reduce((m, f) => Math.min(m, f.price), Infinity)
  const fastest = result.data?.flights.reduce((m, f) => Math.min(m, f.durationMinutes), Infinity)

  return (
    <div className="space-y-4">
      <Card className="p-4 sm:p-5">
        <form className="grid gap-3 md:grid-cols-[1fr_auto_1fr_10rem_8rem_auto] md:items-end" onSubmit={(e) => { e.preventDefault(); if (!sameAirport) setSubmitted({ from, to, date, passengers }) }}>
          <Field label="Қайдан" htmlFor="from">
            <select id="from" className="input" value={from} onChange={(e) => setFrom(e.target.value)}>
              {airports.map((a) => <option key={a.code} value={a.code}>{a.city} ({a.code})</option>)}
            </select>
          </Field>
          <button type="button" onClick={() => { setFrom(to); setTo(from) }} className="mx-auto grid size-10 place-items-center rounded-xl border border-line text-muted transition hover:rotate-180 hover:text-primary" aria-label="Әуежайларды ауыстыру">
            <ArrowDownUp className="size-4 md:rotate-90" />
          </button>
          <Field label="Қайда" htmlFor="to" error={sameAirport ? 'Басқа бағытты таңдаңыз' : undefined}>
            <select id="to" className="input" value={to} onChange={(e) => setTo(e.target.value)}>
              {airports.map((a) => <option key={a.code} value={a.code}>{a.city} ({a.code})</option>)}
            </select>
          </Field>
          <Field label="Ұшу күні" htmlFor="dep"><input id="dep" type="date" className="input" min={isoDate(0)} max={isoDate(14)} value={date} onChange={(e) => setDate(e.target.value)} /></Field>
          <Field label="Жолаушылар" htmlFor="pax">
            <select id="pax" className="input" value={passengers} onChange={(e) => setPassengers(Number(e.target.value))}>
              {[1, 2, 3, 4, 5, 6].map((n) => <option key={n} value={n}>{n}</option>)}
            </select>
          </Field>
          <Button type="submit" size="lg" icon={<Search className="size-4" />} disabled={sameAirport}>Іздеу</Button>
        </form>
      </Card>

      <div className="flex flex-wrap items-center justify-between gap-3">
        <p className="text-sm text-muted">{result.data ? `Табылған рейстер: ${result.data.flights.length} · сұрыптау стратегиясы: «${result.data.sortLabel}»` : 'Іздеу...'}</p>
        <div className="inline-flex rounded-xl border border-line bg-white p-1" role="radiogroup" aria-label="Сұрыптау стратегиясы">
          {SORTS.map((s) => (
            <button key={s.key} role="radio" aria-checked={sort === s.key} onClick={() => setSort(s.key)}
              className={cx('inline-flex items-center gap-1.5 rounded-lg px-3 py-1.5 text-xs font-semibold transition', sort === s.key ? 'bg-primary text-white' : 'text-muted hover:text-ink')}>
              {s.icon}{s.label}
            </button>
          ))}
        </div>
      </div>

      {result.isLoading ? <div className="space-y-3">{[0, 1, 2].map((i) => <Skeleton key={i} className="h-28" />)}</div>
        : result.isError ? <Card><ErrorState error={result.error} onRetry={() => result.refetch()} /></Card>
          : result.data!.flights.length === 0 ? (
            <Card><EmptyState icon={<Plane className="size-6" />} title="Бұл іздеу бойынша рейс жоқ" description="Демо рейстер алдағы 14 күнге жоспарланған. ALA → NQZ, ALA → IST немесе ALA → DXB бағыттарын көріңіз." /></Card>
          ) : (
            <div className={cx('stagger space-y-3 transition-opacity', result.isFetching && 'opacity-60')}>
              {result.data!.flights.map((f) => {
                const shift = dayShift(f)
                return (
                  <Card key={f.id} className="group p-4 transition hover:-translate-y-0.5 hover:shadow-pop sm:p-5">
                    <div className="flex flex-col gap-4 md:flex-row md:items-center">
                      <div className="flex items-center gap-3 md:w-48">
                        <span className="grid size-11 shrink-0 place-items-center rounded-2xl bg-gradient-to-br from-[#4385FF] to-[#6C63FF] font-display text-xs font-extrabold text-white">{f.flightNumber.slice(0, 2)}</span>
                        <div className="min-w-0"><p className="truncate font-semibold">{f.airline}</p><p className="text-xs text-muted">{f.flightNumber}</p></div>
                      </div>
                      <div className="flex flex-1 items-center gap-3">
                        <div><p className="font-display text-2xl font-bold tabular-nums">{depTime(f)}</p><p className="text-xs text-muted">{f.origin.code} &middot; {weekdayDate(f.departureTime, f.origin.timezone)}</p></div>
                        <div className="flex-1 px-2 text-center">
                          <p className="text-[11px] font-semibold whitespace-nowrap text-muted">{duration(f.durationMinutes)}</p>
                          <div className="relative my-1 h-px bg-line"><Plane className="absolute -top-2 left-1/2 size-4 -translate-x-1/2 bg-white text-primary" /></div>
                          <p className="text-[11px] text-muted">Тікелей</p>
                        </div>
                        <div className="text-right"><p className="font-display text-2xl font-bold tabular-nums">{arrTime(f)}{shift > 0 && <sup className="ml-0.5 text-xs text-danger">+{shift}</sup>}</p><p className="text-xs text-muted">{f.destination.code}, жергілікті уақыт</p></div>
                      </div>
                      <div className="flex items-center justify-between gap-4 md:w-64 md:justify-end">
                        <div className="text-right">
                          <div className="mb-1 flex justify-end gap-1">
                            {f.price === cheapest && <Badge tone="mint">Ең арзан</Badge>}
                            {f.durationMinutes === fastest && <Badge tone="sky">Ең жылдам</Badge>}
                          </div>
                          <p className="font-display text-xl font-extrabold">{money(f.price)}</p>
                          <p className={cx('text-xs', f.availableSeats < 10 ? 'font-semibold text-danger' : 'text-muted')}>Бос орын: {f.availableSeats} &middot; бір адамға</p>
                        </div>
                        <Button onClick={() => setSelected(f)}>Таңдау</Button>
                      </div>
                    </div>
                  </Card>
                )
              })}
            </div>
          )}
      <BookingDialog flight={selected} passengers={submitted.passengers} onClose={() => setSelected(null)}
        onBooked={(b) => { setSelected(null); onBooked(b) }} />
    </div>
  )
}

function BookingsTab({ initialQuery, onOpen }: { initialQuery: string; onOpen: (b: Booking) => void }) {
  const { can } = useAuth()
  const [q, setQ] = useState(initialQuery)
  const [status, setStatus] = useState<BookingStatus | ''>('')
  const [all, setAll] = useState(false)
  const [page, setPage] = useState(0)
  const query = useDebounced(q)
  useEffect(() => setPage(0), [query, status, all])
  const list = useQuery({
    queryKey: ['bookings', { query, status, all, page }],
    queryFn: () => api.get<Page<Booking>>('/bookings', { q: query, status, all, page, size: 8 }),
    placeholderData: keepPreviousData,
  })
  return (
    <Card>
      <div className="flex flex-col gap-3 p-4 sm:flex-row sm:flex-wrap sm:items-center sm:p-5">
        <SearchInput value={q} onChange={setQ} placeholder="Брондау коды немесе рейс нөмірі" />
        <select className="input sm:w-44" value={status} onChange={(e) => setStatus(e.target.value as BookingStatus | '')} aria-label="Брондау күйі">
          <option value="">Кез келген күй</option>
          {['PENDING', 'CONFIRMED', 'CANCELLED', 'EXPIRED'].map((s) => <option key={s} value={s}>{humanize(s)}</option>)}
        </select>
        {can('ADMIN', 'MANAGER') && (
          <label className="flex items-center gap-2 text-sm text-muted sm:ml-auto"><input type="checkbox" className="accent-primary" checked={all} onChange={(e) => setAll(e.target.checked)} /> Барлық пайдаланушылар</label>
        )}
      </div>
      {list.isLoading ? <SkeletonRows /> : list.isError ? <ErrorState error={list.error} /> : list.data!.content.length === 0 ? (
        <EmptyState icon={<Ticket className="size-6" />} title="Әзірге брондау жоқ" description="Алғашқы демо-брондауды жасау үшін рейс тауып, орын ұстаңыз." />
      ) : (
        <>
          <ul className="divide-y divide-line/70 border-t border-line">
            {list.data!.content.map((b) => (
              <li key={b.id}>
                <button onClick={() => onOpen(b)} className="flex w-full flex-col gap-3 px-5 py-4 text-left transition hover:bg-primary-soft/30 sm:flex-row sm:items-center">
                  <div className="flex items-center gap-3 sm:w-44">
                    <span className="grid size-10 place-items-center rounded-xl bg-primary-soft text-primary"><Ticket className="size-4" /></span>
                    <div><p className="font-display font-bold tracking-wide">{b.reference}</p><p className="text-xs text-muted">{dateTime(b.createdAt)}</p></div>
                  </div>
                  <div className="flex flex-1 items-center gap-2 text-sm">
                    <PlaneTakeoff className="size-4 text-muted" /><b>{b.flight.origin.code}</b> {depTime(b.flight)}
                    <span className="text-muted">&rarr;</span>
                    <PlaneLanding className="size-4 text-muted" /><b>{b.flight.destination.code}</b> {arrTime(b.flight)}
                    <span className="hidden text-muted md:inline">&middot; {b.flight.flightNumber} &middot; {weekdayDate(b.flight.departureTime, b.flight.origin.timezone)}</span>
                  </div>
                  <div className="flex items-center justify-between gap-4 sm:justify-end">
                    <span className="text-sm text-muted">{b.passengerCount} жолаушы &middot; <b className="text-ink">{money(b.totalPrice)}</b></span>
                    <StatusBadge status={b.status} />
                  </div>
                </button>
              </li>
            ))}
          </ul>
          <Pagination page={list.data!.page} totalPages={list.data!.totalPages} totalElements={list.data!.totalElements} onPage={setPage} />
        </>
      )}
    </Card>
  )
}
