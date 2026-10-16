import { Link } from 'react-router'
import { useQuery } from '@tanstack/react-query'
import {
  Activity, ArrowRight, Bell, CalendarClock, FilePlus, FileText, Inbox, Package, PackagePlus, Plane, PlaneTakeoff,
  Radio, Stethoscope, TriangleAlert, Users, Wallet, Zap,
} from 'lucide-react'
import { api } from '../../lib/api'
import type { AppNotification, DashboardView } from '../../lib/types'
import { actorName, businessHour, compactMoney, longDate, number, relative, time, weekdayDate } from '../../lib/format'
import { label, t } from '../../lib/i18n'
import { StatCard } from '../../components/ui/StatCard'
import { Card, CardHeader } from '../../components/ui/Card'
import { ErrorState, Skeleton } from '../../components/ui/States'
import { StatusBadge, Badge } from '../../components/ui/Badge'
import { Avatar } from '../../components/ui/Avatar'
import { useAuth } from '../../auth/AuthContext'
import { AppointmentTrendChart, InventoryMovementChart, NotificationActivityChart, StatusDonut } from './DashboardCharts'
import { SeverityDot } from '../notifications/severity'
import { cx } from '../../components/ui/cx'

function greeting() {
  const h = businessHour()
  return h < 12 ? 'Қайырлы таң' : h < 18 ? 'Қайырлы күн' : 'Қайырлы кеш'
}

