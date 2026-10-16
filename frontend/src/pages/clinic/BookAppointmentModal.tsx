import { useEffect, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { CalendarPlus } from 'lucide-react'
import { api, errorMessage } from '../../lib/api'
import type { Appointment, Doctor, Page, Patient, Specialty } from '../../lib/types'
import { dateTime } from '../../lib/format'
import { Modal } from '../../components/ui/Modal'
import { Field } from '../../components/ui/Field'
import { Button } from '../../components/ui/Button'
import { useToast } from '../../components/ui/Toast'
import { useAuth } from '../../auth/AuthContext'
import { SlotPicker } from './SlotPicker'
import { isoDate } from '../../lib/format'
import { useDebounced } from '../../hooks/useDebounced'

export function BookAppointmentModal({ open, onClose }: { open: boolean; onClose: () => void }) {
  const { user, can } = useAuth()
  const staff = can('ADMIN', 'MANAGER', 'EMPLOYEE')
  const qc = useQueryClient()
  const toast = useToast()
  const [specialtyId, setSpecialtyId] = useState('')
  const [doctorId, setDoctorId] = useState<number | null>(null)
  const [patientId, setPatientId] = useState<number | null>(null)
  const [patientQuery, setPatientQuery] = useState('')
  const [date, setDate] = useState(isoDate(1))
  const [slot, setSlot] = useState<string | null>(null)
  const [reason, setReason] = useState('')
  const [touched, setTouched] = useState(false)
  const pq = useDebounced(patientQuery)

  useEffect(() => {
    if (open) {
      setSpecialtyId(''); setDoctorId(null); setSlot(null); setReason(''); setTouched(false); setDate(isoDate(1))
      if (staff) setPatientId(null)
    }
  }, [open, staff])
  useEffect(() => setSlot(null), [doctorId, date])

  const specialties = useQuery({ queryKey: ['clinic', 'specialties'], queryFn: () => api.get<Specialty[]>('/clinic/specialties'), enabled: open })
  const doctors = useQuery({ queryKey: ['clinic', 'doctors'], queryFn: () => api.get<Doctor[]>('/clinic/doctors'), enabled: open })
  const me = useQuery({ queryKey: ['clinic', 'me'], queryFn: () => api.get<Patient>('/clinic/patients/me'), enabled: open && user?.role === 'USER', retry: false })
  const patients = useQuery({
    queryKey: ['clinic', 'patients', 'picker', pq],
    queryFn: () => api.get<Page<Patient>>('/clinic/patients', { q: pq, size: 30 }),
    enabled: open && staff,
  })
  useEffect(() => { if (me.data) setPatientId(me.data.id) }, [me.data])

  const book = useMutation({
    mutationFn: () => api.post<Appointment>('/clinic/appointments', { patientId, doctorId, startTime: slot, reason: reason.trim() }),
    onSuccess: (a) => {
      toast.success('Қабылдауға жазылдыңыз', `${a.patientName} — ${a.doctorName}, ${dateTime(a.startTime)}`)
      qc.invalidateQueries({ queryKey: ['clinic'] })
      qc.invalidateQueries({ queryKey: ['dashboard'] })
      onClose()
    },
    onError: (e) => {
      toast.error('Жазылу орындалмады', errorMessage(e))
      qc.invalidateQueries({ queryKey: ['clinic', 'slots'] })
    },
  })

  const visibleDoctors = (doctors.data ?? []).filter((d) => !specialtyId || String(d.specialtyId) === specialtyId)
  const reasonError = touched && reason.trim().length < 3 ? 'Себебін жазыңыз (кемінде 3 таңба)' : undefined
  const canSubmit = patientId && doctorId && slot && reason.trim().length >= 3

  return (
    <Modal open={open} onClose={onClose} size="lg" title="Қабылдауға жазылу" subtitle="Уақыт серверде қайта тексеріледі: бір дәрігерге бір уақытқа екі адам жазыла алмайды."
      icon={<span className="grid size-10 place-items-center rounded-xl bg-mint-soft text-mint"><CalendarPlus className="size-5" /></span>}
      footer={<>
        <Button variant="secondary" onClick={onClose}>Болдырмау</Button>
        <Button variant="success" loading={book.isPending} onClick={() => { setTouched(true); if (canSubmit) book.mutate() }} disabled={!slot}>Қабылдауға жазылу</Button>
      </>}>
      <div className="grid gap-4 sm:grid-cols-2">
        {staff ? (
          <Field label="Пациент" error={touched && !patientId ? 'Пациентті таңдаңыз' : undefined} htmlFor="patient-search">
            <input id="patient-search" className="input mb-2" placeholder="Пациентті іздеу..." value={patientQuery} onChange={(e) => setPatientQuery(e.target.value)} />
            <select className="input" value={patientId ?? ''} onChange={(e) => setPatientId(e.target.value ? Number(e.target.value) : null)} aria-label="Пациент">
              <option value="">Пациентті таңдаңыз...</option>
              {patients.data?.content.map((p) => <option key={p.id} value={p.id}>{p.fullName} ({p.dateOfBirth})</option>)}
            </select>
          </Field>
        ) : (
          <Field label="Пациент">
            <div className="input bg-canvas">{me.data?.fullName ?? (me.isError ? 'Аккаунтыңызға байланған пациент картасы жоқ' : 'Жүктелуде...')}</div>
          </Field>
        )}
        <Field label="Мамандық" htmlFor="spec">
          <select id="spec" className="input" value={specialtyId} onChange={(e) => { setSpecialtyId(e.target.value); setDoctorId(null) }}>
            <option value="">Барлық мамандықтар</option>
            {specialties.data?.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}
          </select>
        </Field>
        <Field label="Дәрігер" error={touched && !doctorId ? 'Дәрігерді таңдаңыз' : undefined} htmlFor="doc">
          <select id="doc" className="input" value={doctorId ?? ''} onChange={(e) => setDoctorId(e.target.value ? Number(e.target.value) : null)}>
            <option value="">Дәрігерді таңдаңыз...</option>
            {visibleDoctors.map((d) => <option key={d.id} value={d.id}>{d.fullName} - {d.specialty}</option>)}
          </select>
        </Field>
        <Field label="Күні" htmlFor="date">
          <input id="date" type="date" className="input" min={isoDate(0)} value={date} onChange={(e) => setDate(e.target.value)} />
        </Field>
        <div className="sm:col-span-2">
          <Field label="Уақыты" error={touched && !slot ? 'Бос уақытты таңдаңыз' : undefined}>
            <SlotPicker doctorId={doctorId} date={date} value={slot} onChange={setSlot} />
          </Field>
        </div>
        <div className="sm:col-span-2">
          <Field label="Қабылдау себебі" error={reasonError} htmlFor="reason">
            <input id="reason" className="input" maxLength={255} value={reason} onChange={(e) => setReason(e.target.value)} onBlur={() => setTouched(true)} placeholder="мысалы: қайта қаралу" />
          </Field>
        </div>
      </div>
    </Modal>
  )
}
