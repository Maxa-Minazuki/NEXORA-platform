import type { ReactNode } from 'react'
import { cx } from './cx'

export interface TabItem { key: string; label: string; icon?: ReactNode; count?: number }

export function Tabs({ items, value, onChange }: { items: TabItem[]; value: string; onChange: (key: string) => void }) {
  return (
    <div className="-mx-1 overflow-x-auto px-1 pb-1">
      <div className="inline-flex gap-1 rounded-2xl border border-line bg-white p-1" role="tablist">
        {items.map((t) => (
          <button
            key={t.key}
            role="tab"
            aria-selected={value === t.key}
            onClick={() => onChange(t.key)}
            className={cx(
              'inline-flex items-center gap-2 whitespace-nowrap rounded-xl px-3.5 py-2 text-sm font-semibold transition',
              value === t.key ? 'bg-primary text-white shadow-[0_6px_16px_-8px_rgba(108,99,255,0.8)]' : 'text-muted hover:bg-canvas hover:text-ink',
            )}
          >
            {t.icon}
            {t.label}
            {t.count !== undefined && (
              <span className={cx('rounded-full px-1.5 text-[11px]', value === t.key ? 'bg-white/25' : 'bg-canvas')}>{t.count}</span>
            )}
          </button>
        ))}
      </div>
    </div>
  )
}