export function DashboardPage() {
  const { can } = useAuth()
  const q = useQuery({ queryKey: ['dashboard'], queryFn: () => api.get<DashboardView>('/dashboard'), refetchInterval: 60_000 })
  const latest = useQuery({ queryKey: ['notifications', 'latest'], queryFn: () => api.get<AppNotification[]>('/notifications/latest') })

  if (q.isError) return <Card><ErrorState error={q.error} onRetry={() => q.refetch()} /></Card>
  const d = q.data
  const staff = can('ADMIN', 'MANAGER', 'EMPLOYEE')
  const approver = can('ADMIN', 'MANAGER')

  const actions = [
    staff && { label: 'Тауар қосу', icon: <PackagePlus className="size-4" />, to: '/inventory?new=1', tone: 'bg-sky-soft text-sky' },
    { label: 'Қабылдауға жазылу', icon: <CalendarClock className="size-4" />, to: '/clinic?book=1', tone: 'bg-mint-soft text-mint' },
    { label: 'Рейстерді іздеу', icon: <PlaneTakeoff className="size-4" />, to: '/flights', tone: 'bg-primary-soft text-primary' },
    can('ADMIN', 'MANAGER', 'EMPLOYEE', 'DOCTOR') && { label: 'Құжат жасау', icon: <FilePlus className="size-4" />, to: '/documents?new=1', tone: 'bg-peach-soft text-[#C9731C]' },
    { label: 'Хабарландыруларды көру', icon: <Bell className="size-4" />, to: '/notifications', tone: 'bg-rose-soft text-[#E0537F]' },
  ].filter(Boolean) as { label: string; icon: React.ReactNode; to: string; tone: string }[]

  return (
    <div className="space-y-6">
      <section className="card relative overflow-hidden p-6 sm:p-7">
        <div className="pointer-events-none absolute -top-28 -right-20 size-80 rounded-full bg-gradient-to-br from-[#6C63FF] to-[#26C6B8] opacity-[0.14] blur-2xl" />
        <div className="pointer-events-none absolute -bottom-32 left-1/4 size-72 rounded-full bg-gradient-to-br from-[#FF7FA8] to-[#FFB86B] opacity-[0.12] blur-2xl" />
        <div className="relative flex flex-col gap-6 xl:flex-row xl:items-end xl:justify-between">
          <div>
            <p className="text-sm font-semibold text-muted">{longDate()}</p>
            <h1 className="mt-1 text-3xl font-extrabold tracking-tight sm:text-[34px]">
              {greeting()}, {d ? d.greetingName.split(' ').filter((p) => p !== 'Dr.')[0] : '...'}!
            </h1>
            <div className="mt-3 flex flex-wrap items-center gap-2 text-sm text-muted">
              <span className="inline-flex items-center gap-1.5 rounded-full bg-mint-soft px-2.5 py-1 text-xs font-semibold text-mint">
                <Radio className="size-3.5" /> Тікелей жаңарту қосулы
              </span>
              <span>Барлық модульдер жұмыс істеп тұр &middot; {d ? `сіздің шешіміңізді күтетін құжаттар: ${d.kpis.awaitingMyAction}, оқылмаған хабарландырулар: ${d.kpis.unreadNotifications}` : 'күйі жүктелуде...'}</span>
            </div>
          </div>
          <div className="grid grid-cols-2 gap-2 sm:flex sm:flex-wrap">
            {actions.map((a) => (
              <Link key={a.label} to={a.to} className="group flex items-center gap-2 rounded-xl border border-line bg-white px-3 py-2.5 text-sm font-semibold transition hover:-translate-y-0.5 hover:shadow-card">
                <span className={cx('grid size-7 place-items-center rounded-lg', a.tone)}>{a.icon}</span>
                <span className="truncate">{a.label}</span>
              </Link>
            ))}
          </div>
        </div>
      </section>

      {!d ? (
        <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">{Array.from({ length: 8 }).map((_, i) => <Skeleton key={i} className="h-32" />)}</div>
      ) : (
        <>
          <div className="stagger grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
            {staff && d.kpis.products !== null && (
              <>
                <StatCard accent="sky" icon={<Package className="size-5" />} label="Тауарлар" value={number(d.kpis.products)} hint="Қоймадағы белсенді SKU" to="/inventory" />
                <StatCard accent="teal" icon={<Wallet className="size-5" />} label="Қор құны" value={compactMoney(d.kpis.stockValue)} hint="Сатып алу бағасымен" to="/inventory" />
                <StatCard accent="peach" icon={<TriangleAlert className="size-5" />} label="Қор аз" value={(d.kpis.lowStock ?? 0) + (d.kpis.outOfStock ?? 0)}
                  hint={`Таусылғаны: ${d.kpis.outOfStock}`} to="/inventory?tab=alerts" />
              </>
            )}
            <StatCard accent="mint" icon={<Stethoscope className="size-5" />} label="Бүгінгі қабылдаулар" value={d.kpis.appointmentsToday} hint={`Алдағы: ${d.kpis.upcomingAppointments}, дәрігерлер: ${d.kpis.doctors}`} to="/clinic" />
            <StatCard accent="violet" icon={<Plane className="size-5" />} label="Брондауға болатын рейстер" value={d.kpis.bookableFlights} hint={`Брондаулар саны: ${d.kpis.bookings}`} to="/flights" />
            <StatCard accent="peach" icon={<FileText className="size-5" />} label="Бекітуде" value={d.kpis.documentsInApproval} hint="Күтуде немесе қаралуда" to="/documents?status=PENDING_APPROVAL" />
            {approver && <StatCard accent="violet" icon={<Inbox className="size-5" />} label="Менің шешімімді күтуде" value={d.kpis.awaitingMyAction} hint="Сіздің рөліңізге берілген қадамдар" to="/documents?tab=inbox" />}
            <StatCard accent="rose" icon={<Bell className="size-5" />} label="Оқылмаған хабарландырулар" value={d.kpis.unreadNotifications} hint="Барлық арналар бойынша" to="/notifications" />
          </div>

          <div className="grid gap-4 xl:grid-cols-3">
            {staff && <InventoryMovementChart data={d.inventoryMovement} />}
            <StatusDonut title="Әуе билеттерін брондау" subtitle="Күйлер бойынша бөлінуі" data={d.bookingDistribution} />
            {!staff && <AppointmentTrendChart data={d.appointmentTrend} />}
            {!staff && <StatusDonut title="Құжат айналымы" subtitle="Құжаттар күйлері бойынша" data={d.documentStatus} />}
          </div>
          {staff && (
            <div className="grid gap-4 xl:grid-cols-3">
              <AppointmentTrendChart data={d.appointmentTrend} />
              <StatusDonut title="Құжат айналымы" subtitle="Құжаттар күйлері бойынша" data={d.documentStatus} />
              <NotificationActivityChart data={d.notificationActivity} />
            </div>
          )}

          <div className="grid gap-4 xl:grid-cols-3">
            <Card>
              <CardHeader title="Назар аударыңыз" subtitle="Сізге арналған ескертулер мен бекітулер" icon={<span className="grid size-9 place-items-center rounded-xl bg-peach-soft text-[#C9731C]"><Zap className="size-4" /></span>} />
              <ul className="space-y-2 px-5 pb-5">
                {d.attention.length === 0 && <li className="rounded-xl bg-canvas px-4 py-6 text-center text-sm text-muted">Қазір назар аударуды қажет ететін ештеңе жоқ.</li>}
                {d.attention.map((a, i) => (
                  <li key={i}>
                    <Link to={a.link} className="flex items-start gap-3 rounded-xl border border-line px-3.5 py-3 transition hover:border-primary/30 hover:bg-primary-soft/30">
                      <SeverityDot severity={a.severity as never} />
                      <div className="min-w-0 flex-1">
                        <p className="truncate text-sm font-semibold">{a.title}</p>
                        <p className="truncate text-xs text-muted">{a.detail}</p>
                      </div>
                      <ArrowRight className="mt-1 size-4 text-muted" />
                    </Link>
                  </li>
                ))}
              </ul>
            </Card>

            <Card>
              <CardHeader title="Алдағы қабылдаулар" subtitle="Жақын арадағы расталған және жоспарланған қабылдаулар"
                icon={<span className="grid size-9 place-items-center rounded-xl bg-mint-soft text-mint"><CalendarClock className="size-4" /></span>}
                action={<Link to="/clinic" className="text-xs font-semibold text-primary hover:underline">Емханаға өту</Link>} />
              <ul className="divide-y divide-line/70 px-5 pb-3">
                {d.upcomingAppointments.length === 0 && <li className="py-6 text-center text-sm text-muted">Алдағы қабылдаулар жоқ.</li>}
                {d.upcomingAppointments.map((a) => (
                  <li key={a.id} className="flex items-center gap-3 py-2.5">
                    <Avatar name={a.patient} size="sm" />
                    <div className="min-w-0 flex-1">
                      <p className="truncate text-sm font-semibold">{a.patient}</p>
                      <p className="truncate text-xs text-muted">{a.doctor} &middot; {a.specialty}</p>
                    </div>
                    <div className="text-right">
                      <p className="text-xs font-semibold">{weekdayDate(a.start)}</p>
                      <p className="text-[11px] text-muted">{time(a.start)}</p>
                    </div>
                  </li>
                ))}
              </ul>
            </Card>

            <Card>
              <CardHeader title="Соңғы хабарландырулар" subtitle="Сіздің кіріс жәшігіңіз"
                icon={<span className="grid size-9 place-items-center rounded-xl bg-rose-soft text-[#E0537F]"><Bell className="size-4" /></span>}
                action={<Link to="/notifications" className="text-xs font-semibold text-primary hover:underline">Барлығы</Link>} />
              <ul className="divide-y divide-line/70 px-5 pb-3">
                {latest.data?.length === 0 && <li className="py-6 text-center text-sm text-muted">Әзірге хабарландыру жоқ.</li>}
                {latest.data?.map((n) => (
                  <li key={n.id} className="flex gap-3 py-2.5">
                    <SeverityDot severity={n.severity} />
                    <div className="min-w-0 flex-1">
                      <p className={cx('truncate text-sm', n.read ? 'text-muted' : 'font-semibold')}>{n.title}</p>
                      <p className="text-[11px] text-muted">{relative(n.createdAt)}</p>
                    </div>
                  </li>
                ))}
              </ul>
            </Card>
          </div>

          {approver && (
            <div className="grid gap-4 lg:grid-cols-2">
              <ActivityList title="Соңғы жүйелік оқиғалар" subtitle="Журналға жазылған Observer оқиғалары" items={d.recentEvents} icon={<Activity className="size-4" />} />
              <ActivityList title="Пайдаланушылардың соңғы әрекеттері" subtitle="Invoker арқылы орындалған командалар" items={d.recentActions} icon={<Users className="size-4" />} />
            </div>
          )}
        </>
      )}
    </div>
  )
}

