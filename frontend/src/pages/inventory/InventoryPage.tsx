import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router'
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  Archive, ArrowLeftRight, Boxes, FilePlus, ListChecks, Package, PackagePlus, Pencil, RotateCcw, Send, TriangleAlert,
} from 'lucide-react'
import { api, errorMessage } from '../../lib/api'
import type { Category, InventoryStats, InventoryTransaction, MovementType, Page, Product, StockAlert, WorkflowOutcome } from '../../lib/types'
import { compactMoney, dateTime, humanize, money, number, relative } from '../../lib/format'
import { PageHeader } from '../../components/ui/PageHeader'
import { Tabs } from '../../components/ui/Tabs'
import { Card } from '../../components/ui/Card'
import { Button } from '../../components/ui/Button'
import { Badge, StatusBadge } from '../../components/ui/Badge'
import { SearchInput } from '../../components/ui/SearchInput'
import { Pagination } from '../../components/ui/Pagination'
import { SortHeader, Table, Td, Th, Tr } from '../../components/ui/Table'
import { EmptyState, ErrorState, SkeletonRows } from '../../components/ui/States'
import { useToast } from '../../components/ui/Toast'
import { useDebounced } from '../../hooks/useDebounced'
import { useTab } from '../../hooks/useTab'
import { useAuth } from '../../auth/AuthContext'
import { t } from '../../lib/i18n'
import { ProductFormModal } from './ProductFormModal'
import { AdjustStockModal } from './AdjustStockModal'

export function InventoryPage() {
  const [tab, setTab] = useTab('products')
  const [params, setParams] = useSearchParams()
  const [creating, setCreating] = useState(params.get('new') === '1')
  const stats = useQuery({ queryKey: ['inventory', 'stats'], queryFn: () => api.get<InventoryStats>('/inventory/stats') })

  useEffect(() => {
    if (params.get('new') === '1') {
      const next = new URLSearchParams(params)
      next.delete('new')
      setParams(next, { replace: true })
    }
  }, [params, setParams])

  const s = stats.data
  return (
    <div className="space-y-6">
      <PageHeader accent="sky" icon={<Package className="size-6" />} title={t('nav.inventory')}
        description="Тауарлар, қор деңгейлері және қозғалыстар. Әр қозғалыс — Command; сыни деңгейден өткенде LowStockEvent жарияланып, оны ескертулер, хабарландырулар және басқару панелі бақылайды."
        actions={<Button icon={<PackagePlus className="size-4" />} onClick={() => setCreating(true)}>Тауар қосу</Button>}
        stats={[
          { label: 'Тауарлар', value: s ? number(s.totalProducts) : '-' },
          { label: 'Қор құны', value: s ? compactMoney(s.stockValue) : '-' },
          { label: 'Аз / таусылған', value: s ? `${s.lowStockCount} / ${s.outOfStockCount}` : '-' },
          { label: 'Ескерту стратегиясы', value: s ? humanize(s.detectionStrategy) : '-' },
        ]} />
      <Tabs value={tab} onChange={setTab} items={[
        { key: 'products', label: 'Тауарлар', icon: <Boxes className="size-4" /> },
        { key: 'movements', label: 'Қозғалыстар', icon: <ListChecks className="size-4" /> },
        { key: 'alerts', label: 'Төмен қор ескертулері', icon: <TriangleAlert className="size-4" />, count: s?.openAlerts },
      ]} />
      {tab === 'products' && <ProductsTab initialQuery={params.get('q') ?? ''} />}
      {tab === 'movements' && <MovementsTab />}
      {tab === 'alerts' && <AlertsTab />}
      <ProductFormModal open={creating} onClose={() => setCreating(false)} />
    </div>
  )
}

