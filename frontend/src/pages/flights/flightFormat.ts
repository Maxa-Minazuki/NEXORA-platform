import type { Flight } from '../../lib/types'
import { time } from '../../lib/format'

export const depTime = (f: Flight) => time(f.departureTime, f.origin.timezone)
export const arrTime = (f: Flight) => time(f.arrivalTime, f.destination.timezone)
export function dayShift(f: Flight): number {
  const d = (iso: string, tz: string) => new Intl.DateTimeFormat('en-CA', { timeZone: tz }).format(new Date(iso))
  const a = new Date(d(f.departureTime, f.origin.timezone)).getTime()
  const b = new Date(d(f.arrivalTime, f.destination.timezone)).getTime()
  return Math.round((b - a) / 86_400_000)
}
