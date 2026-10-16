import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router'
import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { FilePlus, FileText, Inbox, Layers } from 'lucide-react'
import { api } from '../../lib/api'
import type { DocumentStatus, DocumentSummary, DocumentType, DocumentTypeInfo, Page } from '../../lib/types'
import { dateTime, humanize, money, relative } from '../../lib/format'
import { PageHeader } from '../../components/ui/PageHeader'
import { Tabs } from '../../components/ui/Tabs'
import { Card } from '../../components/ui/Card'
import { Button } from '../../components/ui/Button'
import { Badge, StatusBadge } from '../../components/ui/Badge'
import { SearchInput } from '../../components/ui/SearchInput'
import { Pagination } from '../../components/ui/Pagination'
import { SortHeader, Table, Td, Th, Tr } from '../../components/ui/Table'
import { EmptyState, ErrorState, SkeletonRows } from '../../components/ui/States'
import { useDebounced } from '../../hooks/useDebounced'
import { useTab } from '../../hooks/useTab'
import { useAuth } from '../../auth/AuthContext'
import { CreateDocumentModal } from './CreateDocumentModal'
import { CLINIC_OFFSET } from '../clinic/clinicTime'
import { label, t } from '../../lib/i18n'

export function DocumentsPage() {
  const { can } = useAuth()
  const approver = can('ADMIN', 'MANAGER')
  const [tab, setTab] = useTab('all')
  const [params, setParams] = useSearchParams()
  const [creating, setCreating] = useState(params.get('new') === '1')
  useEffect(() => {
    if (params.get('new') === '1') { const n = new URLSearchParams(params); n.delete('new'); setParams(n, { replace: true }) }
  }, [params, setParams])
  const inbox = useQuery({ queryKey: ['documents', 'inbox'], queryFn: () => api.get<DocumentSummary[]>('/workflows/inbox'), enabled: approver })
  const pending = useQuery({ queryKey: ['documents', 'count', 'pending'], queryFn: () => api.get<Page<DocumentSummary>>('/documents', { status: 'PENDING_APPROVAL', size: 1 }) })
  const review = useQuery({ queryKey: ['documents', 'count', 'review'], queryFn: () => api.get<Page<DocumentSummary>>('/documents', { status: 'IN_REVIEW', size: 1 }) })
  const approved = useQuery({ queryKey: ['documents', 'count', 'approved'], queryFn: () => api.get<Page<DocumentSummary>>('/documents', { status: 'APPROVED', size: 1 }) })
  const drafts = useQuery({ queryKey: ['documents', 'count', 'draft'], queryFn: () => api.get<Page<DocumentSummary>>('/documents', { status: 'DRAFT', size: 1 }) })

  return (
    <div className="space-y-6">
      <PageHeader accent="peach" icon={<FileText className="size-6" />} title={t('nav.documents')}
        description="Құжаттарды жасау, жіберу және бекіту. Әр әрекет — Command, Chain of Responsibility тексеріп маршрут құрады, State өмірлік циклді қорғайды, ал WorkflowMediator қатысушыларды үйлестіреді."
        actions={can('ADMIN', 'MANAGER', 'EMPLOYEE', 'DOCTOR') && <Button icon={<FilePlus className="size-4" />} onClick={() => setCreating(true)}>Құжат жасау</Button>}
        stats={[
          { label: 'Қараламалар', value: drafts.data?.totalElements ?? '-' },
          { label: 'Бекітуді күтуде', value: pending.data?.totalElements ?? '-' },
          { label: 'Қаралуда', value: review.data?.totalElements ?? '-' },
          { label: 'Бекітілген', value: approved.data?.totalElements ?? '-' },
        ]} />
      <Tabs value={tab} onChange={setTab} items={[
        { key: 'all', label: approver ? 'Барлық құжаттар' : 'Менің құжаттарым', icon: <Layers className="size-4" /> },
        ...(approver ? [{ key: 'inbox', label: 'Менің шешімімді күтуде', icon: <Inbox className="size-4" />, count: inbox.data?.length }] : []),
      ]} />
      {tab === 'inbox' && approver ? <InboxList items={inbox.data} loading={inbox.isLoading} /> : <DocumentList initialQuery={params.get('q') ?? ''} initialStatus={(params.get('status') as DocumentStatus) ?? ''} />}
      <CreateDocumentModal open={creating} onClose={() => setCreating(false)} />
    </div>
  )
}

function InboxList({ items, loading }: { items?: DocumentSummary[]; loading: boolean }) {
  if (loading) return <Card><SkeletonRows /></Card>
  if (!items?.length) return <Card><EmptyState icon={<Inbox className="size-6" />} title="Кіріс бос" description="Сіздің рөліңізді күтіп тұрған бекіту қадамы жоқ." /></Card>
  return (
    <div className="stagger grid gap-4 md:grid-cols-2 xl:grid-cols-3">
      {items.map((d) => (
        <Link key={d.id} to={`/documents/${d.id}`}>
          <Card className="h-full p-5 transition hover:-translate-y-0.5 hover:shadow-pop">
            <div className="flex items-center justify-between gap-2"><span className="text-xs font-bold text-muted">{d.number}</span><StatusBadge status={d.status} /></div>
            <p className="mt-2 line-clamp-2 font-display font-bold">{d.title}</p>
            <p className="mt-1 text-xs text-muted">{d.typeLabel} &middot; {d.authorName}</p>
            <div className="mt-4 flex items-center justify-between">
              <Badge tone="violet">{d.currentStep} &middot; {label(d.currentApproverRole)}</Badge>
              <span className="text-sm font-semibold">{d.amount !== null ? money(d.amount) : ''}</span>
            </div>
            <p className="mt-3 text-[11px] text-muted">Жіберілді: {d.submittedAt ? relative(d.submittedAt) : '-'}</p>
          </Card>
        </Link>
      ))}
    </div>
  )
}

