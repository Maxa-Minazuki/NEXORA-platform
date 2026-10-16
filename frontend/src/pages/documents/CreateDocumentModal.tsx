import { useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { FilePlus, Link2 } from 'lucide-react'
import { api, errorMessage } from '../../lib/api'
import type { Appointment, ChainResult, DocumentDetail, DocumentTypeInfo, Page, Product, WorkflowOutcome } from '../../lib/types'
import { dateTime, humanize } from '../../lib/format'
import { Modal } from '../../components/ui/Modal'
import { Field } from '../../components/ui/Field'
import { Button } from '../../components/ui/Button'
import { useToast } from '../../components/ui/Toast'
import { useAuth } from '../../auth/AuthContext'
import { useDebounced } from '../../hooks/useDebounced'
import { ChainPreview } from './ChainPreview'

const schema = z.object({
  type: z.enum(['PURCHASE_REQUEST', 'INVENTORY_REPLENISHMENT', 'MEDICAL_APPOINTMENT_REPORT', 'GENERAL_ADMINISTRATIVE']),
  title: z.string().trim().min(3, 'Кемінде 3 таңба').max(160),
  content: z.string().trim().min(10, 'Кемінде 10 таңба').max(4000),
  amount: z.string().trim().regex(/^$|^\d{1,9}(\.\d{1,2})?$/, 'Сома: ең көбі 2 ондық белгі').optional(),
  relatedEntityId: z.string().optional(),
})
type Values = z.infer<typeof schema>

export function CreateDocumentModal({ open, onClose }: { open: boolean; onClose: () => void }) {
  const { user, can } = useAuth()
  const qc = useQueryClient()
  const toast = useToast()
  const navigate = useNavigate()
  const types = useQuery({ queryKey: ['documents', 'types'], queryFn: () => api.get<DocumentTypeInfo[]>('/documents/types'), enabled: open })
  const allowed = useMemo(() => (types.data ?? []).filter((t) => !t.systemIssued && user && t.authorRoles.includes(user.role)), [types.data, user])
  const { register, handleSubmit, watch, reset, formState: { errors } } = useForm<Values>({
    resolver: zodResolver(schema),
    defaultValues: { type: 'GENERAL_ADMINISTRATIVE', title: '', content: '', amount: '', relatedEntityId: '' },
  })
  const [submitAfter, setSubmitAfter] = useState(true)
  useEffect(() => {
    if (open && allowed.length) reset({ type: allowed[0].type as Values['type'], title: '', content: '', amount: '', relatedEntityId: '' })
  }, [open, allowed, reset])

  const values = watch()
  const typeInfo = allowed.find((t) => t.type === values.type)
  const products = useQuery({
    queryKey: ['inventory', 'products', 'picker'],
    queryFn: () => api.get<Page<Product>>('/inventory/products', { size: 100, sort: 'name,asc' }),
    enabled: open && typeInfo?.relatedEntityType === 'PRODUCT' && can('ADMIN', 'MANAGER', 'EMPLOYEE'),
  })
  const appointments = useQuery({
    queryKey: ['clinic', 'appointments', 'completed-picker'],
    queryFn: () => api.get<Page<Appointment>>('/clinic/appointments', { status: 'COMPLETED', size: 50, sort: 'startTime,desc' }),
    enabled: open && typeInfo?.relatedEntityType === 'APPOINTMENT',
  })

  const payload = (v: Values) => ({
    type: v.type, title: v.title.trim(), content: v.content.trim(),
    amount: v.amount ? v.amount : null,
    relatedEntityType: v.relatedEntityId ? typeInfo?.relatedEntityType ?? null : null,
    relatedEntityId: v.relatedEntityId ? Number(v.relatedEntityId) : null,
  })
  const debounced = useDebounced(JSON.stringify(payload(values)), 450)
  const preview = useQuery({
    queryKey: ['documents', 'validate', debounced],
    queryFn: () => api.post<ChainResult>('/workflows/validate', JSON.parse(debounced)),
    enabled: open && values.title.trim().length > 0 && values.content.trim().length > 0,
    placeholderData: (prev) => prev,
  })

  const create = useMutation({
    mutationFn: async (v: Values) => {
      const doc = await api.post<DocumentDetail>('/documents', payload(v))
      if (!submitAfter) return { doc, submitted: null as WorkflowOutcome | null }
      const submitted = await api.post<WorkflowOutcome>(`/documents/${doc.summary.id}/submit`)
      return { doc, submitted }
    },
    onSuccess: ({ doc, submitted }) => {
      toast.success(`${doc.summary.number} жасалды`, submitted ? `Күйі: ${humanize(submitted.document.summary.status)}` : 'Қаралама ретінде сақталды')
      qc.invalidateQueries({ queryKey: ['documents'] })
      qc.invalidateQueries({ queryKey: ['dashboard'] })
      onClose()
      navigate(`/documents/${doc.summary.id}`)
    },
    onError: (e) => {
      toast.error('Құжат жіберілмеді', errorMessage(e))
      qc.invalidateQueries({ queryKey: ['documents'] })
    },
  })

  return (
    <Modal open={open} onClose={onClose} size="xl" title="Құжат жасау" subtitle="Оң жақтағы тексерулер әр өзгерісте сервердегі нақты тізбекті іске қосады."
      icon={<span className="grid size-10 place-items-center rounded-xl bg-peach-soft text-[#C9731C]"><FilePlus className="size-5" /></span>}
      footer={<>
        <label className="mr-auto flex items-center gap-2 self-center text-sm text-muted">
          <input type="checkbox" className="accent-primary" checked={submitAfter} onChange={(e) => setSubmitAfter(e.target.checked)} /> Бірден бекітуге жіберу
        </label>
        <Button variant="secondary" onClick={onClose}>Болдырмау</Button>
        <Button type="submit" form="doc-form" loading={create.isPending}>{submitAfter ? 'Жасап, бекітуге жіберу' : 'Қаралама сақтау'}</Button>
      </>}>
      <div className="grid gap-6 lg:grid-cols-[1.4fr_1fr]">
        <form id="doc-form" className="space-y-4" onSubmit={handleSubmit((v) => create.mutate(v))} noValidate>
          <Field label="Құжат түрі" htmlFor="dtype">
            <select id="dtype" className="input" {...register('type')}>
              {allowed.map((t) => <option key={t.type} value={t.type}>{t.label}</option>)}
            </select>
          </Field>
          <Field label="Тақырып" error={errors.title?.message} htmlFor="dtitle"><input id="dtitle" className="input" {...register('title')} /></Field>
          <div className="grid gap-4 sm:grid-cols-2">
            <Field label={`Сома (₸)${typeInfo?.amountRequired ? ' — міндетті' : ' — міндетті емес'}`} error={errors.amount?.message} htmlFor="damount"
              hint="250 000-нан жоғары болса — менеджер қарайды; 1 500 000-нан жоғары болса — әкімші соңғы бекітуді береді">
              <input id="damount" inputMode="decimal" className="input" {...register('amount')} />
            </Field>
            {typeInfo?.relatedEntityType === 'PRODUCT' && (
              <Field label="Тауар" htmlFor="drel"><select id="drel" className="input" {...register('relatedEntityId')}>
                <option value="">Тауарды таңдаңыз...</option>
                {products.data?.content.map((p) => <option key={p.id} value={p.id}>{p.name} ({p.sku})</option>)}
              </select></Field>
            )}
            {typeInfo?.relatedEntityType === 'APPOINTMENT' && (
              <Field label="Аяқталған қабылдау" htmlFor="drel"><select id="drel" className="input" {...register('relatedEntityId')}>
                <option value="">Қабылдауды таңдаңыз...</option>
                {appointments.data?.content.map((a) => <option key={a.id} value={a.id}>{dateTime(a.startTime)} - {a.patientName}</option>)}
              </select></Field>
            )}
          </div>
          <Field label="Мазмұны" error={errors.content?.message} htmlFor="dcontent">
            <textarea id="dcontent" rows={8} className="input" {...register('content')} placeholder="Өтінішті, мөлшерін және негіздемесін сипаттаңыз..." />
          </Field>
        </form>
        <div className="rounded-2xl border border-line bg-canvas/50 p-4">
          <p className="label flex items-center gap-1.5"><Link2 className="size-3.5" /> Chain of Responsibility</p>
          <ChainPreview result={preview.data} loading={preview.isFetching} />
        </div>
      </div>
    </Modal>
  )
}
