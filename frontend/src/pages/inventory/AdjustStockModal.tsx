import { useEffect } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { ArrowLeftRight, PackageMinus, PackagePlus, SlidersHorizontal } from 'lucide-react'
import { api, errorMessage } from '../../lib/api'
import { humanize } from '../../lib/format'
import type { AdjustmentResult, Product } from '../../lib/types'
import { Modal } from '../../components/ui/Modal'
import { Field } from '../../components/ui/Field'
import { Button } from '../../components/ui/Button'
import { useToast } from '../../components/ui/Toast'
import { cx } from '../../components/ui/cx'

const schema = z.object({
  type: z.enum(['RECEIPT', 'WRITE_OFF', 'ADJUSTMENT']),
  quantity: z.coerce.number<string>().int('Бүтін сан болуы керек').refine((n) => n !== 0, 'Нөлге тең болмауы керек').refine((n) => Math.abs(n) <= 100_000, 'Тым үлкен'),
  reason: z.string().trim().min(3, 'Қысқаша себебін жазыңыз').max(255, 'Тым ұзын'),
}).refine((v) => v.type === 'ADJUSTMENT' || v.quantity > 0, { path: ['quantity'], message: 'Оң сан енгізіңіз' })

type In = z.input<typeof schema>
type Out = z.output<typeof schema>

const types = [
  { key: 'RECEIPT', label: 'Кіріс', icon: <PackagePlus className="size-4" />, hint: 'Тауар келді (+)' },
  { key: 'WRITE_OFF', label: 'Шығару', icon: <PackageMinus className="size-4" />, hint: 'Берілді немесе бүлінді (-)' },
  { key: 'ADJUSTMENT', label: 'Түзету', icon: <SlidersHorizontal className="size-4" />, hint: 'Түгендеу (+/-)' },
] as const

export function AdjustStockModal({ product, onClose }: { product: Product | null; onClose: () => void }) {
  const qc = useQueryClient()
  const toast = useToast()
  const { register, handleSubmit, watch, setValue, reset, formState: { errors } } = useForm<In, unknown, Out>({
    resolver: zodResolver(schema), defaultValues: { type: 'WRITE_OFF', quantity: '1', reason: '' },
  })
  useEffect(() => { if (product) reset({ type: 'WRITE_OFF', quantity: '1', reason: '' }) }, [product, reset])
  const type = watch('type')
  const qty = Number(watch('quantity')) || 0
  const delta = type === 'RECEIPT' ? qty : type === 'WRITE_OFF' ? -qty : qty
  const after = product ? product.quantity + delta : 0

  const adjust = useMutation({
    mutationFn: (v: Out) => api.post<AdjustmentResult>(`/inventory/products/${product!.id}/adjustments`, v),
    onSuccess: (r) => {
      toast.success('Қор жаңартылды', `${r.product.name}: ${r.transaction.quantityBefore} → ${r.product.quantity} ${r.product.unit}`)
      if (r.lowStockTriggered) {
        toast.warning('Төмен қор оқиғасы жарияланды', `Observer-лер ескерту жасап, қызметкерлерге хабарлады (стратегия: ${humanize(r.detectionStrategy)}).`)
      }
      qc.invalidateQueries({ queryKey: ['inventory'] })
      qc.invalidateQueries({ queryKey: ['dashboard'] })
      onClose()
    },
    onError: (e) => toast.error('Қор өзгертілмеді', errorMessage(e)),
  })

  return (
    <Modal open={!!product} onClose={onClose} size="md" title="Қор қозғалысы" subtitle={product ? `${product.name} (${product.sku})` : ''}
      icon={<span className="grid size-10 place-items-center rounded-xl bg-teal-soft text-teal"><ArrowLeftRight className="size-5" /></span>}
      footer={<>
        <Button variant="secondary" onClick={onClose}>Болдырмау</Button>
        <Button type="submit" form="adjust-form" loading={adjust.isPending}>Қозғалысты орындау</Button>
      </>}>
      {product && (
        <form id="adjust-form" onSubmit={handleSubmit((v) => adjust.mutate(v))} className="space-y-4" noValidate>
          <div className="grid grid-cols-3 gap-2" role="radiogroup" aria-label="Қозғалыс түрі">
            {types.map((t) => (
              <button key={t.key} type="button" role="radio" aria-checked={type === t.key} onClick={() => setValue('type', t.key)}
                className={cx('rounded-xl border p-3 text-left transition', type === t.key ? 'border-primary bg-primary-soft text-primary' : 'border-line hover:border-primary/30')}>
                {t.icon}
                <p className="mt-1.5 text-sm font-semibold">{t.label}</p>
                <p className="text-[11px] text-muted">{t.hint}</p>
              </button>
            ))}
          </div>
          <Field label={type === 'ADJUSTMENT' ? 'Түзету (+/-)' : 'Мөлшері'} error={errors.quantity?.message} htmlFor="aq">
            <input id="aq" inputMode="numeric" className="input" {...register('quantity')} />
          </Field>
          <Field label="Себебі" error={errors.reason?.message} htmlFor="ar">
            <input id="ar" className="input" placeholder="мысалы: №4471 жеткізілім, емханаға берілді" {...register('reason')} />
          </Field>
          <div className="flex items-center justify-between rounded-xl bg-canvas px-4 py-3 text-sm">
            <span className="text-muted">Нәтиже</span>
            <span className="font-semibold tabular-nums">{product.quantity} &rarr; <span className={after < 0 ? 'text-danger' : after <= product.criticalThreshold ? 'text-[#C9731C]' : 'text-mint'}>{after}</span> {product.unit}</span>
          </div>
          {after >= 0 && after <= product.criticalThreshold && (
            <p className="text-xs font-medium text-[#C9731C]">Тауар сыни деңгейге ({product.criticalThreshold}) жетеді — LowStockEvent оқиғасы жарияланады.</p>
          )}
          {after < 0 && <p className="text-xs font-medium text-danger">Қор жеткіліксіз — сервер бұл қозғалысты қабылдамайды.</p>}
        </form>
      )}
    </Modal>
  )
}