function ProductsTab({ initialQuery }: { initialQuery: string }) {
  const { can } = useAuth()
  const qc = useQueryClient()
  const toast = useToast()
  const [q, setQ] = useState(initialQuery)
  const [categoryId, setCategoryId] = useState('')
  const [availability, setAvailability] = useState('')
  const [minValue, setMinValue] = useState('')
  const [maxValue, setMaxValue] = useState('')
  const [includeInactive, setIncludeInactive] = useState(false)
  const [sort, setSort] = useState('name,asc')
  const [page, setPage] = useState(0)
  const [editing, setEditing] = useState<Product | null>(null)
  const [adjusting, setAdjusting] = useState<Product | null>(null)
  const query = useDebounced(q)
  const min = useDebounced(minValue)
  const max = useDebounced(maxValue)

  useEffect(() => setPage(0), [query, categoryId, availability, min, max, includeInactive, sort])

  const categories = useQuery({ queryKey: ['inventory', 'categories'], queryFn: () => api.get<Category[]>('/inventory/categories') })
  const products = useQuery({
    queryKey: ['inventory', 'products', { query, categoryId, availability, min, max, includeInactive, sort, page }],
    queryFn: () => api.get<Page<Product>>('/inventory/products', {
      q: query, categoryId, availability, minValue: min, maxValue: max, includeInactive, sort, page, size: 10,
    }),
    placeholderData: keepPreviousData,
  })
  const archive = useMutation({
    mutationFn: (p: Product) => api.patch<Product>(`/inventory/products/${p.id}/active`, undefined, { value: !p.active }),
    onSuccess: (p) => {
      toast.success(p.active ? 'Тауар қалпына келтірілді' : 'Тауар мұрағатқа жіберілді', p.name)
      qc.invalidateQueries({ queryKey: ['inventory'] })
    },
    onError: (e) => toast.error('Тауар жаңартылмады', errorMessage(e)),
  })

  return (
    <Card>
      <div className="flex flex-col gap-3 p-4 sm:p-5 lg:flex-row lg:flex-wrap lg:items-center">
        <SearchInput value={q} onChange={setQ} placeholder="Атауы немесе SKU бойынша іздеу" />
        <div className="grid grid-cols-2 gap-2 sm:flex sm:flex-wrap">
          <select className="input sm:w-44" value={categoryId} onChange={(e) => setCategoryId(e.target.value)} aria-label="Санат">
            <option value="">Барлық санаттар</option>
            {categories.data?.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
          </select>
          <select className="input sm:w-40" value={availability} onChange={(e) => setAvailability(e.target.value)} aria-label="Қолжетімділік">
            <option value="">Кез келген қолжетімділік</option>
            <option value="IN_STOCK">Қоймада бар</option>
            <option value="CRITICAL">Аз немесе таусылған</option>
            <option value="LOW_STOCK">Қор аз</option>
            <option value="OUT_OF_STOCK">Таусылған</option>
          </select>
          <input className="input sm:w-32" inputMode="numeric" placeholder="Құны, кемі" value={minValue} onChange={(e) => setMinValue(e.target.value.replace(/[^\d.]/g, ''))} aria-label="Қор құнының ең аз мәні" />
          <input className="input sm:w-32" inputMode="numeric" placeholder="Құны, көбі" value={maxValue} onChange={(e) => setMaxValue(e.target.value.replace(/[^\d.]/g, ''))} aria-label="Қор құнының ең көп мәні" />
        </div>
        <label className="flex items-center gap-2 text-sm text-muted lg:ml-auto">
          <input type="checkbox" checked={includeInactive} onChange={(e) => setIncludeInactive(e.target.checked)} className="accent-primary" /> Мұрағатты көрсету
        </label>
      </div>
      {products.isLoading ? <SkeletonRows /> : products.isError ? <ErrorState error={products.error} onRetry={() => products.refetch()} /> : (
        products.data!.content.length === 0 ? (
          <EmptyState title="Тауар табылмады" description="Сүзгілерді өзгертіңіз немесе жаңа тауар қосыңыз." />
        ) : (
          <>
            <Table minWidth={920}>
              <thead>
                <tr>
                  <SortHeader label="Тауар" field="name" sort={sort} onSort={setSort} />
                  <SortHeader label="Қалдық" field="quantity" sort={sort} onSort={setSort} />
                  <Th>Күйі</Th>
                  <SortHeader label="Бағасы" field="retailPrice" sort={sort} onSort={setSort} />
                  <Th>Қор құны</Th>
                  <Th className="text-right">Әрекеттер</Th>
                </tr>
              </thead>
              <tbody>
                {products.data!.content.map((p) => {
                  const pct = Math.min(100, Math.round((p.quantity / Math.max(p.minStock * 3, 1)) * 100))
                  return (
                    <Tr key={p.id} className={p.active ? '' : 'opacity-55'}>
                      <Td className="max-w-72">
                        <p className="truncate font-semibold" title={`${p.name} - ${p.supplierName ?? 'жеткізушісіз'}`}>{p.name}</p>
                        <p className="mt-0.5 flex min-w-0 items-center gap-1.5 text-xs text-muted">
                          <span className="shrink-0">{p.sku}</span><Badge tone="sky" className="truncate">{p.categoryName}</Badge>
                        </p>
                      </Td>
                      <Td>
                        <p className="font-semibold tabular-nums">{number(p.quantity)} <span className="text-xs font-normal text-muted">{p.unit}</span></p>
                        <div className="mt-1 h-1.5 w-24 overflow-hidden rounded-full bg-canvas" title={`сыни деңгей: ${p.criticalThreshold}`}>
                          <div className={p.stockStatus === 'IN_STOCK' ? 'h-full bg-teal' : p.stockStatus === 'LOW_STOCK' ? 'h-full bg-peach' : 'h-full bg-danger'} style={{ width: `${Math.max(pct, 3)}%` }} />
                        </div>
                        <p className="mt-0.5 text-[11px] text-muted">ең аз {p.minStock}</p>
                      </Td>
                      <Td><StatusBadge status={p.stockStatus} /></Td>
                      <Td className="tabular-nums">
                        <p className="font-medium">{money(p.retailPrice)}</p>
                        <p className="text-xs text-muted">өзіндік құны {money(p.purchasePrice)}</p>
                      </Td>
                      <Td className="font-semibold tabular-nums">{money(p.stockValue)}</Td>
                      <Td>
                        <div className="flex justify-end gap-1">
                          <Button size="sm" variant="soft" icon={<ArrowLeftRight className="size-3.5" />} onClick={() => setAdjusting(p)} disabled={!p.active} aria-label={`Қор қозғалысы: ${p.name}`} title="Қор қозғалысы" />
                          <Button size="sm" variant="ghost" icon={<Pencil className="size-3.5" />} onClick={() => setEditing(p)} aria-label={`Өңдеу: ${p.name}`} title="Өңдеу" />
                          {can('ADMIN', 'MANAGER') && (
                            <Button size="sm" variant="ghost" icon={p.active ? <Archive className="size-3.5" /> : <RotateCcw className="size-3.5" />}
                              onClick={() => archive.mutate(p)} aria-label={p.active ? 'Мұрағаттау' : 'Қалпына келтіру'} title={p.active ? 'Мұрағаттау' : 'Қалпына келтіру'} />
                          )}
                        </div>
                      </Td>
                    </Tr>
                  )
                })}
              </tbody>
            </Table>
            <Pagination page={products.data!.page} totalPages={products.data!.totalPages} totalElements={products.data!.totalElements} onPage={setPage} />
          </>
        )
      )}
      <ProductFormModal open={!!editing} product={editing ?? undefined} onClose={() => setEditing(null)} />
      <AdjustStockModal product={adjusting} onClose={() => setAdjusting(null)} />
    </Card>
  )
}

function MovementsTab() {
  const [type, setType] = useState<MovementType | ''>('')
  const [page, setPage] = useState(0)
  const tx = useQuery({
    queryKey: ['inventory', 'transactions', type, page],
    queryFn: () => api.get<Page<InventoryTransaction>>('/inventory/transactions', { type, page, size: 15 }),
    placeholderData: keepPreviousData,
  })
  return (
    <Card>
      <div className="flex flex-wrap items-center gap-3 p-4 sm:p-5">
        <select className="input w-48" value={type} onChange={(e) => { setType(e.target.value as MovementType | ''); setPage(0) }} aria-label="Қозғалыс түрі">
          <option value="">Барлық қозғалыс түрлері</option>
          <option value="RECEIPT">Кірістер</option>
          <option value="WRITE_OFF">Есептен шығарулар</option>
          <option value="ADJUSTMENT">Түзетулер</option>
          <option value="INITIAL">Бастапқы қалдықтар</option>
        </select>
        <p className="text-sm text-muted">Қордың әр өзгерісінің өзгермейтін журналы.</p>
      </div>
      {tx.isLoading ? <SkeletonRows /> : tx.isError ? <ErrorState error={tx.error} /> : tx.data!.content.length === 0 ? <EmptyState title="Әзірге қозғалыс жоқ" /> : (
        <>
          <Table minWidth={760}>
            <thead><tr><Th>Уақыты</Th><Th>Тауар</Th><Th>Түрі</Th><Th className="text-right">Өзгеріс</Th><Th className="text-right">Бұрын &rarr; Кейін</Th><Th>Себебі</Th></tr></thead>
            <tbody>
              {tx.data!.content.map((t) => (
                <Tr key={t.id}>
                  <Td className="whitespace-nowrap text-muted">{dateTime(t.createdAt)}</Td>
                  <Td><p className="font-semibold">{t.productName}</p><p className="text-xs text-muted">{t.productSku}</p></Td>
                  <Td><StatusBadge status={t.type} /></Td>
                  <Td className={`text-right font-semibold tabular-nums ${t.quantityChange > 0 ? 'text-mint' : 'text-danger'}`}>{t.quantityChange > 0 ? '+' : ''}{t.quantityChange}</Td>
                  <Td className="text-right text-muted tabular-nums">{t.quantityBefore} &rarr; <span className="font-semibold text-ink">{t.quantityAfter}</span></Td>
                  <Td className="max-w-64 truncate text-muted">{t.reason}</Td>
                </Tr>
              ))}
            </tbody>
          </Table>
          <Pagination page={tx.data!.page} totalPages={tx.data!.totalPages} totalElements={tx.data!.totalElements} onPage={setPage} />
        </>
      )}
    </Card>
  )
}

function AlertsTab() {
  const [openOnly, setOpenOnly] = useState(true)
  const qc = useQueryClient()
  const toast = useToast()
  const alerts = useQuery({ queryKey: ['inventory', 'alerts', openOnly], queryFn: () => api.get<StockAlert[]>('/inventory/alerts', { openOnly }) })
  const request = useMutation({
    mutationFn: ({ id, submit }: { id: number; submit: boolean }) =>
      api.post<WorkflowOutcome>(`/workflows/replenishment-from-alert/${id}`, { submit }),
    onSuccess: (o) => {
      toast.success(`${o.document.summary.number} жасалды`, o.document.summary.status === 'DRAFT'
        ? 'Қаралама сақталды — оны «Құжат айналымы» бөлімінен жіберіңіз.' : `«${o.document.summary.currentStep}» қадамына жіберілді.`)
      qc.invalidateQueries({ queryKey: ['inventory'] })
      qc.invalidateQueries({ queryKey: ['documents'] })
    },
    onError: (e) => toast.error('Өтінім жасалмады', errorMessage(e)),
  })

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center gap-3">
        <label className="flex items-center gap-2 text-sm text-muted">
          <input type="checkbox" className="accent-primary" checked={openOnly} onChange={(e) => setOpenOnly(e.target.checked)} /> Тек ашық ескертулер
        </label>
        <p className="text-sm text-muted">Ескертулерді <b>LowStockAlertRecorder</b> бақылаушысы жасайды; ұсынылатын мөлшер әр тауардың толықтыру стратегиясымен есептеледі.</p>
      </div>
      {alerts.isLoading ? <Card><SkeletonRows rows={3} /></Card> : alerts.isError ? <Card><ErrorState error={alerts.error} /></Card> :
        alerts.data!.length === 0 ? <Card><EmptyState icon={<TriangleAlert className="size-6" />} title="Төмен қор ескертулері жоқ" description="Барлық тауарлар сыни деңгейден жоғары." /></Card> : (
          <div className="stagger grid gap-4 md:grid-cols-2 xl:grid-cols-3">
            {alerts.data!.map((a) => (
              <Card key={a.id} className="flex flex-col p-5">
                <div className="flex items-start justify-between gap-3">
                  <div className="min-w-0">
                    <p className="truncate font-display font-bold">{a.productName}</p>
                    <p className="text-xs text-muted">{a.productSku} &middot; {relative(a.createdAt)}</p>
                  </div>
                  <StatusBadge status={a.status} />
                </div>
                <div className="mt-4 grid grid-cols-3 gap-2 text-center">
                  <div className="rounded-xl bg-danger-soft/60 py-2"><p className="text-[11px] text-muted">Қазір</p><p className="font-display text-lg font-bold text-danger">{a.currentQuantity}</p></div>
                  <div className="rounded-xl bg-canvas py-2"><p className="text-[11px] text-muted">Шегі</p><p className="font-display text-lg font-bold">{a.threshold}</p></div>
                  <div className="rounded-xl bg-teal-soft py-2"><p className="text-[11px] text-muted">Ұсыныс</p><p className="font-display text-lg font-bold text-[#169C90]">{a.suggestedQuantity}</p></div>
                </div>
                <p className="mt-3 text-xs text-muted">Стратегия: <b>{humanize(a.strategy)}</b></p>
                <div className="mt-auto flex flex-wrap gap-2 pt-4">
                  {a.status === 'OPEN' ? (
                    <>
                      <Button size="sm" icon={<Send className="size-3.5" />} loading={request.isPending && request.variables?.id === a.id && request.variables.submit}
                        onClick={() => request.mutate({ id: a.id, submit: true })}>Өтінім жасап, жіберу</Button>
                      <Button size="sm" variant="secondary" icon={<FilePlus className="size-3.5" />}
                        onClick={() => request.mutate({ id: a.id, submit: false })}>Тек қаралама</Button>
                    </>
                  ) : a.documentId ? (
                    <Link to={`/documents/${a.documentId}`}><Button size="sm" variant="soft">Өтінімді ашу</Button></Link>
                  ) : null}
                </div>
              </Card>
            ))}
          </div>
        )}
    </div>
  )
}
