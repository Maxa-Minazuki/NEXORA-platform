import { useEffect, useState } from 'react'
import { useSearchParams } from 'react-router'
import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { CalendarClock, CalendarPlus, ClipboardList, DoorOpen, Pencil, Stethoscope, UserPlus, Users } from 'lucide-react'
import { api } from '../../lib/api'
import type { Appointment, AppointmentStatus, Doctor, Page, Patient, Specialty } from '../../lib/types'
import { dateOnly, dateTime, humanize, time, weekdayDate } from '../../lib/format'
import { PageHeader } from '../../components/ui/PageHeader'
import { Tabs } from '../../components/ui/Tabs'
import { Card } from '../../components/ui/Card'
import { Button } from '../../components/ui/Button'
import { Badge, StatusBadge } from '../../components/ui/Badge'
import { Avatar } from '../../components/ui/Avatar'
import { SearchInput } from '../../components/ui/SearchInput'
import { Pagination } from '../../components/ui/Pagination'
import { Table, Td, Th, Tr } from '../../components/ui/Table'
import { EmptyState, ErrorState, SkeletonRows } from '../../components/ui/States'
import { Modal } from '../../components/ui/Modal'
import { useDebounced } from '../../hooks/useDebounced'
import { useTab } from '../../hooks/useTab'
import { useAuth } from '../../auth/AuthContext'
import { t } from '../../lib/i18n'
import { BookAppointmentModal } from './BookAppointmentModal'
import { AppointmentActions } from './AppointmentActions'
import { PatientFormModal } from './PatientFormModal'
import { dayRange, today } from './clinicTime'

export function ClinicPage() {
  const { can, user } = useAuth()
  const [tab, setTab] = useTab('appointments')
  const [params, setParams] = useSearchParams()
  const [booking, setBooking] = useState(params.get('book') === '1')
  const canBook = can('ADMIN', 'MANAGER', 'EMPLOYEE', 'USER')
  const seesPatients = can('ADMIN', 'MANAGER', 'EMPLOYEE', 'DOCTOR')

  useEffect(() => {
    if (params.get('book') === '1') {
      const next = new URLSearchParams(params); next.delete('book'); setParams(next, { replace: true })
    }
  }, [params, setParams])

  const range = dayRange(today())
  const todayCount = useQuery({ queryKey: ['clinic', 'appointments', 'today'], queryFn: () => api.get<Page<Appointment>>('/clinic/appointments', { ...range, size: 1 }) })
  const upcoming = useQuery({ queryKey: ['clinic', 'appointments', 'upcoming-count'], queryFn: () => api.get<Page<Appointment>>('/clinic/appointments', { from: new Date().toISOString(), size: 1 }) })
  const doctors = useQuery({ queryKey: ['clinic', 'doctors'], queryFn: () => api.get<Doctor[]>('/clinic/doctors') })
  const specialties = useQuery({ queryKey: ['clinic', 'specialties'], queryFn: () => api.get<Specialty[]>('/clinic/specialties') })

  const title = user?.role === 'USER' ? 'Менің қабылдауларым' : user?.role === 'DOCTOR' ? 'Менің кестем' : 'Қабылдаулар'
  return (
    <div className="space-y-6">
      <PageHeader accent="mint" icon={<Stethoscope className="size-6" />} title={t('nav.clinic')}
        description="Пациенттер, дәрігерлер, апталық кестелер және қабылдаулар. PatientAppointmentFacade дәрігер күнтізбесін құлыптап, жұмыс уақытын тексереді және қабаттасатын жазылуларды қабылдамайды."
        actions={canBook && <Button variant="success" icon={<CalendarPlus className="size-4" />} onClick={() => setBooking(true)}>Қабылдауға жазылу</Button>}
        stats={[
          { label: user?.role === 'USER' ? 'Бүгінгі қабылдауларым' : 'Бүгінгі қабылдаулар', value: todayCount.data?.totalElements ?? '-' },
          { label: 'Алдағы', value: upcoming.data?.totalElements ?? '-' },
          { label: 'Дәрігерлер', value: doctors.data?.length ?? '-' },
          { label: 'Мамандықтар', value: specialties.data?.length ?? '-' },
        ]} />
      <Tabs value={tab} onChange={setTab} items={[
        { key: 'appointments', label: title, icon: <CalendarClock className="size-4" /> },
        ...(seesPatients ? [{ key: 'patients', label: 'Пациенттер', icon: <Users className="size-4" /> }] : []),
        { key: 'doctors', label: 'Дәрігерлер мен кестелер', icon: <ClipboardList className="size-4" /> },
      ]} />
      {tab === 'appointments' && <AppointmentsTab doctors={doctors.data ?? []} specialties={specialties.data ?? []} initialQuery={params.get('q') ?? ''} />}
      {tab === 'patients' && seesPatients && <PatientsTab initialQuery={params.get('q') ?? ''} />}
      {tab === 'doctors' && <DoctorsTab doctors={doctors.data} loading={doctors.isLoading} />}
      <BookAppointmentModal open={booking} onClose={() => setBooking(false)} />
    </div>
  )
}