function DocumentList({ initialQuery, initialStatus }: { initialQuery: string; initialStatus: DocumentStatus | '' }) {
  const { can } = useAuth()
  const [q, setQ] = useState(initialQuery)
  const [type, setType] = useState<DocumentType | ''>('')
  const [status, setStatus] = useState<DocumentStatus | ''>(initialStatus)
  const [mine, setMine] = useState(false)
  const [from, setFrom] = useState('')
  const [to, setTo] = useState('')
  const [sort, setSort] = useState('updatedAt,desc')
  const [page, setPage] = useState(0)
  const query = useDebounced(q)
  useEffect(() => setPage(0), [query, type, status, mine, from, to, sort])
  const types = useQuery({ queryKey: ['documents', 'types'], queryFn: () => api.get<DocumentTypeInfo[]>('/documents/types') })
  const list = useQuery({
    queryKey: ['documents', 'list', { query, type, status, mine, from, to, sort, page }],
    queryFn: () => api.get<Page<DocumentSummary>>('/documents', {
      q: query, type, status, mine, sort, page, size: 10,
      from: from ? new Date(`${from}T00:00:00${CLINIC_OFFSET}`).toISOString() : undefined,
      to: to ? new Date(new Date(`${to}T00:00:00${CLINIC_OFFSET}`).getTime() + 86_400_000).toISOString() : undefined,
    }),
    placeholderData: keepPreviousData,
  })
  return (
    <Card>
      <div className="flex flex-col gap-3 p-4 sm:p-5 lg:flex-row lg:flex-wrap lg:items-center">
        <SearchInput value={q} onChange={setQ} placeholder="Тақырып немесе нөмір бойынша іздеу" />
        <div className="grid grid-cols-2 gap-2 sm:flex sm:flex-wrap">
          <select className="input sm:w-52" value={type} onChange={(e) => setType(e.target.value as DocumentType | '')} aria-label="Түрі">
            <option value="">Барлық түрлер</option>
            {types.data?.map((t) => <option key={t.type} value={t.type}>{t.label}</option>)}
          </select>
          <select className="input sm:w-44" value={status} onChange={(e) => setStatus(e.target.value as DocumentStatus | '')} aria-label="Күйі">
            <option value="">Кез келген күй</option>
            {['DRAFT', 'PENDING_APPROVAL', 'IN_REVIEW', 'APPROVED', 'REJECTED'].map((s) => <option key={s} value={s}>{humanize(s)}</option>)}
          </select>
          <input type="date" className="input sm:w-40" value={from} onChange={(e) => setFrom(e.target.value)} aria-label="Жасалған күні (бастап)" />
          <input type="date" className="input sm:w-40" value={to} onChange={(e) => setTo(e.target.value)} aria-label="Жасалған күні (дейін)" />
        </div>
        {can('ADMIN', 'MANAGER') && <label className="flex items-center gap-2 text-sm text-muted lg:ml-auto"><input type="checkbox" className="accent-primary" checked={mine} onChange={(e) => setMine(e.target.checked)} /> Тек менікі</label>}
      </div>
      {list.isLoading ? <SkeletonRows /> : list.isError ? <ErrorState error={list.error} /> : list.data!.content.length === 0 ? (
        <EmptyState icon={<FileText className="size-6" />} title="Құжат табылмады" description="Сүзгілерді өзгертіңіз немесе жаңа құжат жасаңыз." />
      ) : (
        <>
          <Table minWidth={900}>
            <thead><tr>
              <SortHeader label="Құжат" field="title" sort={sort} onSort={setSort} />
              <Th>Түрі</Th><Th>Авторы</Th>
              <SortHeader label="Сома" field="amount" sort={sort} onSort={setSort} />
              <SortHeader label="Күйі" field="status" sort={sort} onSort={setSort} />
              <Th>Ағымдағы қадам</Th>
              <SortHeader label="Жаңартылды" field="updatedAt" sort={sort} onSort={setSort} />
            </tr></thead>
            <tbody>
              {list.data!.content.map((d) => (
                <Tr key={d.id}>
                  <Td className="max-w-72"><Link to={`/documents/${d.id}`} className="block truncate font-semibold hover:text-primary" title={d.title}>{d.title}</Link><p className="text-xs text-muted">{d.number}</p></Td>
                  <Td><Badge tone="peach">{d.typeLabel}</Badge></Td>
                  <Td className="text-muted">{d.authorName}</Td>
                  <Td className="tabular-nums">{d.amount !== null ? money(d.amount) : '-'}</Td>
                  <Td><StatusBadge status={d.status} /></Td>
                  <Td className="text-sm">{d.currentStep ? <span>{d.currentStep} <span className="text-xs text-muted">({label(d.currentApproverRole)})</span></span> : <span className="text-muted">-</span>}</Td>
                  <Td className="whitespace-nowrap text-muted">{dateTime(d.updatedAt)}</Td>
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
