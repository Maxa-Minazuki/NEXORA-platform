import { Link, useLocation } from 'react-router'
import { ChevronRight } from 'lucide-react'
import { NAV, navLabel } from './nav'
import { t } from '../../lib/i18n'

export function Breadcrumbs() {
  const { pathname } = useLocation()
  const parts = pathname.split('/').filter(Boolean)
  const root = NAV.find((n) => n.to === '/' + (parts[0] ?? ''))
  const crumbs: { label: string; to?: string }[] = [{ label: 'NEXORA', to: '/' }]
  if (root && root.to !== '/') crumbs.push({ label: navLabel(root), to: root.to })
  if (parts.length === 0) crumbs.push({ label: t('nav.overview') })
  if (parts.length > 1) crumbs.push({ label: 'Құжат #' + parts[1] })
  return (
    <nav aria-label="Навигация жолы" className="flex min-w-0 items-center gap-1 text-xs font-semibold text-muted">
      {crumbs.map((c, i) => (
        <span key={i} className="flex min-w-0 items-center gap-1">
          {i > 0 && <ChevronRight className="size-3.5 shrink-0" />}
          {c.to && i < crumbs.length - 1 ? <Link to={c.to} className="truncate hover:text-primary">{c.label}</Link>
            : <span className="truncate text-ink">{c.label}</span>}
        </span>
      ))}
    </nav>
  )
}
