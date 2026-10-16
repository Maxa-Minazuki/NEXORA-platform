import { useEffect, useState } from 'react'
import { Outlet, useLocation } from 'react-router'
import { Menu } from 'lucide-react'
import { Sidebar } from './Sidebar'
import { NotificationBell } from './NotificationBell'
import { UserMenu } from './UserMenu'
import { GlobalSearch } from './GlobalSearch'
import { Breadcrumbs } from './Breadcrumbs'
import { useEventStream } from '../../hooks/useEventStream'
import { cx } from '../ui/cx'

const STORAGE_KEY = 'nexora.sidebar.collapsed'

function readCollapsed(): boolean {
  try {
    return localStorage.getItem(STORAGE_KEY) === '1'
  } catch {
    return false
  }
}

export function AppLayout() {
  const [collapsed, setCollapsed] = useState(readCollapsed)
  const [mobileOpen, setMobileOpen] = useState(false)
  const location = useLocation()
  useEventStream(true)

  useEffect(() => {
    try { localStorage.setItem(STORAGE_KEY, collapsed ? '1' : '0') } catch { /* storage unavailable */ }
  }, [collapsed])
  useEffect(() => { window.scrollTo({ top: 0 }) }, [location.pathname])

  return (
    <div className="min-h-screen">
      <Sidebar collapsed={collapsed} onToggle={() => setCollapsed((c) => !c)} mobileOpen={mobileOpen} onCloseMobile={() => setMobileOpen(false)} />
      <div className={cx('transition-[padding] duration-300', collapsed ? 'lg:pl-[76px]' : 'lg:pl-64')}>
        <header className="glass sticky top-0 z-20 border-b border-line">
          <div className="flex h-16 items-center gap-3 px-4 sm:px-6">
            <button onClick={() => setMobileOpen(true)} className="grid size-10 place-items-center rounded-xl border border-line bg-white text-muted lg:hidden" aria-label="Мәзірді ашу">
              <Menu className="size-5" />
            </button>
            <div className="hidden min-w-0 flex-1 md:block"><Breadcrumbs /></div>
            <div className="flex min-w-0 flex-1 justify-end md:flex-none"><GlobalSearch /></div>
            <NotificationBell />
            <UserMenu />
          </div>
        </header>
        <main key={location.pathname} className="mx-auto w-full max-w-[1440px] animate-fade px-4 py-6 sm:px-6 lg:py-8">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
