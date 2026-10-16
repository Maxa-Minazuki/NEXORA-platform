import { useEffect, useRef, useState } from 'react'
import { Link, useNavigate } from 'react-router'
import { Bell, CheckCheck } from 'lucide-react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '../../lib/api'
import type { AppNotification } from '../../lib/types'
import { relative } from '../../lib/format'
import { SeverityDot } from '../../pages/notifications/severity'
import { cx } from '../ui/cx'

export function NotificationBell() {
  const [open, setOpen] = useState(false)
  const ref = useRef<HTMLDivElement>(null)
  const navigate = useNavigate()
  const qc = useQueryClient()
  const unread = useQuery({ queryKey: ['notifications', 'unread'], queryFn: () => api.get<{ count: number }>('/notifications/unread-count') })
  const latest = useQuery({
    queryKey: ['notifications', 'latest'],
    queryFn: () => api.get<AppNotification[]>('/notifications/latest'),
    enabled: open,
  })
  const readAll = useMutation({
    mutationFn: () => api.post('/notifications/read-all'),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['notifications'] }),
  })
  const read = useMutation({
    mutationFn: (id: number) => api.post(`/notifications/${id}/read`),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['notifications'] }),
  })

  useEffect(() => {
    const onClick = (e: MouseEvent) => { if (ref.current && !ref.current.contains(e.target as Node)) setOpen(false) }
    document.addEventListener('mousedown', onClick)
    return () => document.removeEventListener('mousedown', onClick)
  }, [])

  const count = unread.data?.count ?? 0
  return (
    <div className="relative" ref={ref}>
      <button onClick={() => setOpen((o) => !o)} aria-label={`Хабарландырулар, оқылмағаны: ${count}`}
        className="relative grid size-10 place-items-center rounded-xl border border-line bg-white text-muted transition hover:text-primary">
        <Bell className={cx('size-[18px]', count > 0 && 'text-ink')} />
        {count > 0 && (
          <span className="absolute -top-1.5 -right-1.5 grid min-w-5 place-items-center rounded-full bg-rose px-1 text-[10px] font-bold text-white ring-2 ring-white">
            {count > 99 ? '99+' : count}
          </span>
        )}
      </button>
      {open && (
        <div className="absolute right-0 z-40 mt-2 w-[min(22rem,calc(100vw-2rem))] animate-rise overflow-hidden rounded-2xl border border-line bg-white shadow-pop">
          <div className="flex items-center justify-between border-b border-line px-4 py-3">
            <p className="text-sm font-bold">Хабарландырулар</p>
            <button onClick={() => readAll.mutate()} className="inline-flex items-center gap-1 text-xs font-semibold text-primary hover:underline">
              <CheckCheck className="size-3.5" /> Барлығын оқылды деу
            </button>
          </div>
          <div className="max-h-96 overflow-y-auto">
            {latest.isLoading && <p className="p-4 text-sm text-muted">Жүктелуде...</p>}
            {latest.data?.length === 0 && <p className="p-6 text-center text-sm text-muted">Жаңа хабарландыру жоқ.</p>}
            {latest.data?.map((n) => (
              <button key={n.id} onClick={() => { read.mutate(n.id); setOpen(false); if (n.link) navigate(n.link) }}
                className={cx('flex w-full gap-3 border-b border-line/70 px-4 py-3 text-left transition hover:bg-canvas', !n.read && 'bg-primary-soft/40')}>
                <SeverityDot severity={n.severity} />
                <div className="min-w-0 flex-1">
                  <p className="truncate text-sm font-semibold">{n.title}</p>
                  <p className="line-clamp-2 text-xs text-muted">{n.message}</p>
                  <p className="mt-1 text-[11px] text-muted">{relative(n.createdAt)}</p>
                </div>
              </button>
            ))}
          </div>
          <Link to="/notifications" onClick={() => setOpen(false)} className="block py-3 text-center text-xs font-semibold text-primary hover:bg-canvas">
            Хабарландыру орталығын ашу
          </Link>
        </div>
      )}
    </div>
  )
}
