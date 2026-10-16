import { Link } from 'react-router'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Cog, Settings, ShieldCheck, UserRound, Users } from 'lucide-react'
import { api, errorMessage } from '../lib/api'
import type { AdminUser, Role } from '../lib/types'
import { dateOnly, humanize } from '../lib/format'
import { PageHeader } from '../components/ui/PageHeader'
import { Tabs } from '../components/ui/Tabs'
import { Card, CardHeader } from '../components/ui/Card'
import { Badge } from '../components/ui/Badge'
import { Avatar } from '../components/ui/Avatar'
import { Button } from '../components/ui/Button'
import { Table, Td, Th, Tr } from '../components/ui/Table'
import { SkeletonRows, ErrorState } from '../components/ui/States'
import { useToast } from '../components/ui/Toast'
import { useTab } from '../hooks/useTab'
import { useAuth } from '../auth/AuthContext'
import { label, t } from '../lib/i18n'

const ROLES: Role[] = ['ADMIN', 'MANAGER', 'DOCTOR', 'EMPLOYEE', 'USER']

export function SettingsPage() {
  const { user, can } = useAuth()
  const [tab, setTab] = useTab('profile')
  return (
    <div className="space-y-6">
      <PageHeader accent="violet" icon={<Settings className="size-6" />} title={t('nav.settings')} description="Профиль, хабарландыру баптаулары, платформа конфигурациясы және (әкімшілер үшін) пайдаланушыларды басқару." />
      <Tabs value={tab} onChange={setTab} items={[
        { key: 'profile', label: 'Профиль', icon: <UserRound className="size-4" /> },
        { key: 'platform', label: 'Платформа', icon: <Cog className="size-4" /> },
        ...(can('ADMIN') ? [{ key: 'users', label: 'Пайдаланушылар', icon: <Users className="size-4" /> }] : []),
      ]} />
      {tab === 'profile' && user && (
        <Card className="p-6">
          <div className="flex flex-col gap-5 sm:flex-row sm:items-center">
            <Avatar name={user.fullName} size="lg" />
            <div className="min-w-0 flex-1">
              <p className="font-display text-xl font-bold">{user.fullName}</p>
              <p className="text-sm text-muted">{user.title} &middot; {user.email}</p>
              <div className="mt-2 flex gap-2"><Badge tone="violet">{label(user.role)}</Badge><Badge>@{user.username}</Badge></div>
            </div>
            <Link to="/notifications?tab=preferences"><Button variant="secondary">Хабарландыру баптаулары</Button></Link>
          </div>
          <div className="mt-6 rounded-2xl bg-canvas p-4 text-sm text-muted">
            <p className="flex items-center gap-2 font-semibold text-ink"><ShieldCheck className="size-4 text-primary" /> Сеанс қауіпсіздігі</p>
            <p className="mt-1">Сіз HttpOnly сеанс cookie арқылы кірдіңіз. Деректерді өзгертетін сұраныстар CSRF белгісімен жіберіледі. Интерфейс тілі: қазақ тілі.</p>
          </div>
        </Card>
      )}
      {tab === 'platform' && <PlatformSettings />}
      {tab === 'users' && can('ADMIN') && <UsersAdmin />}
    </div>
  )
}

function PlatformSettings() {
  const settings = useQuery({ queryKey: ['platform-settings'], queryFn: () => api.get<Record<string, unknown>>('/dashboard/settings') })
  if (settings.isLoading) return <Card><SkeletonRows /></Card>
  if (settings.isError) return <Card><ErrorState error={settings.error} /></Card>
  return (
    <Card>
      <CardHeader title="Платформа конфигурациясы" subtitle="Мәндерді PlatformSettings singleton береді — қолданба контекстіне бір ортақ, өзгермейтін экземпляр (мұнда тек оқуға; орта айнымалылары арқылы бапталады)." />
      <dl className="grid gap-px border-t border-line bg-line sm:grid-cols-2">
        {Object.entries(settings.data!).map(([k, v]) => (
          <div key={k} className="bg-white px-5 py-3">
            <dt className="text-[11px] font-semibold tracking-wide text-muted uppercase">{humanize(k)}</dt>
            <dd className="mt-0.5 font-mono text-sm break-all">{typeof v === 'boolean' ? (v ? 'иә' : 'жоқ') : k === 'lowStockPolicy' ? humanize(String(v)) : String(v)}</dd>
          </div>
        ))}
      </dl>
    </Card>
  )
}

function UsersAdmin() {
  const { user } = useAuth()
  const qc = useQueryClient()
  const toast = useToast()
  const users = useQuery({ queryKey: ['users'], queryFn: () => api.get<AdminUser[]>('/users') })
  const update = useMutation({
    mutationFn: (u: { id: number; role: Role; active: boolean }) => api.put<AdminUser>(`/users/${u.id}`, { role: u.role, active: u.active }),
    onSuccess: (u) => { toast.success('Пайдаланушы жаңартылды', `${u.fullName}: ${label(u.role)}${u.active ? '' : ' (бұғатталған)'}`); qc.invalidateQueries({ queryKey: ['users'] }) },
    onError: (e) => toast.error('Өзгеріс қабылданбады', errorMessage(e)),
  })
  if (users.isLoading) return <Card><SkeletonRows /></Card>
  if (users.isError) return <Card><ErrorState error={users.error} /></Card>
  return (
    <Card>
      <CardHeader title="Пайдаланушылар мен рөлдер" subtitle="Рөл өзгерісі пайдаланушы келесі рет кіргенде күшіне енеді. Өзіңізді бұғаттай немесе рөліңізді өзгерте алмайсыз." />
      <Table minWidth={760}>
        <thead><tr><Th>Пайдаланушы</Th><Th>Рөлі</Th><Th>Күйі</Th><Th>Тіркелген күні</Th><Th className="text-right">Белсенді</Th></tr></thead>
        <tbody>
          {users.data!.map((u) => (
            <Tr key={u.id}>
              <Td><div className="flex items-center gap-3"><Avatar name={u.fullName} size="sm" /><div><p className="font-semibold">{u.fullName}</p><p className="text-xs text-muted">@{u.username} &middot; {u.email}</p></div></div></Td>
              <Td>
                <select className="input h-9 w-36 py-1" value={u.role} disabled={u.id === user?.id} aria-label={`${u.fullName} рөлі`}
                  onChange={(e) => update.mutate({ id: u.id, role: e.target.value as Role, active: u.active })}>
                  {ROLES.map((r) => <option key={r} value={r}>{label(r)}</option>)}
                </select>
              </Td>
              <Td>{u.active ? <Badge tone="mint" dot>Белсенді</Badge> : <Badge dot>Бұғатталған</Badge>}</Td>
              <Td className="text-muted">{dateOnly(u.createdAt)}</Td>
              <Td className="text-right">
                <Button size="sm" variant={u.active ? 'secondary' : 'soft'} disabled={u.id === user?.id}
                  onClick={() => update.mutate({ id: u.id, role: u.role, active: !u.active })}>{u.active ? 'Бұғаттау' : 'Белсендіру'}</Button>
              </Td>
            </Tr>
          ))}
        </tbody>
      </Table>
    </Card>
  )
}
