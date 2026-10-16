import { NavLink } from 'react-router'
import { PanelLeftClose, PanelLeftOpen, Sparkles, X } from 'lucide-react'
import { useAuth } from '../../auth/AuthContext'
import { cx } from '../ui/cx'
import { NAV, navLabel } from './nav'
import { useQuery } from '@tanstack/react-query'
import { api } from '../../lib/api'
import { t } from '../../lib/i18n'

export function Logo({ collapsed }: { collapsed?: boolean }) {
  return (
    <div className="flex items-center gap-3">
      <img src="/nexora.svg" alt="" className="size-9 rounded-xl shadow-[0_8px_18px_-8px_rgba(108,99,255,0.9)]" />
      {!collapsed && (
        <div className="leading-tight">
          <p className="font-display text-[17px] font-extrabold tracking-[0.14em] text-ink">NEXORA</p>
          <p className="text-[10px] font-semibold tracking-wide text-muted">{t('app.subtitle')}</p>
        </div>
      )}
    </div>
  )
}

export function Sidebar({ collapsed, onToggle, mobileOpen, onCloseMobile }: {
  collapsed: boolean; onToggle: () => void; mobileOpen: boolean; onCloseMobile: () => void
}) {
  const { user } = useAuth()
  const unread = useQuery({
    queryKey: ['notifications', 'unread'],
    queryFn: () => api.get<{ count: number }>('/notifications/unread-count'),
    refetchInterval: 60_000,
  })
  const items = NAV.filter((n) => !n.roles || (user && n.roles.includes(user.role)))

  const content = (isMobile: boolean) => (
    <div className="flex h-full flex-col">
      <div className={cx('flex h-16 items-center px-4', collapsed && !isMobile ? 'justify-center' : 'justify-between')}>
        <Logo collapsed={collapsed && !isMobile} />
        {isMobile && (
          <button onClick={onCloseMobile} className="rounded-lg p-2 text-muted hover:bg-canvas" aria-label="Мәзірді жабу"><X className="size-5" /></button>
        )}
      </div>
      <nav className="mt-2 flex-1 space-y-1 overflow-y-auto px-3" aria-label="Негізгі мәзір">
        {items.map((item) => (
          <NavLink
            key={item.to}
            to={item.to}
            end={item.to === '/'}
            onClick={isMobile ? onCloseMobile : undefined}
            title={collapsed && !isMobile ? navLabel(item) : undefined}
            className={({ isActive }) => cx(
              'group relative flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-semibold transition',
              collapsed && !isMobile && 'justify-center px-0',
              isActive ? 'bg-primary-soft text-primary' : 'text-muted hover:bg-canvas hover:text-ink',
            )}
          >
            {({ isActive }) => (
              <>
                {isActive && <span className="absolute top-2 bottom-2 left-0 w-1 rounded-r-full bg-primary" />}
                <span className={cx('transition group-hover:scale-110', isActive && 'text-primary')}>{item.icon}</span>
                {(!collapsed || isMobile) && <span className="flex-1">{navLabel(item)}</span>}
                {item.to === '/notifications' && (unread.data?.count ?? 0) > 0 && (
                  <span className={cx('rounded-full bg-rose px-1.5 text-[10px] font-bold text-white',
                    collapsed && !isMobile && 'absolute top-1 right-1.5')}>{unread.data!.count}</span>
                )}
              </>
            )}
          </NavLink>
        ))}
      </nav>
      {(!collapsed || isMobile) && (
        <div className="m-3 overflow-hidden rounded-2xl bg-gradient-to-br from-[#6C63FF] to-[#4385FF] p-4 text-white">
          <Sparkles className="size-5 opacity-90" />
          <p className="mt-2 text-sm font-bold">{t('nav.lab')}</p>
          <p className="mt-1 text-xs text-white/80">{t('app.tagline')}</p>
          <NavLink to="/lab" onClick={isMobile ? onCloseMobile : undefined}
            className="mt-3 inline-block rounded-lg bg-white/20 px-3 py-1.5 text-xs font-semibold backdrop-blur transition hover:bg-white/30">
            Зертханаға өту
          </NavLink>
        </div>
      )}
      {!isMobile && (
        <button onClick={onToggle} className="m-3 flex items-center justify-center gap-2 rounded-xl border border-line py-2 text-xs font-semibold text-muted transition hover:text-primary"
          aria-label={collapsed ? 'Мәзірді жаю' : 'Мәзірді жию'}>
          {collapsed ? <PanelLeftOpen className="size-4" /> : <><PanelLeftClose className="size-4" /> Жию</>}
        </button>
      )}
    </div>
  )

  return (
    <>
      <aside className={cx('fixed inset-y-0 left-0 z-30 hidden border-r border-line bg-white/90 backdrop-blur transition-[width] duration-300 lg:block',
        collapsed ? 'w-[76px]' : 'w-64')}>
        {content(false)}
      </aside>
      {mobileOpen && (
        <div className="fixed inset-0 z-40 lg:hidden">
          <div className="absolute inset-0 animate-fade bg-ink/40" onClick={onCloseMobile} />
          <aside className="absolute inset-y-0 left-0 w-72 max-w-[85vw] animate-rise bg-white shadow-pop">{content(true)}</aside>
        </div>
      )}
    </>
  )
}
