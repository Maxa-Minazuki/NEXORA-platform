import { useEffect } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { patientSchema } from './patientSchema'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { UserRound } from 'lucide-react'
import { api, errorMessage } from '../../lib/api'
import type { Patient } from '../../lib/types'
import { Modal } from '../../components/ui/Modal'
import { Field } from '../../components/ui/Field'
import { Button } from '../../components/ui/Button'
import { useToast } from '../../components/ui/Toast'

type Values = z.infer<typeof patientSchema>

export function PatientFormModal({ open, onClose, patient }: { open: boolean; onClose: () => void; patient?: Patient }) {
  const qc = useQueryClient()
  const toast = useToast()
  const { register, handleSubmit, reset, formState: { errors } } = useForm<Values>({ resolver: zodResolver(patientSchema) })
  useEffect(() => {
    if (open) {
      reset(patient ? {
        firstName: patient.firstName, lastName: patient.lastName, dateOfBirth: patient.dateOfBirth, gender: patient.gender,
        phone: patient.phone ?? '', email: patient.email ?? '', notes: patient.notes ?? '',
      } : { firstName: '', lastName: '', dateOfBirth: '', gender: 'FEMALE', phone: '', email: '', notes: '' })
    }
  }, [open, patient, reset])
  const save = useMutation({
    mutationFn: (v: Values) => {
      const body = { ...v, email: v.email || null, phone: v.phone || null }
      return patient ? api.put<Patient>(`/clinic/patients/${patient.id}`, body) : api.post<Patient>('/clinic/patients', body)
    },
    onSuccess: (p) => {
      toast.success(patient ? 'Пациент жаңартылды' : 'Пациент қосылды', p.fullName)
      qc.invalidateQueries({ queryKey: ['clinic'] })
      onClose()
    },
    onError: (e) => toast.error('Пациент сақталмады', errorMessage(e)),
  })
  return (
    <Modal open={open} onClose={onClose} title={patient ? 'Пациентті өңдеу' : 'Пациент қосу'} subtitle="Тек ойдан шығарылған деректерді енгізіңіз — бұл демо жүйе."
      icon={<span className="grid size-10 place-items-center rounded-xl bg-mint-soft text-mint"><UserRound className="size-5" /></span>}
      footer={<>
        <Button variant="secondary" onClick={onClose}>Болдырмау</Button>
        <Button type="submit" form="patient-form" variant="success" loading={save.isPending}>Сақтау</Button>
      </>}>
      <form id="patient-form" className="grid gap-4 sm:grid-cols-2" onSubmit={handleSubmit((v) => save.mutate(v))} noValidate>
        <Field label="Аты" error={errors.firstName?.message} htmlFor="fn"><input id="fn" className="input" {...register('firstName')} /></Field>
        <Field label="Тегі" error={errors.lastName?.message} htmlFor="ln"><input id="ln" className="input" {...register('lastName')} /></Field>
        <Field label="Туған күні" error={errors.dateOfBirth?.message} htmlFor="dob"><input id="dob" type="date" className="input" {...register('dateOfBirth')} /></Field>
        <Field label="Жынысы" htmlFor="g">
          <select id="g" className="input" {...register('gender')}><option value="FEMALE">Әйел</option><option value="MALE">Ер</option><option value="OTHER">Басқа</option></select>
        </Field>
        <Field label="Телефон" error={errors.phone?.message} htmlFor="ph"><input id="ph" className="input" {...register('phone')} /></Field>
        <Field label="E-mail" error={errors.email?.message} htmlFor="em"><input id="em" className="input" {...register('email')} /></Field>
        <div className="sm:col-span-2"><Field label="Ескертпелер" error={errors.notes?.message} htmlFor="nt"><textarea id="nt" rows={3} className="input" {...register('notes')} /></Field></div>
      </form>
    </Modal>
  )
}
