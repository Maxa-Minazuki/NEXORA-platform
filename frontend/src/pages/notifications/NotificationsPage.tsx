import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router'
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Bell, CheckCheck, Cpu, Factory, Mail, MonitorSmartphone, RefreshCw, Send, SlidersHorizontal, Truck } from 'lucide-react'
import { api, errorMessage } from '../../lib/api'
import type { AppNotification, Channel, ChannelInfo, DeliveryAttempt, DispatchReport, NotificationCategory, Page, Preference, Severity } from '../../lib/types'
import { dateTime, dayGroup, humanize, relative } from '../../lib/format'
import { PageHeader } from '../../components/ui/PageHeader'
import { Tabs } from '../../components/ui/Tabs'
import { Card, CardHeader } from '../../components/ui/Card'
import { Button } from '../../components/ui/Button'
import { Badge, StatusBadge } from '../../components/ui/Badge'
import { Pagination } from '../../components/ui/Pagination'
import { Table, Td, Th, Tr } from '../../components/ui/Table'
import { EmptyState, ErrorState, SkeletonRows } from '../../components/ui/States'
import { useToast } from '../../components/ui/Toast'
import { useTab } from '../../hooks/useTab'
import { useAuth } from '../../auth/AuthContext'
import { cx } from '../../components/ui/cx'
import { SeverityDot } from './severity'

const SEVERITIES: Severity[] = ['INFO', 'SUCCESS', 'WARNING', 'CRITICAL']
const CATEGORIES: NotificationCategory[] = ['INVENTORY', 'CLINIC', 'FLIGHT', 'WORKFLOW', 'SYSTEM']

export function NotificationsPage() {
  const [tab, setTab] = useTab('inbox')
  const unread = useQuery({ queryKey: ['notifications', 'оқылмаған'], queryFn: () => api.get<{ count: number }>('/notifications/unread-count') })
  const channels = useQuery({ queryKey: ['notifications', 'channels'], queryFn: () => api.get<ChannelInfo[]>('/notifications/channels') })
  return (
    <div className="space-y-6">
      <PageHeader accent="rose" icon={<Bell className="size-6" />} title="Хабарландыру орталығы"
        description="Хабарландыруларды бизнес-оқиғалар жасайды (Observer). Strategy маңыздылыққа қарай арналарды таңдайды, Factory Method жеткізу өңдеушісін жасайды, ал Abstract Factory арнаның форматтаушысын, жіберушісін және қолжетімділік тексерісін береді."
        stats={[
          { label: 'Оқылмаған', value: unread.data?.count ?? '-' },
          { label: 'Арналар', value: channels.data?.length ?? '-' },
          { label: 'Қосымша ішінде', value: 'Тікелей (SSE)' },
          { label: 'E-mail', value: channels.data?.find((c) => c.channel === 'EMAIL')?.available ? 'SMTP дайын' : 'Демо режимі' },
        ]} />
      <Tabs value={tab} onChange={setTab} items={[
        { key: 'inbox', label: 'Кіріс', icon: <Bell className="size-4" />, count: unread.data?.count },
        { key: 'deliveries', label: 'Жеткізу журналы', icon: <Truck className="size-4" /> },
        { key: 'preferences', label: 'Баптаулар', icon: <SlidersHorizontal className="size-4" /> },
        { key: 'channels', label: 'Арналар және сынақ', icon: <Factory className="size-4" /> },
      ]} />
      {tab === 'inbox' && <Inbox />}
      {tab === 'deliveries' && <Deliveries />}
      {tab === 'preferences' && <Preferences />}
      {tab === 'channels' && <Channels channels={channels.data} />}
    </div>
  )
}

