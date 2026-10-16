import { useSearchParams } from 'react-router'

/** Keeps the active tab in the URL (?tab=...), so links like /inventory?tab=alerts work. */
export function useTab(defaultTab: string): [string, (t: string) => void] {
  const [params, setParams] = useSearchParams()
  const tab = params.get('tab') ?? defaultTab
  const set = (t: string) => {
    const next = new URLSearchParams(params)
    next.set('tab', t)
    setParams(next, { replace: true })
  }
  return [tab, set]
}