function AppointmentsTab({ doctors, specialties, initialQuery }: { doctors: Doctor[]; specialties: Specialty[]; initialQuery: string }) {
  const { user } = useAuth()
  const [q, setQ] = useState(initialQuery)
  const [doctorId, setDoctorId] = useState('')
  const [specialtyId, setSpecialtyId] = useState('')
  const [status, setStatus] = useState<AppointmentStatus | ''>('')
  const [date, setDate] = useState('')
  const [scope, setScope] = useState<'upcoming' | 'past' | 'all'>('upcoming')
  const [page, setPage] = useState(0)
  const query = useDebounced(q)
  useEffect(() => setPage(0), [query, doctorId, specialtyId, status, date, scope])
  const [now] = useState(() => new Date().toISOString())
  const range = date ? dayRange(date)
    : scope === 'upcoming' ? { from: now, to: undefined }
      : scope === 'past' ? { from: undefined, to: now } : { from: undefined, to: undefined }
  const list = useQuery({
    queryKey: ['clinic', 'appointments', { query, doctorId, specialtyId, status, date, scope, page }],
    queryFn: () => api.get<Page<Appointment>>('/clinic/appointments', {
      q: query, doctorId, specialtyId, status, from: range.from, to: range.to, page, size: 10,
      sort: date || scope === 'upcoming' ? 'startTime,asc' : 'startTime,desc',
    }),
    placeholderData: keepPreviousData,
  })
  const isPatient = user?.role === 'USER'
  const isDoctor = user?.role === 'DOCTOR'

  return (
    <Card>
      <div className="flex flex-col gap-3 p-4 sm:p-5 lg:flex-row lg:flex-wrap lg:items-center">
        {!isPatient && <SearchInput value={q} onChange={setQ} placeholder="Пациенттің аты бойынша іздеу" />}
        <div className="grid grid-cols-2 gap-2 sm:flex sm:flex-wrap">
          {!isDoctor && (
            <select className="input sm:w-48" value={doctorId} onChange={(e) => setDoctorId(e.target.value)} aria-label="Дәрігер">
              <option value="">Барлық дәрігерлер</option>
              {doctors.map((d) => <option key={d.id} value={d.id}>{d.fullName}</option>)}
            </select>
          )}
          <select className="input sm:w-44" value={specialtyId} onChange={(e) => setSpecialtyId(e.target.value)} aria-label="Мамандық">
            <option value="">Барлық мамандықтар</option>
            {specialties.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}
          </select>
          <select className="input sm:w-40" value={status} onChange={(e) => setStatus(e.target.value as AppointmentStatus | '')} aria-label="Күйі">
            <option value="">Кез келген күй</option>
            {['SCHEDULED', 'CONFIRMED', 'COMPLETED', 'CANCELLED', 'NO_SHOW'].map((s) => <option key={s} value={s}>{humanize(s)}</option>)}
          </select>
          <select className="input sm:w-36" value={scope} onChange={(e) => setScope(e.target.value as 'upcoming' | 'past' | 'all')} aria-label="Кезең" disabled={!!date}>
            <option value="upcoming">Алдағы</option><option value="past">Өткен</option><option value="all">Барлық күндер</option>
          </select>
          <input type="date" className="input sm:w-44" value={date} onChange={(e) => setDate(e.target.value)} aria-label="Күні" />
          {date && <Button variant="ghost" size="md" onClick={() => setDate('')}>Күнді тазарту</Button>}
        </div>
      </div>
      {list.isLoading ? <SkeletonRows /> : list.isError ? <ErrorState error={list.error} onRetry={() => list.refetch()} /> :
        list.data!.content.length === 0 ? <EmptyState icon={<CalendarClock className="size-6" />} title="Қабылдау табылмады" description="Басқа сүзгіні таңдаңыз немесе жаңа қабылдауға жазылыңыз." /> : (
          <>
            <Table minWidth={900}>
              <thead><tr><Th>Уақыты</Th>{!isPatient && <Th>Пациент</Th>}<Th>Дәрігер</Th><Th>Себебі</Th><Th>Күйі</Th><Th className="text-right">Әрекеттер</Th></tr></thead>
              <tbody>
                {list.data!.content.map((a) => (
                  <Tr key={a.id}>
                    <Td className="whitespace-nowrap">
                      <p className="font-semibold">{weekdayDate(a.startTime)}</p>
                      <p className="text-xs text-muted tabular-nums">{time(a.startTime)} – {time(a.endTime)}</p>
                    </Td>
                    {!isPatient && <Td><div className="flex items-center gap-2"><Avatar name={a.patientName} size="sm" /><span className="font-semibold">{a.patientName}</span></div></Td>}
                    <Td><p className="font-medium">{a.doctorName}</p><p className="text-xs text-muted">{a.specialty} &middot; {a.room}-кабинет</p></Td>
                    <Td className="max-w-56"><p className="truncate">{a.reason}</p>{a.cancelReason && <p className="truncate text-xs text-muted">Болдырылмады: {a.cancelReason}</p>}</Td>
                    <Td><StatusBadge status={a.status} /></Td>
                    <Td><AppointmentActions appointment={a} /></Td>
                  </Tr>
                ))}
              </tbody>
            </Table>
            <Pagination page={list.data!.page} totalPages={list.data!.totalPages} totalElements={list.data!.totalElements} onPage={setPage} />
          </>
        )}
    </Card>
  )
}

