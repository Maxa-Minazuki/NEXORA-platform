import type { HTMLAttributes, ReactNode, TdHTMLAttributes, ThHTMLAttributes } from 'react'
import { cx } from './cx'

/** Tables scroll horizontally inside their card, never the page. */
export function Table({ children, minWidth = 720 }: { children: ReactNode; minWidth?: number }) {
  return (
    <div className="overflow-x-auto">
      <table className="w-full text-left text-sm" style={{ minWidth }}>{children}</table>
    </div>
  )
}

export function Th({ className, ...rest }: ThHTMLAttributes<HTMLTableCellElement>) {
  return <th className={cx('border-y border-line bg-canvas/60 px-4 py-2.5 text-[11px] font-bold tracking-wide whitespace-nowrap text-muted uppercase', className)} {...rest} />
}

export function Td({ className, ...rest }: TdHTMLAttributes<HTMLTableCellElement>) {
  return <td className={cx('border-b border-line/70 px-4 py-3 align-middle whitespace-nowrap', className)} {...rest} />
}

export function Tr({ className, ...rest }: HTMLAttributes<HTMLTableRowElement>) {
  return <tr className={cx('transition-colors hover:bg-primary-soft/40', className)} {...rest} />
}

export function SortHeader({ label, field, sort, onSort, className }: {
  label: string; field: string; sort: string; onSort: (s: string) => void; className?: string
}) {
  const [f, dir] = sort.split(',')
  const active = f === field
  return (
    <Th className={className}>
      <button className={cx('inline-flex items-center gap-1 uppercase', active && 'text-primary')}
        onClick={() => onSort(`${field},${active && dir === 'asc' ? 'desc' : 'asc'}`)}>
        {label}<span className="text-[9px]">{active ? (dir === 'asc' ? '▲' : '▼') : '↕'}</span>
      </button>
    </Th>
  )
}
