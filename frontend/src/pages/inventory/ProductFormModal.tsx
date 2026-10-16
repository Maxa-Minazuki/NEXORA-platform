import { useEffect } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { productSchema } from './productSchema'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { PackagePlus } from 'lucide-react'
import { api, ApiError, errorMessage } from '../../lib/api'
import type { Category, Product, Supplier } from '../../lib/types'
import { Modal } from '../../components/ui/Modal'
import { Field } from '../../components/ui/Field'
import { Button } from '../../components/ui/Button'
import { useToast } from '../../components/ui/Toast'


type FormInput = z.input<typeof productSchema>
type FormOutput = z.output<typeof productSchema>

const empty: FormInput = {
  sku: '', name: '', description: '', categoryId: '', supplierId: '', purchasePrice: '', retailPrice: '',
  quantity: '0', minStock: '10', unit: 'дана', replenishmentStrategy: 'TOP_UP_TO_TARGET',
}

export function ProductFormModal({ open, onClose, product }: { open: boolean; onClose: () => void; product?: Product }) {
  const qc = useQueryClient()
  const toast = useToast()
  const categories = useQuery({ queryKey: ['inventory', 'categories'], queryFn: () => api.get<Category[]>('/inventory/categories'), enabled: open })
  const suppliers = useQuery({ queryKey: ['inventory', 'suppliers'], queryFn: () => api.get<Supplier[]>('/inventory/suppliers'), enabled: open })
  const { register, handleSubmit, reset, setError, formState: { errors } } = useForm<FormInput, unknown, FormOutput>({
    resolver: zodResolver(productSchema),
    defaultValues: empty,
  })

  useEffect(() => {
    if (!open) return
    reset(product ? {
      sku: product.sku, name: product.name, description: product.description ?? '', categoryId: String(product.categoryId),
      supplierId: product.supplierId ? String(product.supplierId) : '', purchasePrice: String(product.purchasePrice),
      retailPrice: String(product.retailPrice), quantity: String(product.quantity), minStock: String(product.minStock),
      unit: product.unit, replenishmentStrategy: product.replenishmentStrategy,
    } : empty)
  }, [open, product, reset])

  const save = useMutation({
    mutationFn: (v: FormOutput) => {
      const body = { ...v, categoryId: Number(v.categoryId), supplierId: v.supplierId ? Number(v.supplierId) : null }
      return product ? api.put<Product>(`/inventory/products/${product.id}`, body) : api.post<Product>('/inventory/products', body)
    },
    onSuccess: (p) => {
      toast.success(product ? 'Тауар жаңартылды' : 'Тауар қосылды', `${p.sku} - ${p.name}`)
      if (p.stockStatus !== 'IN_STOCK') toast.warning('Қор сыни деңгейде', 'Бұл тауар бойынша төмен қор ескертуі жасалды.')
      qc.invalidateQueries({ queryKey: ['inventory'] })
      qc.invalidateQueries({ queryKey: ['dashboard'] })
      onClose()
    },
    onError: (e) => {
      if (e instanceof ApiError && e.fieldErrors.length) {
        e.fieldErrors.forEach((f) => setError(f.field as keyof FormInput, { message: f.message }))
      }
      toast.error('Тауар сақталмады', errorMessage(e))
    },
  })

  return (
    <Modal open={open} onClose={onClose} size="lg" title={product ? `Өңдеу: ${product.name}` : 'Тауар қосу'}
      subtitle={product ? 'Қор мөлшері тек қор қозғалысы арқылы өзгереді.' : 'Бастапқы қалдық алғашқы қозғалыс ретінде тіркеледі.'}
      icon={<span className="grid size-10 place-items-center rounded-xl bg-sky-soft text-sky"><PackagePlus className="size-5" /></span>}
      footer={<>
        <Button variant="secondary" onClick={onClose}>Болдырмау</Button>
        <Button type="submit" form="product-form" loading={save.isPending}>{product ? 'Өзгерістерді сақтау' : 'Тауар қосу'}</Button>
      </>}>
      <form id="product-form" onSubmit={handleSubmit((v) => save.mutate(v))} className="grid gap-4 sm:grid-cols-2" noValidate>
        <Field label="SKU" error={errors.sku?.message} htmlFor="sku">
          <input id="sku" className="input uppercase" disabled={!!product} {...register('sku')} />
        </Field>
        <Field label="Атауы" error={errors.name?.message} htmlFor="pname"><input id="pname" className="input" {...register('name')} /></Field>
        <div className="sm:col-span-2">
          <Field label="Сипаттамасы" error={errors.description?.message} htmlFor="desc"><textarea id="desc" rows={2} className="input" {...register('description')} /></Field>
        </div>
        <Field label="Санат" error={errors.categoryId?.message} htmlFor="cat">
          <select id="cat" className="input" {...register('categoryId')}>
            <option value="">Таңдаңыз...</option>
            {categories.data?.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
          </select>
        </Field>
        <Field label="Жеткізуші" htmlFor="sup">
          <select id="sup" className="input" {...register('supplierId')}>
            <option value="">Жеткізушісіз</option>
            {suppliers.data?.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}
          </select>
        </Field>
        <Field label="Сатып алу бағасы (₸)" error={errors.purchasePrice?.message} htmlFor="pp"><input id="pp" inputMode="decimal" className="input" {...register('purchasePrice')} /></Field>
        <Field label="Бөлшек баға (₸)" error={errors.retailPrice?.message} htmlFor="rp"><input id="rp" inputMode="decimal" className="input" {...register('retailPrice')} /></Field>
        <Field label={product ? 'Мөлшері (тек оқу)' : 'Бастапқы мөлшер'} error={errors.quantity?.message} htmlFor="qty">
          <input id="qty" inputMode="numeric" className="input" disabled={!!product} {...register('quantity')} />
        </Field>
        <Field label="Ең аз қор" error={errors.minStock?.message} hint="Төмен қор стратегиясы қолданатын сыни деңгей" htmlFor="min">
          <input id="min" inputMode="numeric" className="input" {...register('minStock')} />
        </Field>
        <Field label="Өлшем бірлігі" error={errors.unit?.message} htmlFor="unit"><input id="unit" className="input" {...register('unit')} /></Field>
        <Field label="Толықтыру стратегиясы" hint="Ұсынылатын тапсырыс мөлшерін анықтайды" htmlFor="strat">
          <select id="strat" className="input" {...register('replenishmentStrategy')}>
            <option value="TOP_UP_TO_TARGET">Ең аз қордың 3 есесіне дейін</option>
            <option value="FIXED_LOT">50 бірліктік тұрақты партиялар</option>
            <option value="DEMAND_BASED">Сұранысқа негізделген (30 күн)</option>
          </select>
        </Field>
      </form>
    </Modal>
  )
}