function PatientsTab({ initialQuery }: { initialQuery: string }) {
  const { can } = useAuth()
  const [q, setQ] = useState(initialQuery)
  const [page, setPage] = useState(0)
  const [editing, setEditing] = useState<Patient | null>(null)
  const [creating, setCreating] = useState(false)
  const [history, setHistory] = useState<Patient | null>(null)
  const query = useDebounced(q)
  useEffect(() => setPage(0), [query])
  const list = useQuery({
    queryKey: ['clinic', 'patients', query, page],
    queryFn: () => api.get<Page<Patient>>('/clinic/patients', { q: query, page, size: 10 }),
    placeholderData: keepPreviousData,
  })
  const visits = useQuery({
    queryKey: ['clinic', 'appointments', 'patient', history?.id],
    queryFn: () => api.get<Page<Appointment>>('/clinic/appointments', { patientId: history!.id, size: 50, sort: 'startTime,desc' }),
    enabled: !!history,
  })
  const canEdit = can('ADMIN', 'MANAGER', 'EMPLOYEE')

  return (
    <Card>
      <div className="flex flex-wrap items-center justify-between gap-3 p-4 sm:p-5">
        <SearchInput value={q} onChange={setQ} placeholder="Аты, e-mail немесе телефон бойынша іздеу" />
        {canEdit && <Button variant="success" icon={<UserPlus className="size-4" />} onClick={() => setCreating(true)}>Пациент қосу</Button>}
      </div>
      {list.isLoading ? <SkeletonRows /> : list.isError ? <ErrorState error={list.error} /> : list.data!.content.length === 0 ? <EmptyState title="Пациент табылмады" /> : (
        <>
          <Table minWidth={760}>
            <thead><tr><Th>Пациент</Th><Th>Туған күні</Th><Th>Байланыс</Th><Th>Аккаунт</Th><Th className="text-right">Әрекеттер</Th></tr></thead>
            <tbody>
              {list.data!.content.map((p) => (
                <Tr key={p.id}>
                  <Td><div className="flex items-center gap-3"><Avatar name={p.fullName} size="sm" /><div><p className="font-semibold">{p.fullName}</p><p className="text-xs text-muted">{humanize(p.gender)}</p></div></div></Td>
                  <Td>{dateOnly(p.dateOfBirth + 'T12:00:00Z')}</Td>
                  <Td><p className="text-sm">{p.phone ?? '-'}</p><p className="text-xs text-muted">{p.email ?? ''}</p></Td>
                  <Td>{p.hasAccount ? <Badge tone="violet">Портал пайдаланушысы</Badge> : <Badge>Тіркеу бөлімі</Badge>}</Td>
                  <Td>
                    <div className="flex justify-end gap-1">
                      <Button size="sm" variant="soft" icon={<ClipboardList className="size-3.5" />} onClick={() => setHistory(p)}>Тарих</Button>
                      {canEdit && <Button size="sm" variant="ghost" icon={<Pencil className="size-3.5" />} onClick={() => setEditing(p)} aria-label={`Өңдеу: ${p.fullName}`} title="Өңдеу" />}
                    </div>
                  </Td>
                </Tr>
              ))}
            </tbody>
          </Table>
          <Pagination page={list.data!.page} totalPages={list.data!.totalPages} totalElements={list.data!.totalElements} onPage={setPage} />
        </>
      )}
      <PatientFormModal open={creating || !!editing} patient={editing ?? undefined} onClose={() => { setCreating(false); setEditing(null) }} />
      <Modal open={!!history} onClose={() => setHistory(null)} title={`Қабылдаулар тарихы — ${history?.fullName ?? ''}`} subtitle="Осы (ойдан шығарылған) пациенттің барлық қабылдаулары" size="lg">
        {visits.isLoading ? <SkeletonRows rows={3} /> : visits.data?.content.length === 0 ? <EmptyState compact title="Әзірге қабылдау жоқ" /> : (
          <ol className="relative space-y-4 border-l border-line pl-5">
            {visits.data?.content.map((a) => (
              <li key={a.id} className="relative">
                <span className="absolute top-1.5 -left-[25px] size-2.5 rounded-full bg-mint ring-4 ring-white" />
                <div className="flex flex-wrap items-center gap-2"><span className="font-semibold">{dateTime(a.startTime)}</span><StatusBadge status={a.status} /></div>
                <p className="text-sm text-muted">{a.doctorName} ({a.specialty}) &middot; {a.reason}</p>
                {a.notes && <p className="mt-1 text-xs text-muted">{a.notes}</p>}
              </li>
            ))}
          </ol>
        )}
      </Modal>
    </Card>
  )
}

