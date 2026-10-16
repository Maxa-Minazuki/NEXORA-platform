import { useState } from 'react'
import { Link, useParams } from 'react-router'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ArrowLeft, CircleCheck, CircleX, FileText, MessageSquare, Network, Pencil, RotateCcw, Send } from 'lucide-react'
import { api, errorMessage } from '../../lib/api'
import type { DocumentDetail, WorkflowOutcome } from '../../lib/types'
import { dateTime, humanize, money, relative } from '../../lib/format'
import { label } from '../../lib/i18n'
import { Card, CardHeader } from '../../components/ui/Card'
import { Button } from '../../components/ui/Button'
import { Badge, StatusBadge } from '../../components/ui/Badge'
import { Avatar } from '../../components/ui/Avatar'
import { Modal } from '../../components/ui/Modal'
import { Field } from '../../components/ui/Field'
import { ErrorState, Spinner } from '../../components/ui/States'
import { useToast } from '../../components/ui/Toast'
import { WorkflowTrack } from './WorkflowTrack'

type Decision = 'approve' | 'reject' | 'return' | null

export function DocumentDetailPage() {
  const { id } = useParams()
  const qc = useQueryClient()
  const toast = useToast()
  const [decision, setDecision] = useState<Decision>(null)
  const [comment, setComment] = useState('')
  const [editing, setEditing] = useState(false)
  const [draft, setDraft] = useState({ title: '', content: '', amount: '' })
  const [newComment, setNewComment] = useState('')
  const [log, setLog] = useState<string[]>([])
  const doc = useQuery({ queryKey: ['documents', 'detail', id], queryFn: () => api.get<DocumentDetail>(`/documents/${id}`) })

  const refresh = (d: DocumentDetail) => {
    qc.setQueryData(['documents', 'detail', id], d)
    qc.invalidateQueries({ queryKey: ['documents'] })
    qc.invalidateQueries({ queryKey: ['dashboard'] })
  }
  const onOutcome = (title: string) => (o: WorkflowOutcome) => {
    refresh(o.document)
    setLog(o.coordinationLog)
    setDecision(null)
    toast.success(title, `${o.document.summary.number}: ${humanize(o.document.summary.status)}`)
  }
  const fail = (e: unknown) => toast.error('Сервер әрекетті қабылдамады', errorMessage(e))

  const submit = useMutation({ mutationFn: () => api.post<WorkflowOutcome>(`/documents/${id}/submit`), onSuccess: onOutcome('Жіберілді'), onError: fail })
  const decide = useMutation({
    mutationFn: (d: Exclude<Decision, null>) => d === 'approve'
      ? api.post<WorkflowOutcome>(`/workflows/${id}/approve`, { comment: comment.trim() || null })
      : d === 'reject' ? api.post<WorkflowOutcome>(`/workflows/${id}/reject`, { reason: comment.trim() })
        : api.post<WorkflowOutcome>(`/workflows/${id}/return`, { comment: comment.trim() }),
    onSuccess: (o, d) => onOutcome(d === 'approve' ? 'Бекітілді' : d === 'reject' ? 'Қабылданбады' : 'Пысықтауға қайтарылды')(o),
    onError: fail,
  })
  const save = useMutation({
    mutationFn: () => api.put<DocumentDetail>(`/documents/${id}`, { title: draft.title.trim(), content: draft.content.trim(), amount: draft.amount || null }),
    onSuccess: (d) => { refresh(d); setEditing(false); toast.success('Қаралама сақталды', `Revision ${d.revision}`) },
    onError: fail,
  })
  const addComment = useMutation({
    mutationFn: () => api.post<DocumentDetail>(`/documents/${id}/comments`, { body: newComment.trim() }),
    onSuccess: (d) => { refresh(d); setNewComment('') },
    onError: fail,
  })

  if (doc.isLoading) return <Spinner />
  if (doc.isError) return <Card><ErrorState error={doc.error} onRetry={() => doc.refetch()} /></Card>
  const d = doc.data!
  const s = d.summary
  const can = (a: string) => d.allowedActions.includes(a)
  const needsText = decision === 'reject' || decision === 'return'

  return (
    <div className="space-y-6">
      <Link to="/documents" className="inline-flex items-center gap-1.5 text-sm font-semibold text-muted hover:text-primary"><ArrowLeft className="size-4" /> Құжаттарға оралу</Link>
      <section className="card relative overflow-hidden p-5 sm:p-6">
        <div className="pointer-events-none absolute -top-24 -right-16 size-72 rounded-full bg-gradient-to-br from-[#FFB86B] to-[#6C63FF] opacity-[0.13] blur-2xl" />
        <div className="relative flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
          <div className="min-w-0">
            <div className="flex flex-wrap items-center gap-2"><span className="text-xs font-bold text-muted">{s.number}</span><Badge tone="peach">{s.typeLabel}</Badge><StatusBadge status={s.status} /></div>
            <h1 className="mt-2 text-2xl font-extrabold tracking-tight">{s.title}</h1>
            <p className="mt-1 text-sm text-muted">Авторы: {s.authorName} &middot; жасалды {dateTime(s.createdAt)} &middot; {d.revision}-нұсқа &middot; {d.approvalRound}-кезең
              {s.amount !== null && <> &middot; <b className="text-ink">{money(s.amount)}</b></>}</p>
          </div>
          <div className="flex flex-wrap gap-2">
            {can('edit') && <Button variant="secondary" icon={<Pencil className="size-4" />} onClick={() => { setDraft({ title: s.title, content: d.content, amount: s.amount?.toString() ?? '' }); setEditing(true) }}>Қараламаны өңдеу</Button>}
            {can('submit') && <Button icon={<Send className="size-4" />} loading={submit.isPending} onClick={() => submit.mutate()}>Бекітуге жіберу</Button>}
            {can('return') && <Button variant="secondary" icon={<RotateCcw className="size-4" />} onClick={() => { setComment(''); setDecision('return') }}>Қайтару</Button>}
            {can('reject') && <Button variant="danger" icon={<CircleX className="size-4" />} onClick={() => { setComment(''); setDecision('reject') }}>Қабылдамау</Button>}
            {can('approve') && <Button variant="success" icon={<CircleCheck className="size-4" />} onClick={() => { setComment(''); setDecision('approve') }}>Бекіту</Button>}
          </div>
        </div>
        {!d.canDecide && (s.status === 'PENDING_APPROVAL' || s.status === 'IN_REVIEW') && (
          <p className="relative mt-4 rounded-xl bg-canvas px-3 py-2 text-xs text-muted">
            <b>{s.currentStep}</b> қадамы бойынша шешімді {label(s.currentApproverRole)} қабылдауы керек. Автор өз құжатын бекіте алмайды — бұл серверде тексеріледі.
          </p>
        )}
        {d.rejectionReason && <p className="relative mt-4 rounded-xl bg-danger-soft px-3 py-2 text-sm text-danger"><b>Қабылданбады:</b> {d.rejectionReason}</p>}
      </section>

      <Card className="p-5 sm:p-6">
        <p className="label mb-4">Бекіту маршруты</p>
        <WorkflowTrack items={d.track} />
        {d.plannedRoute.length > 0 && <p className="mt-4 text-xs text-muted">Маршрутты Chain of Responsibility жоспарлады (қазіргі қаралама бойынша сынақ іске қосу).</p>}
      </Card>

      {log.length > 0 && (
        <Card className="p-5">
          <p className="flex items-center gap-2 text-sm font-bold"><Network className="size-4 text-primary" /> WorkflowMediator үйлестіру журналы</p>
          <ol className="mt-3 space-y-1 font-mono text-[11px] text-muted">
            {log.map((l, i) => <li key={i} className={l.startsWith('Mediator') ? 'text-primary' : ''}>{l}</li>)}
          </ol>
        </Card>
      )}

      <div className="grid gap-6 xl:grid-cols-[1.5fr_1fr]">
        <div className="space-y-6">
          <Card>
            <CardHeader title="Мазмұны" icon={<span className="grid size-9 place-items-center rounded-xl bg-peach-soft text-[#C9731C]"><FileText className="size-4" /></span>}
              subtitle={d.relatedEntityType ? `Байланысты нысан: ${humanize(d.relatedEntityType)} #${d.relatedEntityId}` : undefined} />
            <p className="px-5 pb-5 text-sm leading-relaxed whitespace-pre-wrap">{d.content}</p>
          </Card>
          <Card>
            <CardHeader title="Пікірлер" subtitle={`Пікірлер саны: ${d.comments.length}`}
              icon={<span className="grid size-9 place-items-center rounded-xl bg-primary-soft text-primary"><MessageSquare className="size-4" /></span>} />
            <div className="space-y-3 px-5">
              {d.comments.map((c) => (
                <div key={c.id} className="flex gap-3">
                  <Avatar name={c.author} size="sm" />
                  <div className="min-w-0 flex-1 rounded-2xl bg-canvas px-3.5 py-2.5">
                    <p className="text-xs"><b>{c.author}</b> <span className="text-muted">&middot; {relative(c.createdAt)}</span></p>
                    <p className="mt-0.5 text-sm whitespace-pre-wrap">{c.body}</p>
                  </div>
                </div>
              ))}
            </div>
            <form className="flex gap-2 p-5" onSubmit={(e) => { e.preventDefault(); if (newComment.trim()) addComment.mutate() }}>
              <input className="input" placeholder="Пікір жазыңыз..." maxLength={1000} value={newComment} onChange={(e) => setNewComment(e.target.value)} aria-label="Жаңа пікір" />
              <Button type="submit" loading={addComment.isPending} disabled={!newComment.trim()}>Жариялау</Button>
            </form>
          </Card>
        </div>
        <div className="space-y-6">
          <Card>
            <CardHeader title="Тарих" subtitle="Құжатпен жасалған барлық әрекеттер ретімен" />
            <ol className="relative mx-5 mb-5 space-y-3 border-l border-line pl-5">
              {d.history.map((h, i) => (
                <li key={i} className="relative">
                  <span className="absolute top-1.5 -left-[25px] size-2.5 rounded-full bg-[#FFB86B] ring-4 ring-white" />
                  <p className="text-sm font-semibold">{humanize(h.action)} {h.toStatus && <span className="font-normal text-muted">&rarr; {humanize(h.toStatus)}</span>}</p>
                  <p className="text-xs text-muted">{h.actor ?? 'жүйе'} &middot; {dateTime(h.at)}</p>
                  {h.note && <p className="mt-0.5 text-xs text-muted italic">{h.note}</p>}
                </li>
              ))}
            </ol>
          </Card>
          {d.steps.length > 0 && (
            <Card>
              <CardHeader title="Бекіту қадамдары" subtitle="Барлық кезеңдер" />
              <ul className="space-y-2 px-5 pb-5">
                {d.steps.map((st) => (
                  <li key={st.id} className="flex items-start justify-between gap-3 rounded-xl border border-line px-3 py-2">
                    <div className="min-w-0"><p className="text-sm font-semibold">{st.round}-кезең &middot; {st.order}. {st.name}</p><p className="text-xs text-muted">{label(st.requiredRole)}{st.decidedBy ? ` — ${st.decidedBy}` : ''}{st.comment ? `: ${st.comment}` : ''}</p></div>
                    <StatusBadge status={st.status === 'ACTIVE' ? 'PENDING' : st.status} label={humanize(st.status)} />
                  </li>
                ))}
              </ul>
            </Card>
          )}
          <Card>
            <CardHeader title="Нұсқалар" subtitle="Маңызды өзгерістердің көшірмелері" />
            <ul className="space-y-2 px-5 pb-5">
              {d.revisions.map((r) => (
                <li key={r.revision} className="flex items-center justify-between gap-3 text-sm">
                  <span className="min-w-0 truncate"><b>{r.revision}-нұсқа</b> {r.title}</span>
                  <span className="shrink-0 text-xs text-muted">{r.changedBy} &middot; {relative(r.createdAt)}</span>
                </li>
              ))}
            </ul>
          </Card>
        </div>
      </div>

      <Modal open={decision !== null} onClose={() => setDecision(null)} size="sm"
        title={decision === 'approve' ? `Бекіту: ${s.currentStep}` : decision === 'reject' ? 'Құжатты қабылдамау' : 'Пысықтауға қайтару'}
        subtitle={s.title}
        footer={<>
          <Button variant="secondary" onClick={() => setDecision(null)}>Болдырмау</Button>
          <Button variant={decision === 'reject' ? 'danger' : decision === 'approve' ? 'success' : 'primary'}
            disabled={needsText && comment.trim().length < 5} loading={decide.isPending} onClick={() => decision && decide.mutate(decision)}>
            {decision === 'approve' ? 'Бекіту' : decision === 'reject' ? 'Қабылдамау' : 'Қайтару'}
          </Button>
        </>}>
        <Field label={decision === 'approve' ? 'Пікір (міндетті емес)' : decision === 'reject' ? 'Себебі (міндетті)' : 'Нені өзгерту керек? (міндетті)'}
          error={needsText && comment.length > 0 && comment.trim().length < 5 ? 'Кемінде 5 таңба' : undefined} htmlFor="decision-text">
          <textarea id="decision-text" rows={4} className="input" value={comment} maxLength={500} onChange={(e) => setComment(e.target.value)} />
        </Field>
      </Modal>

      <Modal open={editing} onClose={() => setEditing(false)} size="lg" title="Қараламаны өңдеу" subtitle="Сақтағанда жаңа нұсқа жасалады"
        footer={<>
          <Button variant="secondary" onClick={() => setEditing(false)}>Болдырмау</Button>
          <Button loading={save.isPending} disabled={draft.title.trim().length < 3 || draft.content.trim().length < 10} onClick={() => save.mutate()}>Нұсқаны сақтау</Button>
        </>}>
        <div className="space-y-4">
          <Field label="Тақырып" htmlFor="et"><input id="et" className="input" value={draft.title} maxLength={160} onChange={(e) => setDraft({ ...draft, title: e.target.value })} /></Field>
          <Field label="Сома (₸)" htmlFor="ea"><input id="ea" className="input" inputMode="decimal" value={draft.amount} onChange={(e) => setDraft({ ...draft, amount: e.target.value.replace(/[^\d.]/g, '') })} /></Field>
          <Field label="Мазмұны" htmlFor="ec"><textarea id="ec" rows={8} className="input" value={draft.content} maxLength={4000} onChange={(e) => setDraft({ ...draft, content: e.target.value })} /></Field>
        </div>
      </Modal>
    </div>
  )
}
