import { useState } from 'react'
import { Link } from 'react-router'
import { useMutation } from '@tanstack/react-query'
import { ArrowUpRight, FlaskConical, Play, Sparkles } from 'lucide-react'
import { api, errorMessage } from '../../lib/api'
import { isoDate } from '../../lib/format'
import { PageHeader } from '../../components/ui/PageHeader'
import { Card } from '../../components/ui/Card'
import { Button } from '../../components/ui/Button'
import { Badge } from '../../components/ui/Badge'
import { accentStyles } from '../../components/ui/accents'
import { cx } from '../../components/ui/cx'
import { GROUP_LABEL, PATTERNS, type PatternInfo } from './patterns'
import { t } from '../../lib/i18n'

async function runDemo(demo: string): Promise<unknown> {
  switch (demo) {
    case 'singleton': return api.get('/lab/singleton')
    case 'channels': return api.get('/notifications/channels')
    case 'low-stock': return api.post('/lab/low-stock')
    case 'booking-lifecycle': return api.post('/lab/booking-lifecycle')
    case 'flight-strategies': return api.get('/lab/flight-strategies', { from: 'ALA', to: 'NQZ', date: isoDate(2) })
    case 'workflow-submit': return api.post('/lab/workflow-submit')
    case 'my-commands': return api.get('/lab/my-commands')
    case 'chain': {
      const ok = await api.post('/workflows/validate', { type: 'PURCHASE_REQUEST', title: 'Серверлік бөлмеге арналған серверлер', content: 'Жаңа серверлік бөлмеге төрт сөрелік сервер, коммерциялық ұсыныс қоса берілген.', amount: '2400000.00' })
      const bad = await api.post('/workflows/validate', { type: 'PURCHASE_REQUEST', title: 'Сомасыз өтінім', content: 'Бұл сатып алу өтінімінде сома жоқ, сондықтан тізбек тоқтауы керек.' })
      return { highValueRequest: ok, requestWithoutAmount: bad }
    }
    default: throw new Error('Белгісіз демо')
  }
}

export function PatternsLabPage() {
  const [filter, setFilter] = useState<'All' | PatternInfo['group']>('All')
  const list = PATTERNS.filter((p) => filter === 'All' || p.group === filter)
  return (
    <div className="space-y-6">
      <PageHeader accent="teal" icon={<FlaskConical className="size-6" />} title={t('nav.lab')}
        description="Нақты бизнес-логикада қолданылатын он GoF паттерні. Әр батырма серверге сұраныс жіберіп, нақты кластарды іске қосады — бұл бетте ештеңе имитацияланбайды."
        stats={[{ label: 'Паттерндер', value: 10 }, { label: GROUP_LABEL.Creational, value: 3 }, { label: GROUP_LABEL.Structural, value: 1 }, { label: GROUP_LABEL.Behavioral, value: 6 }]} />
      <div className="flex flex-wrap gap-2">
        {(['All', 'Creational', 'Structural', 'Behavioral'] as const).map((g) => (
          <button key={g} onClick={() => setFilter(g)} className={cx('rounded-xl border px-3.5 py-2 text-sm font-semibold transition', filter === g ? 'border-primary bg-primary text-white' : 'border-line bg-white text-muted hover:text-ink')}>{GROUP_LABEL[g]}</button>
        ))}
      </div>
      <div className="grid gap-5 xl:grid-cols-2">
        {list.map((p, i) => <PatternCard key={p.id} pattern={p} index={PATTERNS.indexOf(p) + 1} delay={i} />)}
      </div>
    </div>
  )
}

function PatternCard({ pattern: p, index, delay }: { pattern: PatternInfo; index: number; delay: number }) {
  const a = accentStyles[p.accent]
  const run = useMutation({ mutationFn: () => runDemo(p.demo) })
  return (
    <Card className="flex animate-rise flex-col overflow-hidden" style={{ animationDelay: `${delay * 40}ms` }}>
      <div className="flex items-start gap-4 p-5">
        <span className={cx('grid size-11 shrink-0 place-items-center rounded-2xl bg-gradient-to-br text-white', a.gradient)}>{p.icon}</span>
        <div className="min-w-0 flex-1">
          <div className="flex flex-wrap items-center gap-2">
            <span className="text-xs font-bold text-muted">#{index}</span>
            <h2 className="font-display text-lg font-bold">{p.name}</h2>
            <Badge tone="neutral">{GROUP_LABEL[p.group]}</Badge>
          </div>
          <p className="mt-1 text-sm text-muted">{p.summary}</p>
        </div>
      </div>
      <div className="space-y-3 px-5">
        <div className="rounded-xl bg-canvas px-4 py-3 text-sm"><p className="label mb-1 flex items-center gap-1"><Sparkles className="size-3" />Сценарий</p>{p.scenario}</div>
        <div className="flex flex-wrap gap-1.5">{p.classes.map((c) => <code key={c} className={cx('rounded-lg px-2 py-1 text-[11px] font-semibold', a.soft, a.text)}>{c}</code>)}</div>
      </div>
      <div className="mt-4 flex flex-wrap items-center gap-2 border-t border-line px-5 py-3">
        <Button size="sm" icon={<Play className="size-3.5" />} loading={run.isPending} onClick={() => run.mutate()}>{p.demoLabel}</Button>
        {p.link && <Link to={p.link.to} className="inline-flex items-center gap-1 text-xs font-semibold text-primary hover:underline">{p.link.label}<ArrowUpRight className="size-3.5" /></Link>}
        {p.roles && <span className="ml-auto text-[11px] text-muted">қажетті рөл: {p.roles}</span>}
      </div>
      {(run.data !== undefined || run.isError) && (
        <div className="border-t border-line bg-[#1E2236] px-5 py-4">
          {run.isError ? <p className="text-sm font-semibold text-[#FF9DB8]">{errorMessage(run.error)}</p> : <ResultView data={run.data} />}
        </div>
      )}
    </Card>
  )
}

function ResultView({ data }: { data: unknown }) {
  const obj = data as Record<string, unknown>
  const lines: string[] = []
  const collect = (key: string) => { if (Array.isArray(obj?.[key])) (obj[key] as unknown[]).forEach((l) => lines.push(String(l))) }
  ;['steps', 'coordinationLog', 'createLog', 'cancelLog'].forEach(collect)
  return (
    <div className="space-y-3">
      {lines.length > 0 && (
        <ol className="space-y-1 font-mono text-[11px] text-[#C9D1FF]">
          {lines.map((l, i) => <li key={i} className={l.startsWith('Mediator') ? 'text-[#7CE0D3]' : ''}>{l}</li>)}
        </ol>
      )}
      <details open={lines.length === 0}>
        <summary className="cursor-pointer text-[11px] font-semibold text-[#9AA3C7]">Сервердің бастапқы жауабы</summary>
        <pre className="mt-2 max-h-72 overflow-auto font-mono text-[11px] whitespace-pre-wrap text-[#E6E9FF]">{JSON.stringify(data, null, 2)}</pre>
      </details>
    </div>
  )
}