const DAY_SHORT: Record<string, string> = { MONDAY: 'Дс', TUESDAY: 'Сс', WEDNESDAY: 'Ср', THURSDAY: 'Бс', FRIDAY: 'Жм', SATURDAY: 'Сн', SUNDAY: 'Жс' }

function DoctorsTab({ doctors, loading }: { doctors?: Doctor[]; loading: boolean }) {
  if (loading) return <Card><SkeletonRows /></Card>
  if (!doctors?.length) return <Card><EmptyState title="Әзірге дәрігер жоқ" /></Card>
  return (
    <div className="stagger grid gap-4 md:grid-cols-2 xl:grid-cols-3">
      {doctors.map((d) => (
        <Card key={d.id} className="p-5">
          <div className="flex items-start gap-3">
            <Avatar name={d.fullName} size="lg" />
            <div className="min-w-0">
              <p className="truncate font-display font-bold">{d.fullName}</p>
              <Badge tone="mint" className="mt-1">{d.specialty}</Badge>
            </div>
          </div>
          <p className="mt-3 line-clamp-2 text-sm text-muted">{d.bio}</p>
          <p className="mt-3 flex items-center gap-1.5 text-xs text-muted"><DoorOpen className="size-3.5" /> {d.room}-кабинет &middot; {d.email}</p>
          <div className="mt-4 grid grid-cols-7 gap-1 text-center">
            {['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'].map((day) => {
              const s = d.schedule.find((x) => x.dayOfWeek === day)
              return (
                <div key={day} className={s ? 'rounded-lg bg-mint-soft px-0.5 py-1.5' : 'rounded-lg bg-canvas px-0.5 py-1.5 opacity-60'}
                  title={s ? `${s.startTime.slice(0, 5)}-${s.endTime.slice(0, 5)}` : 'Демалыс'}>
                  <p className="text-[10px] font-bold text-muted">{DAY_SHORT[day]}</p>
                  <p className={s ? 'text-[10px] font-semibold text-mint' : 'text-[10px] text-muted'}>{s ? s.startTime.slice(0, 2) + '-' + s.endTime.slice(0, 2) : '—'}</p>
                </div>
              )
            })}
          </div>
        </Card>
      ))}
    </div>
  )
}
