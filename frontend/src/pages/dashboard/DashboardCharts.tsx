import { Area, AreaChart, Bar, BarChart, CartesianGrid, Cell, Pie, PieChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { ChartCard, Legend } from '../../components/charts/ChartCard'
import { ChartTooltip } from '../../components/charts/ChartTooltip'
import { AXIS, GRID, SERIES, SEVERITY_COLORS, STATUS_COLORS } from '../../components/charts/palette'
import type { DashboardView } from '../../lib/types'
import { humanize, shortDay } from '../../lib/format'

const tooltipLabel = (l: string) => (/^\d{4}-\d{2}-\d{2}$/.test(l) ? shortDay(l) : l)

export function InventoryMovementChart({ data }: { data: DashboardView['inventoryMovement'] }) {
  const empty = data.every((d) => d.inbound === 0 && d.outbound === 0)
  return (
    <ChartCard title="Қор қозғалысы" subtitle="Кіріс пен шығыс (бірлік), соңғы 14 күн" empty={empty} className="xl:col-span-2"
      rows={data} columns={[{ key: 'date', label: 'Күні', format: (v) => shortDay(String(v)) }, { key: 'inbound', label: 'Кіріс' }, { key: 'outbound', label: 'Шығыс' }]}>
      <Legend items={[{ label: 'Кіріс', color: SERIES[0] }, { label: 'Шығыс', color: SERIES[1] }]} />
      <ResponsiveContainer width="100%" height="88%">
        <AreaChart data={data} margin={{ top: 8, right: 8, left: -12, bottom: 0 }}>
          <defs>
            <linearGradient id="inbound" x1="0" y1="0" x2="0" y2="1">
              <stop offset="0" stopColor={SERIES[0]} stopOpacity={0.22} />
              <stop offset="1" stopColor={SERIES[0]} stopOpacity={0} />
            </linearGradient>
            <linearGradient id="outbound" x1="0" y1="0" x2="0" y2="1">
              <stop offset="0" stopColor={SERIES[1]} stopOpacity={0.18} />
              <stop offset="1" stopColor={SERIES[1]} stopOpacity={0} />
            </linearGradient>
          </defs>
          <CartesianGrid stroke={GRID} vertical={false} />
          <XAxis dataKey="date" tickFormatter={shortDay} {...AXIS} tickLine={false} axisLine={false} minTickGap={24} />
          <YAxis {...AXIS} tickLine={false} axisLine={false} width={44} />
          <Tooltip content={<ChartTooltip labelFormatter={tooltipLabel} />} cursor={{ stroke: '#C9CDDD', strokeDasharray: '3 3' }} />
          <Area type="monotone" dataKey="inbound" name="Кіріс" stroke={SERIES[0]} strokeWidth={2} fill="url(#inbound)" activeDot={{ r: 4, strokeWidth: 2, stroke: '#fff' }} />
          <Area type="monotone" dataKey="outbound" name="Шығыс" stroke={SERIES[1]} strokeWidth={2} fill="url(#outbound)" activeDot={{ r: 4, strokeWidth: 2, stroke: '#fff' }} />
        </AreaChart>
      </ResponsiveContainer>
    </ChartCard>
  )
}

export function StatusDonut({ title, subtitle, data }: { title: string; subtitle: string; data: { name: string; value: number }[] }) {
  const total = data.reduce((s, d) => s + d.value, 0)
  return (
    <ChartCard title={title} subtitle={subtitle} empty={total === 0} rows={data.map((d) => ({ ...d, name: humanize(d.name) }))}
      columns={[{ key: 'name', label: 'Күйі' }, { key: 'value', label: 'Саны' }]}>
      <div className="flex h-full flex-col items-center gap-3">
        <div className="relative h-36 w-36 shrink-0">
          <ResponsiveContainer width="100%" height="100%">
            <PieChart>
              <Pie data={data} dataKey="value" nameKey="name" innerRadius="66%" outerRadius="100%" paddingAngle={2} stroke="#fff" strokeWidth={2}>
                {data.map((d) => <Cell key={d.name} fill={STATUS_COLORS[d.name] ?? SERIES[3]} />)}
              </Pie>
              <Tooltip content={<ChartTooltip nameFormatter={humanize} />} />
            </PieChart>
          </ResponsiveContainer>
          <div className="pointer-events-none absolute inset-0 grid place-items-center text-center">
            <div><p className="font-display text-2xl font-extrabold">{total}</p><p className="text-[11px] text-muted">барлығы</p></div>
          </div>
        </div>
        <ul className="grid w-full grid-cols-1 gap-x-4 gap-y-1 text-sm sm:grid-cols-2">
          {data.map((d) => (
            <li key={d.name} className="flex items-center gap-2">
              <span className="size-2.5 rounded-sm" style={{ background: STATUS_COLORS[d.name] ?? SERIES[3] }} />
              <span className="min-w-0 flex-1 truncate text-muted" title={humanize(d.name)}>{humanize(d.name)}</span>
              <span className="font-semibold tabular-nums">{d.value}</span>
            </li>
          ))}
        </ul>
      </div>
    </ChartCard>
  )
}

export function AppointmentTrendChart({ data }: { data: DashboardView['appointmentTrend'] }) {
  const empty = data.every((d) => d.count === 0)
  return (
    <ChartCard title="Емхана қабылдаулары" subtitle="Өткен 7 күн және алдағы апта" empty={empty} rows={data}
      columns={[{ key: 'date', label: 'Күні', format: (v) => shortDay(String(v)) }, { key: 'count', label: 'Қабылдаулар' }]}>
      <ResponsiveContainer width="100%" height="100%">
        <BarChart data={data} margin={{ top: 8, right: 8, left: -18, bottom: 0 }} barCategoryGap="28%">
          <CartesianGrid stroke={GRID} vertical={false} />
          <XAxis dataKey="date" tickFormatter={shortDay} {...AXIS} tickLine={false} axisLine={false} minTickGap={16} />
          <YAxis {...AXIS} allowDecimals={false} tickLine={false} axisLine={false} width={40} />
          <Tooltip content={<ChartTooltip labelFormatter={tooltipLabel} />} cursor={{ fill: 'rgba(108,99,255,0.06)' }} />
          <Bar dataKey="count" name="Қабылдаулар" fill={SERIES[2]} radius={[4, 4, 0, 0]} maxBarSize={22} />
        </BarChart>
      </ResponsiveContainer>
    </ChartCard>
  )
}

export function NotificationActivityChart({ data }: { data: DashboardView['notificationActivity'] }) {
  const empty = data.every((d) => d.info + d.success + d.warning + d.critical === 0)
  const series = [
    { key: 'info', label: 'Ақпарат', color: SEVERITY_COLORS.info },
    { key: 'success', label: 'Сәтті', color: SEVERITY_COLORS.success },
    { key: 'warning', label: 'Ескерту', color: SEVERITY_COLORS.warning },
    { key: 'critical', label: 'Сыни', color: SEVERITY_COLORS.critical },
  ]
  return (
    <ChartCard title="Хабарландыру белсенділігі" subtitle="Бүкіл платформа бойынша, маңыздылығы бойынша, 14 күн" empty={empty} rows={data}
      columns={[{ key: 'date', label: 'Күні', format: (v) => shortDay(String(v)) }, ...series.map((s) => ({ key: s.key, label: s.label }))]}>
      <Legend items={series} />
      <ResponsiveContainer width="100%" height="88%">
        <BarChart data={data} margin={{ top: 8, right: 8, left: -18, bottom: 0 }} barCategoryGap="30%">
          <CartesianGrid stroke={GRID} vertical={false} />
          <XAxis dataKey="date" tickFormatter={shortDay} {...AXIS} tickLine={false} axisLine={false} minTickGap={20} />
          <YAxis {...AXIS} allowDecimals={false} tickLine={false} axisLine={false} width={40} />
          <Tooltip content={<ChartTooltip labelFormatter={tooltipLabel} />} cursor={{ fill: 'rgba(108,99,255,0.06)' }} />
          {series.map((s, i) => (
            <Bar key={s.key} dataKey={s.key} name={s.label} stackId="sev" fill={s.color} stroke="#fff" strokeWidth={1}
              radius={i === series.length - 1 ? [4, 4, 0, 0] : 0} maxBarSize={22} />
          ))}
        </BarChart>
      </ResponsiveContainer>
    </ChartCard>
  )
}
