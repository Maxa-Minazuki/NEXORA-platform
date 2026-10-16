import { useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { CalendarClock, CalendarX, Check, CheckCheck, UserRound } from 'lucide-react'
import { api, errorMessage } from '../../lib/api'
import type { Appointment, AppointmentStatus } from '../../lib/types'
import { dateTime, isoDate } from '../../lib/format'
import { Button } from '../../components/ui/Button'
import { Modal } from '../../components/ui/Modal'
import { Field } from '../../components/ui/Field'
import { useToast } from '../../components/ui/Toast'
import { useAuth } from '../../auth/AuthContext'
import { SlotPicker } from './SlotPicker'

export function AppointmentActions({ appointment }: { appointment: Appointment }) {
  const { can } = useAuth()
  const qc = useQueryClient()
  const toast = useToast()
  const [mode, setMode] = useState<'cancel' | 'reschedule' | null>(null)
  const [reason, setReason] = useState('')
  const [date, setDate] = useState(isoDate(1))
  const [slot, setSlot] = useState<string | null>(null)
  const done = (title: string) => (a: Appointment) => {
    toast.success(title, `${a.patientName} - ${dateTime(a.startTime)}`)
    qc.invalidateQueries({ queryKey: ['clinic'] })
    qc.invalidateQueries({ queryKey: ['dashboard'] })
    setMode(null)
  }
  const fail = (e: unknown) => toast.error('Әрекет орындалмады', errorMessage(e))

  const status = useMutation({
    mutationFn: (s: AppointmentStatus) => api.post<Appointment>(`/clinic/appointments/${appointment.id}/status`, { status: s }),
    onSuccess: done('Күйі жаңартылды'), onError: fail,
  })
  const cancel = useMutation({
    mutationFn: () => api.post<Appointment>(`/clinic/appointments/${appointment.id}/cancel`, { reason: reason.trim() }),
    onSuccess: done('Қабылдау болдырылмады'), onError: fail,
  })
  const reschedule = useMutation({
    mutationFn: () => api.post<Appointment>(`/clinic/appointments/${appointment.id}/reschedule`, { startTime: slot }),
    onSuccess: done('Қабылдау ауыстырылды'), onError: fail,
  })

  const allowed = appointment.allowedTransitions
  const clinical = can('ADMIN', 'MANAGER', 'EMPLOYEE', 'DOCTOR')
  const started = new Date(appointment.startTime).getTime() <= Date.now()
  const active = allowed.includes('CANCELLED')

  return (
    <div className="flex flex-nowrap justify-end gap-1">
      {clinical && allowed.includes('CONFIRMED') && (
        <Button size="sm" variant="soft" icon={<Check className="size-3.5" />} loading={status.isPending} onClick={() => status.mutate('CONFIRMED')}>Растау</Button>
      )}
      {clinical && started && allowed.includes('COMPLETED') && (
        <Button size="sm" variant="success" icon={<CheckCheck className="size-3.5" />} onClick={() => status.mutate('COMPLETED')}>Аяқтау</Button>
      )}
      {clinical && started && allowed.includes('NO_SHOW') && (
        <Button size="sm" variant="ghost" icon={<UserRound className="size-3.5" />} onClick={() => status.mutate('NO_SHOW')}>Келмеді</Button>
      )}
      {active && !started && (
        <>
          <Button size="sm" variant="ghost" icon={<CalendarClock className="size-3.5" />} onClick={() => { setSlot(null); setMode('reschedule') }} aria-label="Басқа уақытқа ауыстыру" />
          <Button size="sm" variant="ghost" icon={<CalendarX className="size-3.5" />} onClick={() => { setReason(''); setMode('cancel') }} aria-label="Қабылдауды болдырмау" />
        </>
      )}

      <Modal open={mode === 'cancel'} onClose={() => setMode(null)} size="sm" title="Қабылдауды болдырмау"
        subtitle={`${appointment.patientName} — ${appointment.doctorName}, ${dateTime(appointment.startTime)}`}
        footer={<>
          <Button variant="secondary" onClick={() => setMode(null)}>Сақтап қалу</Button>
          <Button variant="danger" disabled={reason.trim().length < 3} loading={cancel.isPending} onClick={() => cancel.mutate()}>Қабылдауды болдырмау</Button>
        </>}>
        <Field label="Себебі" hint="Пациент пен дәрігерге хабарландыруда көрсетіледі" htmlFor="cr">
          <input id="cr" className="input" value={reason} maxLength={255} onChange={(e) => setReason(e.target.value)} placeholder="мысалы: пациент сапарда" />
        </Field>
      </Modal>

      <Modal open={mode === 'reschedule'} onClose={() => setMode(null)} size="lg" title="Қабылдауды ауыстыру"
        subtitle={`${appointment.doctorName} — қазіргі уақыты: ${dateTime(appointment.startTime)}`}
        footer={<>
          <Button variant="secondary" onClick={() => setMode(null)}>Жабу</Button>
          <Button variant="success" disabled={!slot} loading={reschedule.isPending} onClick={() => reschedule.mutate()}>Ауыстыру</Button>
        </>}>
        <div className="space-y-4">
          <Field label="Жаңа күн" htmlFor="rd"><input id="rd" type="date" className="input sm:w-56" min={isoDate(0)} value={date} onChange={(e) => { setDate(e.target.value); setSlot(null) }} /></Field>
          <SlotPicker doctorId={appointment.doctorId} date={date} value={slot} onChange={setSlot} />
        </div>
      </Modal>
    </div>
  )
}