function Inbox() {
  const qc = useQueryClient()
  const navigate = useNavigate()
  const [unreadOnly, setUnreadOnly] = useState(false)
  const [severity, setSeverity] = useState<Severity | ''>('')
  const [category, setCategory] = useState<NotificationCategory | ''>('')
  const [page, setPage] = useState(0)
  const [expanded, setExpanded] = useState<number | null>(null)
  useEffect(() => setPage(0), [unreadOnly, severity, category])
  const list = useQuery({
    queryKey: ['notifications', 'list', { unreadOnly, severity, category, page }],
    queryFn: () => api.get<Page<AppNotification>>('/notifications', { unreadOnly, severity, category, page, size: 15 }),
    placeholderData: keepPreviousData,
  })
  const read = useMutation({ mutationFn: (id: number) => api.post(`/notifications/${id}/read`), onSuccess: () => qc.invalidateQueries({ queryKey: ['notifications'] }) })
  const readAll = useMutation({ mutationFn: () => api.post('/notifications/read-all'), onSuccess: () => qc.invalidateQueries({ queryKey: ['notifications'] }) })

  const groups = new Map<string, AppNotification[]>()
  list.data?.content.forEach((n) => groups.set(dayGroup(n.createdAt), [...(groups.get(dayGroup(n.createdAt)) ?? []), n]))

  return (
    <Card>
      <div className="flex flex-col gap-3 p-4 sm:flex-row sm:flex-wrap sm:items-center sm:p-5">
        <div className="inline-flex rounded-xl border border-line p-1">
          {[false, true].map((u) => (
            <button key={String(u)} onClick={() => setUnreadOnly(u)} className={cx('rounded-lg px-3 py-1.5 text-xs font-semibold', unreadOnly === u ? 'bg-primary text-white' : 'text-muted')}>{u ? 'Оқылмаған' : 'Барлығы'}</button>
          ))}
        </div>
        <select className="input sm:w-40" value={severity} onChange={(e) => setSeverity(e.target.value as Severity | '')} aria-label="Маңыздылығы">
          <option value="">Кез келген маңыздылық</option>{SEVERITIES.map((s) => <option key={s} value={s}>{humanize(s)}</option>)}
        </select>
        <select className="input sm:w-40" value={category} onChange={(e) => setCategory(e.target.value as NotificationCategory | '')} aria-label="Модуль">
          <option value="">Кез келген модуль</option>{CATEGORIES.map((c) => <option key={c} value={c}>{humanize(c)}</option>)}
        </select>
        <Button variant="soft" className="sm:ml-auto" icon={<CheckCheck className="size-4" />} loading={readAll.isPending} onClick={() => readAll.mutate()}>Барлығын оқылды деу</Button>
      </div>
      {list.isLoading ? <SkeletonRows /> : list.isError ? <ErrorState error={list.error} /> : list.data!.content.length === 0 ? (
        <EmptyState icon={<Bell className="size-6" />} title={unreadOnly ? 'Оқылмаған хабарландыру жоқ' : 'Хабарландыру жоқ'} description="Бизнес-оқиғалар (төмен қор, брондау, бекіту) осында нақты уақытта көрсетіледі." />
      ) : (
        <>
          {[...groups.entries()].map(([group, items]) => (
            <div key={group}>
              <p className="border-y border-line bg-canvas/60 px-5 py-2 text-[11px] font-bold tracking-wide text-muted uppercase">{group}</p>
              <ul>
                {items.map((n) => (
                  <li key={n.id} className={cx('border-b border-line/70 px-5 py-3.5 transition', !n.read && 'bg-primary-soft/30')}>
                    <div className="flex gap-3">
                      <SeverityDot severity={n.severity} />
                      <div className="min-w-0 flex-1">
                        <div className="flex flex-wrap items-center gap-2">
                          <p className={cx('text-sm', n.read ? 'font-medium' : 'font-bold')}>{n.title}</p>
                          <Badge>{humanize(n.category)}</Badge>
                          {!n.read && <span className="size-2 rounded-full bg-rose" aria-label="оқылмаған" />}
                        </div>
                        <p className="mt-0.5 text-sm text-muted">{n.message}</p>
                        <div className="mt-2 flex flex-wrap items-center gap-3 text-xs">
                          <span className="text-muted">{relative(n.createdAt)}</span>
                          {n.link && <button className="font-semibold text-primary hover:underline" onClick={() => { if (!n.read) read.mutate(n.id); navigate(n.link!) }}>Ашу</button>}
                          {!n.read && <button className="font-semibold text-muted hover:text-ink" onClick={() => read.mutate(n.id)}>Оқылды деу</button>}
                          <button className="font-semibold text-muted hover:text-ink" onClick={() => setExpanded(expanded === n.id ? null : n.id)}>
                            Жеткізу: {n.deliveries.length}
                          </button>
                        </div>
                        {expanded === n.id && (
                          <ul className="mt-2 space-y-1">
                            {n.deliveries.map((d) => (
                              <li key={d.id} className="flex flex-wrap items-center gap-2 text-xs text-muted">
                                <StatusBadge status={d.status} /> <b>{humanize(d.channel)}</b> #{d.attemptNo} &middot; {d.detail}
                              </li>
                            ))}
                          </ul>
                        )}
                      </div>
                    </div>
                  </li>
                ))}
              </ul>
            </div>
          ))}
          <Pagination page={list.data!.page} totalPages={list.data!.totalPages} totalElements={list.data!.totalElements} onPage={setPage} />
        </>
      )}
    </Card>
  )
}