function ActivityList({ title, subtitle, items, icon }: { title: string; subtitle: string; items: DashboardView['recentEvents']; icon: React.ReactNode }) {
  return (
    <Card>
      <CardHeader title={title} subtitle={subtitle} icon={<span className="grid size-9 place-items-center rounded-xl bg-primary-soft text-primary">{icon}</span>}
        action={<Link to="/audit" className="text-xs font-semibold text-primary hover:underline">{t('nav.audit')}</Link>} />
      <ol className="relative mx-5 mb-5 space-y-3 border-l border-line pl-5">
        {items.length === 0 && <li className="text-sm text-muted">Әзірге әрекет жоқ.</li>}
        {items.map((a) => (
          <li key={a.id} className="relative">
            <span className={cx('absolute top-1.5 -left-[25px] size-2.5 rounded-full ring-4 ring-white', a.outcome === 'FAILURE' ? 'bg-danger' : 'bg-primary')} />
            <div className="flex flex-wrap items-center gap-2">
              <span className="text-sm font-semibold">{label(a.action)}</span>
              {a.outcome === 'FAILURE' ? <StatusBadge status="FAILURE" /> : <Badge tone="neutral">{label(a.entityType)}</Badge>}
            </div>
            <p className="truncate text-xs text-muted">{a.details} &middot; {actorName(a.actor)} &middot; {relative(a.at)}</p>
          </li>
        ))}
      </ol>
    </Card>
  )
}
