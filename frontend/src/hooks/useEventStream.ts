import { useEffect } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { useToast } from '../components/ui/Toast'

/**
 * Subscribes to the backend's Server-Sent Events stream. "notification" events show a toast and
 * refresh the inbox; "dashboard" events (any committed business event) refresh the live data.
 * EventSource reconnects automatically if the connection drops.
 */
export function useEventStream(enabled: boolean) {
  const queryClient = useQueryClient()
  const toast = useToast()

  useEffect(() => {
    if (!enabled || typeof EventSource === 'undefined') return
    const source = new EventSource('/api/notifications/stream', { withCredentials: true })
    source.addEventListener('notification', (e) => {
      try {
        const n = JSON.parse((e as MessageEvent).data)
        const kind = n.severity === 'CRITICAL' ? 'error' : n.severity === 'WARNING' ? 'warning' : n.severity === 'SUCCESS' ? 'success' : 'info'
        toast[kind](n.title, n.message)
      } catch {
        /* ignore malformed event */
      }
      queryClient.invalidateQueries({ queryKey: ['notifications'] })
    })
    source.addEventListener('dashboard', () => {
      queryClient.invalidateQueries({ queryKey: ['dashboard'] })
      queryClient.invalidateQueries({ queryKey: ['inventory'] })
      queryClient.invalidateQueries({ queryKey: ['documents'] })
    })
    return () => source.close()
  }, [enabled, queryClient, toast])
}
