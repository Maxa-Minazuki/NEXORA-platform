import { useState, type ReactNode } from 'react'
import { ChartColumn, Table2 } from 'lucide-react'
import { Card, CardHeader } from '../ui/Card'
import { EmptyState } from '../ui/States'
import { cx } from '../ui/cx'

export interface TableColumn { key: string; label: string; format?: (v: unknown) => string }

/** Chart container with an accessible table view (identity never depends on color alone). */
export function ChartCard({ title, subtitle, children, rows, columns, empty, className, height = 260 }: {
  title: string; subtitle?: string; children: ReactNode; rows: Record<string, unknown>[]; columns: TableColumn[]
  empty?: boolean; className?: string; height?: number
}) {
  const [table, setTable] = useState(false)
  return (
    <Card className={cx('flex flex-col', className)}>
      <CardHeader title={title} subtitle={subtitle} action={
        <button onClick={() => setTable((t) => !t)} aria-pressed={table}
          className="inline-flex items-center gap-1.5 rounded-lg border border-line px-2 py-1 text-[11px] font-semibold text-muted transition hover:text-primary">
          {table ? <ChartColumn className="size-3.5" /> : <Table2 className="size-3.5" />} {table ? 'Диаграмма' : 'Кесте'}
        </button>
      } />
      <div className="px-3 pb-4 sm:px-4">
        {empty ? (
          <EmptyState compact title="Деректер әлі жоқ" description="Модульде әрекет тіркелгенде диаграмма толтырылады." />
        ) : table ? (
          <div className="max-h-[260px] overflow-auto rounded-xl border border-line">
            <table className="w-full text-xs">
              <thead className="sticky top-0 bg-canvas">
                <tr>{columns.map((c) => <th key={c.key} className="px-3 py-2 text-left font-semibold text-muted">{c.label}</th>)}</tr>
              </thead>
              <tbody>
                {rows.map((r, i) => (
                  <tr key={i} className="border-t border-line/70">
                    {columns.map((c) => <td key={c.key} className="px-3 py-1.5 tabular-nums">{c.format ? c.format(r[c.key]) : String(r[c.key] ?? '')}</td>)}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <div style={{ height }} className="w-full min-w-0">{children}</div>
        )}
      </div>
    </Card>
  )
}

export function Legend({ items }: { items: { label: string; color: string }[] }) {
  return (
    <div className="flex flex-wrap gap-x-4 gap-y-1 px-1 pb-2 text-xs text-muted">
      {items.map((i) => (
        <span key={i.label} className="inline-flex items-center gap-1.5">
          <span className="size-2.5 rounded-sm" style={{ background: i.color }} />{i.label}
        </span>
      ))}
    </div>
  )
}
