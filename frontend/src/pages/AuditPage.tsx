import { useEffect, useState } from 'react'
import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { ScrollText } from 'lucide-react'
import { api } from '../lib/api'
import type { AuditEntry, Page } from '../lib/types'
import { actorName, dateTime, humanize } from '../lib/format'
import { PageHeader } from '../components/ui/PageHeader'
import { Card } from '../components/ui/Card'
import { Badge, StatusBadge } from '../components/ui/Badge'
import { SearchInput } from '../components/ui/SearchInput'
import { Pagination } from '../components/ui/Pagination'
import { Table, Td, Th, Tr } from '../components/ui/Table'
import { EmptyState, ErrorState, SkeletonRows } from '../components/ui/States'
import { useDebounced } from '../hooks/useDebounced'
import { label, t } from '../lib/i18n'

export function AuditPage() {
  const [action, setAction] = useState('')
  const [entityType, setEntityType] = useState('')
  const [outcome, setOutcome] = useState('')
  const [actor, setActor] = useState('')
  const [page, setPage] = useState(0)
  const a = useDebounced(actor)
  useEffect(() => setPage(0), [action, entityType, outcome, a])
  const actions = useQuery({ queryKey: ['audit', 'actions'], queryFn: () => api.get<string[]>('/audit/actions') })
  const list = useQuery({
    queryKey: ['audit', { action, entityType, outcome, a, page }],
    queryFn: () => api.get<Page<AuditEntry>>('/audit', { action, entityType, outcome, actor: a, page, size: 20 }),
    placeholderData: keepPreviousData,
  })
  return (
    <div className="space-y-6">
      <PageHeader accent="violet" icon={<ScrollText className="size-6" />} title={t('nav.audit')}
        description="CommandInvoker арқылы орындалған әр Command (сәтті немесе қабылданбаған), жүйеге кірулер және аудит бақылаушысы тіркеген әр бизнес-оқиға (EVENT_*)."
        stats={[{ label: 'Жазбалар', value: list.data?.totalElements ?? '-' }, { label: 'Әрекет түрлері', value: actions.data?.length ?? '-' }]} />
      <Card>
        <div className="flex flex-col gap-3 p-4 sm:flex-row sm:flex-wrap sm:items-center sm:p-5">
          <SearchInput value={actor} onChange={setActor} placeholder="Пайдаланушы бойынша сүзу" />
          <select className="input sm:w-64" value={action} onChange={(e) => setAction(e.target.value)} aria-label="Әрекет">
            <option value="">Барлық әрекеттер</option>{actions.data?.map((x) => <option key={x} value={x}>{label(x)}</option>)}
          </select>
          <select className="input sm:w-44" value={entityType} onChange={(e) => setEntityType(e.target.value)} aria-label="Нысан">
            <option value="">Барлық нысандар</option>
            {['Product', 'Appointment', 'Patient', 'FlightBooking', 'Document', 'Notification', 'User', 'INVENTORY', 'CLINIC', 'FLIGHT', 'WORKFLOW'].map((x) => <option key={x} value={x}>{label(x)}</option>)}
          </select>
          <select className="input sm:w-36" value={outcome} onChange={(e) => setOutcome(e.target.value)} aria-label="Нәтиже">
            <option value="">Кез келген нәтиже</option><option value="SUCCESS">Сәтті</option><option value="FAILURE">Сәтсіз</option>
          </select>
        </div>
        {list.isLoading ? <SkeletonRows /> : list.isError ? <ErrorState error={list.error} /> : list.data!.content.length === 0 ? <EmptyState title="Жазба жоқ" /> : (
          <>
            <Table minWidth={900}>
              <thead><tr><Th>Уақыты</Th><Th>Орындаушы</Th><Th>Әрекет</Th><Th>Нысан</Th><Th>Нәтиже</Th><Th>Толығырақ</Th></tr></thead>
              <tbody>
                {list.data!.content.map((e) => (
                  <Tr key={e.id}>
                    <Td className="whitespace-nowrap text-muted">{dateTime(e.createdAt)}</Td>
                    <Td className="font-semibold">{actorName(e.actor)}</Td>
                    <Td><p className="text-sm font-medium">{label(e.action)}</p><p className="font-mono text-[10px] text-muted">{e.action}</p></Td>
                    <Td><Badge>{label(e.entityType)}{e.entityId ? ` #${e.entityId}` : ''}</Badge></Td>
                    <Td><StatusBadge status={e.outcome === 'SUCCESS' ? 'SUCCESS' : 'FAILURE'} label={humanize(e.outcome)} /></Td>
                    <Td className="max-w-96 truncate text-muted" title={e.details ?? ''}>{e.details}</Td>
                  </Tr>
                ))}
              </tbody>
            </Table>
            <Pagination page={list.data!.page} totalPages={list.data!.totalPages} totalElements={list.data!.totalElements} onPage={setPage} />
          </>
        )}
      </Card>
    </div>
  )
}
