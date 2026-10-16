import { useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router'
import { ArrowRight, FileText, Package, Plane, Search, Stethoscope } from 'lucide-react'
import { useAuth } from '../../auth/AuthContext'
import { cx } from '../ui/cx'

/** Searches inside the modules by forwarding the query (?q=) to their list pages. */
export function GlobalSearch() {
  const [q, setQ] = useState('')
  const [open, setOpen] = useState(false)
  const [active, setActive] = useState(0)
  const navigate = useNavigate()
  const { can } = useAuth()
  const ref = useRef<HTMLDivElement>(null)

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'k') {
        e.preventDefault()
        ref.current?.querySelector('input')?.focus()
      }
    }
    const onClick = (e: MouseEvent) => { if (ref.current && !ref.current.contains(e.target as Node)) setOpen(false) }
    document.addEventListener('keydown', onKey)
    document.addEventListener('mousedown', onClick)
    return () => { document.removeEventListener('keydown', onKey); document.removeEventListener('mousedown', onClick) }
  }, [])

  const targets = [
    can('ADMIN', 'MANAGER', 'EMPLOYEE') && { label: 'Тауарлар', icon: <Package className="size-4" />, to: `/inventory?q=${encodeURIComponent(q)}` },
    can('ADMIN', 'MANAGER', 'EMPLOYEE', 'DOCTOR') && { label: 'Пациенттер', icon: <Stethoscope className="size-4" />, to: `/clinic?tab=patients&q=${encodeURIComponent(q)}` },
    { label: 'Қабылдаулар', icon: <Stethoscope className="size-4" />, to: `/clinic?tab=appointments&q=${encodeURIComponent(q)}` },
    { label: 'Құжаттар', icon: <FileText className="size-4" />, to: `/documents?q=${encodeURIComponent(q)}` },
    { label: 'Брондаулар', icon: <Plane className="size-4" />, to: `/flights?tab=bookings&q=${encodeURIComponent(q)}` },
  ].filter(Boolean) as { label: string; icon: React.ReactNode; to: string }[]

  const go = (to: string) => { navigate(to); setOpen(false); setQ('') }

  return (
    <div ref={ref} className="relative w-full max-w-md">
      <Search className="pointer-events-none absolute top-1/2 left-3.5 size-4 -translate-y-1/2 text-muted" />
      <input
        value={q}
        onChange={(e) => { setQ(e.target.value); setOpen(e.target.value.trim().length > 0); setActive(0) }}
        onFocus={() => q.trim() && setOpen(true)}
        onKeyDown={(e) => {
          if (!open) return
          if (e.key === 'ArrowDown') { e.preventDefault(); setActive((a) => (a + 1) % targets.length) }
          if (e.key === 'ArrowUp') { e.preventDefault(); setActive((a) => (a - 1 + targets.length) % targets.length) }
          if (e.key === 'Enter') go(targets[active].to)
          if (e.key === 'Escape') setOpen(false)
        }}
        placeholder="Тауар, пациент, құжат іздеу..."
        aria-label="Жалпы іздеу"
        className="input h-10 rounded-xl border-line bg-canvas/70 pr-14 pl-10 focus:bg-white"
      />
      <kbd className="pointer-events-none absolute top-1/2 right-3 hidden -translate-y-1/2 rounded-md border border-line bg-white px-1.5 text-[10px] font-semibold text-muted sm:block">Ctrl K</kbd>
      {open && (
        <div className="absolute inset-x-0 z-40 mt-2 animate-rise overflow-hidden rounded-2xl border border-line bg-white p-1.5 shadow-pop">
          {targets.map((t, i) => (
            <button key={t.label} onMouseEnter={() => setActive(i)} onClick={() => go(t.to)}
              className={cx('flex w-full items-center gap-3 rounded-xl px-3 py-2 text-sm', i === active ? 'bg-primary-soft text-primary' : 'text-ink')}>
              {t.icon}
              <span className="flex-1 truncate text-left"><b>{t.label}</b> ішінен іздеу: &laquo;{q}&raquo;</span>
              <ArrowRight className="size-4 opacity-60" />
            </button>
          ))}
        </div>
      )}
    </div>
  )
}
