import { createContext, useCallback, useContext, useMemo, useState, type ReactNode } from 'react'
import { CircleCheck, CircleX, Info, TriangleAlert, X } from 'lucide-react'
import { cx } from './cx'

type ToastKind = 'success' | 'error' | 'info' | 'warning'
interface ToastItem { id: number; kind: ToastKind; title: string; message?: string }

interface ToastApi {
  success: (title: string, message?: string) => void
  error: (title: string, message?: string) => void
  info: (title: string, message?: string) => void
  warning: (title: string, message?: string) => void
}

const ToastContext = createContext<ToastApi | null>(null)
let seq = 0

const styles: Record<ToastKind, { icon: ReactNode; bar: string }> = {
  success: { icon: <CircleCheck className="size-5 text-mint" />, bar: 'bg-mint' },
  error: { icon: <CircleX className="size-5 text-danger" />, bar: 'bg-danger' },
  info: { icon: <Info className="size-5 text-sky" />, bar: 'bg-sky' },
  warning: { icon: <TriangleAlert className="size-5 text-[#E89A3C]" />, bar: 'bg-peach' },
}

export function ToastProvider({ children }: { children: ReactNode }) {
  const [items, setItems] = useState<ToastItem[]>([])
  const dismiss = useCallback((id: number) => setItems((list) => list.filter((t) => t.id !== id)), [])
  const push = useCallback((kind: ToastKind, title: string, message?: string) => {
    const id = ++seq
    setItems((list) => [...list.slice(-3), { id, kind, title, message }])
    setTimeout(() => dismiss(id), kind === 'error' ? 7000 : 4500)
  }, [dismiss])

  const api = useMemo<ToastApi>(() => ({
    success: (t, m) => push('success', t, m),
    error: (t, m) => push('error', t, m),
    info: (t, m) => push('info', t, m),
    warning: (t, m) => push('warning', t, m),
  }), [push])

  return (
    <ToastContext.Provider value={api}>
      {children}
      <div className="pointer-events-none fixed right-4 bottom-4 left-4 z-[60] flex flex-col items-end gap-2 sm:left-auto sm:w-96" aria-live="polite">
        {items.map((t) => (
          <div key={t.id} className="pointer-events-auto relative flex w-full animate-slide-in items-start gap-3 overflow-hidden rounded-2xl border border-line bg-white p-4 pl-5 shadow-pop">
            <span className={cx('absolute inset-y-0 left-0 w-1', styles[t.kind].bar)} />
            {styles[t.kind].icon}
            <div className="min-w-0 flex-1">
              <p className="text-sm font-semibold text-ink">{t.title}</p>
              {t.message && <p className="mt-0.5 text-sm break-words text-muted">{t.message}</p>}
            </div>
            <button onClick={() => dismiss(t.id)} className="text-muted hover:text-ink" aria-label="Жабу"><X className="size-4" /></button>
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  )
}

// eslint-disable-next-line react-refresh/only-export-components
export function useToast(): ToastApi {
  const ctx = useContext(ToastContext)
  if (!ctx) throw new Error('useToast must be used inside ToastProvider')
  return ctx
}
