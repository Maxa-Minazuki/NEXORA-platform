import { useEffect, useRef, useState } from 'react'
import { Link, useNavigate } from 'react-router'
import { ChevronDown, LogOut, Settings, UserRound } from 'lucide-react'
import { useAuth } from '../../auth/AuthContext'
import { Avatar } from '../ui/Avatar'
import { Badge } from '../ui/Badge'
import { label } from '../../lib/i18n'

export function UserMenu() {
  const { user, logout } = useAuth()
  const [open, setOpen] = useState(false)
  const ref = useRef<HTMLDivElement>(null)
  const navigate = useNavigate()
  useEffect(() => {
    const onClick = (e: MouseEvent) => { if (ref.current && !ref.current.contains(e.target as Node)) setOpen(false) }
    document.addEventListener('mousedown', onClick)
    return () => document.removeEventListener('mousedown', onClick)
  }, [])
  if (!user) return null
  return (
    <div className="relative" ref={ref}>
      <button onClick={() => setOpen((o) => !o)} className="flex items-center gap-2 rounded-xl border border-line bg-white py-1 pr-2 pl-1 transition hover:border-primary/40"
        aria-label="Пайдаланушы мәзірі">
        <Avatar name={user.fullName} size="sm" />
        <span className="hidden text-left leading-tight md:block">
          <span className="block max-w-36 truncate text-xs font-bold">{user.fullName}</span>
          <span className="block text-[10px] font-semibold text-muted">{label(user.role)}</span>
        </span>
        <ChevronDown className="size-4 text-muted" />
      </button>
      {open && (
        <div className="absolute right-0 z-40 mt-2 w-64 animate-rise overflow-hidden rounded-2xl border border-line bg-white shadow-pop">
          <div className="flex items-center gap-3 border-b border-line p-4">
            <Avatar name={user.fullName} />
            <div className="min-w-0">
              <p className="truncate text-sm font-bold">{user.fullName}</p>
              <p className="truncate text-xs text-muted">{user.email}</p>
              <Badge tone="violet" className="mt-1">{label(user.role)}</Badge>
            </div>
          </div>
          <Link to="/settings" onClick={() => setOpen(false)} className="flex items-center gap-2 px-4 py-2.5 text-sm hover:bg-canvas">
            <UserRound className="size-4 text-muted" /> Профиль және баптаулар
          </Link>
          <Link to="/settings?tab=platform" onClick={() => setOpen(false)} className="flex items-center gap-2 px-4 py-2.5 text-sm hover:bg-canvas">
            <Settings className="size-4 text-muted" /> Платформа баптаулары
          </Link>
          <button onClick={async () => { await logout(); navigate('/login') }}
            className="flex w-full items-center gap-2 border-t border-line px-4 py-2.5 text-sm text-danger hover:bg-danger-soft">
            <LogOut className="size-4" /> Жүйеден шығу
          </button>
        </div>
      )}
    </div>
  )
}
