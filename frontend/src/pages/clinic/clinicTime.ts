import { isoDate } from '../../lib/format'

/** Kazakhstan uses a single fixed offset (UTC+5); clinic days are interpreted in that zone. */
export const CLINIC_OFFSET = '+05:00'

export function dayRange(date: string): { from: string; to: string } {
  const from = new Date(`${date}T00:00:00${CLINIC_OFFSET}`)
  const to = new Date(from.getTime() + 86_400_000)
  return { from: from.toISOString(), to: to.toISOString() }
}

export function today(): string {
  return isoDate(0)
}