function Deliveries() {
  const { can } = useAuth()
  const qc = useQueryClient()
  const toast = useToast()
  const [page, setPage] = useState(0)
  const list = useQuery({ queryKey: ['notifications', 'deliveries', page], queryFn: () => api.get<Page<DeliveryAttempt>>('/notifications/deliveries', { page, size: 15 }), placeholderData: keepPreviousData })
  const retry = useMutation({
    mutationFn: (id: number) => api.post<DeliveryAttempt>(`/notifications/deliveries/${id}/retry`),
    onSuccess: (a) => { toast.info(`Қайталау: ${humanize(a.status)}`, a.detail ?? undefined); qc.invalidateQueries({ queryKey: ['notifications'] }) },
    onError: (e) => toast.error('Қайталау сәтсіз', errorMessage(e)),
  })
  return (
    <Card>
      <CardHeader title="Жеткізу әрекеттері" subtitle={can('ADMIN') ? 'Барлық пайдаланушылар (әкімші көрінісі)' : 'Сіздің хабарландыруларыңыздың жеткізілуі'} />
      {list.isLoading ? <SkeletonRows /> : list.isError ? <ErrorState error={list.error} /> : list.data!.content.length === 0 ? <EmptyState title="Әзірге жеткізу әрекеті жоқ" /> : (
        <>
          <Table minWidth={760}>
            <thead><tr><Th>Уақыты</Th><Th>Хабарландыру</Th><Th>Арна</Th><Th>Әрекет №</Th><Th>Күйі</Th><Th>Толығырақ</Th><Th className="text-right">Әрекет</Th></tr></thead>
            <tbody>
              {list.data!.content.map((a) => (
                <Tr key={a.id}>
                  <Td className="whitespace-nowrap text-muted">{dateTime(a.createdAt)}</Td>
                  <Td>#{a.notificationId}</Td>
                  <Td><span className="inline-flex items-center gap-1.5 font-semibold">{a.channel === 'EMAIL' ? <Mail className="size-4 text-muted" /> : <MonitorSmartphone className="size-4 text-muted" />}{humanize(a.channel)}</span></Td>
                  <Td>{a.attemptNo}</Td>
                  <Td><StatusBadge status={a.status} /></Td>
                  <Td className="max-w-72 truncate text-muted" title={a.detail ?? ''}>{a.detail}</Td>
                  <Td className="text-right">{a.retryable && <Button size="sm" variant="soft" icon={<RefreshCw className="size-3.5" />} loading={retry.isPending && retry.variables === a.id} onClick={() => retry.mutate(a.id)}>Қайталау</Button>}</Td>
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

function Preferences() {
  const qc = useQueryClient()
  const toast = useToast()
  const prefs = useQuery({ queryKey: ['notifications', 'preferences'], queryFn: () => api.get<Preference[]>('/notifications/preferences') })
  const [local, setLocal] = useState<Preference[]>([])
  useEffect(() => { if (prefs.data) setLocal(prefs.data) }, [prefs.data])
  const save = useMutation({
    mutationFn: () => api.put<Preference[]>('/notifications/preferences', { preferences: local }),
    onSuccess: (p) => { qc.setQueryData(['notifications', 'preferences'], p); toast.success('Баптаулар сақталды') },
    onError: (e) => toast.error('Сақталмады', errorMessage(e)),
  })
  const update = (channel: Channel, patch: Partial<Preference>) => setLocal((l) => l.map((p) => (p.channel === channel ? { ...p, ...patch } : p)))
  if (prefs.isLoading) return <Card><SkeletonRows rows={2} /></Card>
  return (
    <Card>
      <CardHeader title="Арна баптаулары" subtitle="PreferenceBased стратегиясы қолданады. Сыни хабарламаларға CriticalEscalation стратегиясы қолданылады: әрқашан қосымша ішінде және барлық қосулы арналар арқылы." />
      <div className="grid gap-4 px-5 pb-5 md:grid-cols-2">
        {local.map((p) => (
          <div key={p.channel} className="rounded-2xl border border-line p-4">
            <div className="flex items-center justify-between">
              <p className="flex items-center gap-2 font-semibold">{p.channel === 'EMAIL' ? <Mail className="size-4 text-primary" /> : <MonitorSmartphone className="size-4 text-primary" />}{p.channel === 'EMAIL' ? 'Электрондық пошта' : 'Қосымша ішінде (тікелей)'}</p>
              <label className="relative inline-flex cursor-pointer items-center">
                <input type="checkbox" className="peer sr-only" checked={p.enabled} onChange={(e) => update(p.channel, { enabled: e.target.checked })} aria-label={`${humanize(p.channel)} арнасын қосу`} />
                <span className="h-6 w-11 rounded-full bg-line transition peer-checked:bg-primary" />
                <span className="absolute left-0.5 size-5 rounded-full bg-white shadow transition peer-checked:translate-x-5" />
              </label>
            </div>
            <label className="label mt-4" htmlFor={`min-${p.channel}`}>Ең төменгі маңыздылық</label>
            <select id={`min-${p.channel}`} className="input" value={p.minSeverity} disabled={!p.enabled} onChange={(e) => update(p.channel, { minSeverity: e.target.value as Severity })}>
              {SEVERITIES.map((s) => <option key={s} value={s}>{humanize(s)}</option>)}
            </select>
          </div>
        ))}
      </div>
      <div className="flex justify-end border-t border-line px-5 py-4"><Button loading={save.isPending} onClick={() => save.mutate()}>Баптауларды сақтау</Button></div>
    </Card>
  )
}

function Channels({ channels }: { channels?: ChannelInfo[] }) {
  const qc = useQueryClient()
  const toast = useToast()
  const [severity, setSeverity] = useState<Severity>('WARNING')
  const [report, setReport] = useState<DispatchReport | null>(null)
  const send = useMutation({
    mutationFn: () => api.post<DispatchReport>('/notifications/test', { severity, title: `Сынақ хабарландыру: ${humanize(severity)}`, message: 'Арнаны таңдауды көрсету үшін хабарландыру орталығынан жіберілді.' }),
    onSuccess: (r) => { setReport(r); qc.invalidateQueries({ queryKey: ['notifications'] }) },
    onError: (e) => toast.error('Сынақ сәтсіз', errorMessage(e)),
  })
  return (
    <div className="grid gap-6 xl:grid-cols-2">
      <div className="space-y-4">
        {channels?.map((c) => (
          <Card key={c.channel} className="p-5">
            <div className="flex items-start justify-between gap-3">
              <div>
                <p className="flex items-center gap-2 font-display font-bold">{c.channel === 'EMAIL' ? <Mail className="size-4 text-primary" /> : <MonitorSmartphone className="size-4 text-primary" />}{humanize(c.channel)}</p>
                <p className="mt-1 text-xs text-muted">Abstract Factory <code className="font-semibold text-ink">{c.factory}</code> жасайтын объектілер:</p>
              </div>
              <Badge tone={c.available ? 'mint' : 'neutral'} dot>{c.available ? 'Қолжетімді' : c.availabilityReason}</Badge>
            </div>
            <div className="mt-3 flex flex-wrap gap-1.5">{c.family.map((f) => <code key={f} className="rounded-lg bg-canvas px-2 py-1 text-[11px] font-semibold">{f}</code>)}</div>
            <p className="mt-3 flex items-center gap-1.5 text-xs text-muted"><Cpu className="size-3.5" /> Factory Method creator <code className="font-semibold text-ink">{c.creator}</code> &middot; {c.retryable ? 'қайталауға болады' : 'қайталанбайды'}</p>
          </Card>
        ))}
      </div>
      <Card className="p-5">
        <p className="font-display font-bold">Өзіңізге сынақ хабарландыру жіберу</p>
        <p className="mt-1 text-sm text-muted">Қай Strategy маңыздылықты өңдегенін және әр арна не істегенін көрсетеді.</p>
        <div className="mt-4 flex flex-wrap gap-2">
          {SEVERITIES.map((s) => (
            <button key={s} onClick={() => setSeverity(s)} className={cx('rounded-xl border px-3 py-2 text-sm font-semibold transition', severity === s ? 'border-primary bg-primary-soft text-primary' : 'border-line text-muted')}>{humanize(s)}</button>
          ))}
          <Button icon={<Send className="size-4" />} loading={send.isPending} onClick={() => send.mutate()}>Жіберу</Button>
        </div>
        {report && (
          <div className="mt-5 space-y-2 rounded-2xl bg-canvas p-4 text-sm">
            <p>Strategy: <b>{report.strategy}</b></p>
            <p>Таңдалған арналар: <b>{report.selectedChannels.map(humanize).join(', ') || 'жоқ (баптауларыңыз бойынша барлығы сүзілді)'}</b></p>
            <ul className="space-y-1">{report.attempts.map((a) => <li key={a.id} className="flex flex-wrap items-center gap-2 text-xs"><StatusBadge status={a.status} /> <b>{humanize(a.channel)}</b> {a.detail}</li>)}</ul>
          </div>
        )}
      </Card>
    </div>
  )
}
