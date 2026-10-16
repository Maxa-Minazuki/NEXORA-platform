import { label } from './i18n'

/**
 * Kazakh formatting of numbers, money and dates.
 *
 * Browsers ship different ICU locale data: some Chromium builds have no "kk" data and silently fall
 * back to Russian month names. So Intl is only used to split an instant into numeric parts in the
 * right time zone; all Kazakh words (months, weekdays, relative time) come from the tables below.
 */
export const BUSINESS_TZ = 'Asia/Almaty'

const NBSP = ' '
const MONTHS = ['қаңтар', 'ақпан', 'наурыз', 'сәуір', 'мамыр', 'маусым', 'шілде', 'тамыз', 'қыркүйек', 'қазан', 'қараша', 'желтоқсан']
const MONTHS_SHORT = ['қаң.', 'ақп.', 'нау.', 'сәу.', 'мам.', 'мау.', 'шіл.', 'там.', 'қыр.', 'қаз.', 'қар.', 'жел.']
const WEEKDAYS = ['жексенбі', 'дүйсенбі', 'сейсенбі', 'сәрсенбі', 'бейсенбі', 'жұма', 'сенбі']
const WEEKDAYS_SHORT = ['жс', 'дс', 'сс', 'ср', 'бс', 'жм', 'сн']

interface Parts { year: number; month: number; day: number; hour: number; minute: number; weekday: number }

const partFormatters = new Map<string, Intl.DateTimeFormat>()

function parts(date: Date, timeZone: string): Parts {
  let f = partFormatters.get(timeZone)
  if (!f) {
    f = new Intl.DateTimeFormat('en-US', {
      timeZone, year: 'numeric', month: 'numeric', day: 'numeric', hour: 'numeric', minute: 'numeric', hourCycle: 'h23',
    })
    partFormatters.set(timeZone, f)
  }
  const p: Record<string, number> = {}
  for (const x of f.formatToParts(date)) {
    if (x.type !== 'literal') p[x.type] = Number(x.value)
  }
  const weekday = new Date(Date.UTC(p.year, p.month - 1, p.day)).getUTCDay()
  return { year: p.year, month: p.month, day: p.day, hour: p.hour % 24, minute: p.minute, weekday }
}

const pad = (n: number) => String(n).padStart(2, '0')
const capitalize = (s: string) => s.charAt(0).toUpperCase() + s.slice(1)

/** 2272000 -> "2 272 000" (non-breaking spaces, Kazakh grouping). */
function group(value: number, decimals = 0): string {
  const negative = value < 0
  const fixed = Math.abs(value).toFixed(decimals)
  const [int, frac] = fixed.split('.')
  const grouped = int.replace(/\B(?=(\d{3})+(?!\d))/g, NBSP)
  return (negative ? '-' : '') + grouped + (frac ? ',' + frac : '')
}

/** Tenge amounts: "2 272 000 ₸". */
export function money(value: number | null | undefined): string {
  if (value === null || value === undefined) return '-'
  return group(Math.round(value)) + NBSP + '₸'
}

/** Compact tenge amounts: "7,6 млн ₸". */
export function compactMoney(value: number | null | undefined): string {
  if (value === null || value === undefined) return '-'
  const abs = Math.abs(value)
  const [div, unit] = abs >= 1e9 ? [1e9, 'млрд'] : abs >= 1e6 ? [1e6, 'млн'] : abs >= 1e3 ? [1e3, 'мың'] : [1, '']
  const scaled = value / div
  const text = div === 1 ? group(Math.round(scaled)) : group(scaled, scaled % 1 === 0 ? 0 : 1)
  return text + (unit ? NBSP + unit : '') + NBSP + '₸'
}

export function number(value: number | null | undefined): string {
  return value === null || value === undefined ? '-' : group(Math.round(value))
}

/** "16 қаз., 09:30" */
export function dateTime(iso: string | null | undefined, timeZone = BUSINESS_TZ): string {
  if (!iso) return '-'
  const p = parts(new Date(iso), timeZone)
  return `${p.day} ${MONTHS_SHORT[p.month - 1]}, ${pad(p.hour)}:${pad(p.minute)}`
}

/** "16 қазан 2026 ж." */
export function dateOnly(iso: string | null | undefined, timeZone = BUSINESS_TZ): string {
  if (!iso) return '-'
  const p = parts(new Date(iso), timeZone)
  return `${p.day} ${MONTHS[p.month - 1]} ${p.year} ж.`
}

/** "09:30" */
export function time(iso: string, timeZone = BUSINESS_TZ): string {
  const p = parts(new Date(iso), timeZone)
  return `${pad(p.hour)}:${pad(p.minute)}`
}

/** "жм, 16 қаз." */
export function weekdayDate(iso: string, timeZone = BUSINESS_TZ): string {
  const p = parts(new Date(iso), timeZone)
  return `${WEEKDAYS_SHORT[p.weekday]}, ${p.day} ${MONTHS_SHORT[p.month - 1]}`
}

/** "Жұма, 16 қазан 2026 ж." */
export function longDate(date: Date = new Date(), timeZone = BUSINESS_TZ): string {
  const p = parts(date, timeZone)
  return `${capitalize(WEEKDAYS[p.weekday])}, ${p.day} ${MONTHS[p.month - 1]} ${p.year} ж.`
}

/** Current hour (0-23) in the business time zone. */
export function businessHour(date: Date = new Date()): number {
  return parts(date, BUSINESS_TZ).hour
}

/** yyyy-mm-dd of "today + offset" in the business time zone. */
export function isoDate(offsetDays = 0, timeZone = BUSINESS_TZ): string {
  const p = parts(new Date(Date.now() + offsetDays * 86_400_000), timeZone)
  return `${p.year}-${pad(p.month)}-${pad(p.day)}`
}

/** "2026-10-16" -> "16 қаз." */
export function shortDay(isoDay: string): string {
  const [, m, d] = isoDay.split('-').map(Number)
  return `${d} ${MONTHS_SHORT[m - 1]}`
}

export function duration(minutes: number): string {
  const h = Math.floor(minutes / 60)
  const m = minutes % 60
  return h ? `${h} сағ ${m.toString().padStart(2, '0')} мин` : `${m} мин`
}

/** "жаңа ғана", "5 минут бұрын", "3 сағат бұрын", "кеше", "4 күн бұрын", "2 сағаттан кейін" */
export function relative(iso: string): string {
  const diffSec = (Date.now() - new Date(iso).getTime()) / 1000
  const future = diffSec < 0
  const abs = Math.abs(diffSec)
  const say = (n: number, unit: string) => (future ? `${n} ${unit}тан кейін` : `${n} ${unit} бұрын`)
  if (abs < 45) return future ? 'қазір' : 'жаңа ғана'
  if (abs < 3600) return future ? `${Math.round(abs / 60)} минуттан кейін` : say(Math.round(abs / 60), 'минут')
  if (abs < 86400) return future ? `${Math.round(abs / 3600)} сағаттан кейін` : say(Math.round(abs / 3600), 'сағат')
  const days = Math.round(abs / 86400)
  if (days === 1) return future ? 'ертең' : 'кеше'
  return future ? `${days} күннен кейін` : `${days} күн бұрын`
}

/** Kazakh display name of a code (via the central dictionary), otherwise a readable fallback. */
export function humanize(value: string | null | undefined): string {
  if (!value) return '-'
  const kk = label(value)
  if (kk !== value) return kk
  return value.replace(/([a-z])([A-Z])/g, '$1 $2').replace(/_/g, ' ').toLowerCase().replace(/^\p{L}/u, (c) => c.toUpperCase())
}

export function initials(name: string): string {
  return name.replace(/^(Dr\.|Др\.)\s*/, '').split(' ').filter(Boolean).slice(0, 2).map((p) => p[0]).join('').toUpperCase()
}

export function dayGroup(iso: string): string {
  const today = isoDate(0)
  const yesterday = isoDate(-1)
  const p = parts(new Date(iso), BUSINESS_TZ)
  const day = `${p.year}-${pad(p.month)}-${pad(p.day)}`
  if (day === today) return 'Бүгін'
  if (day === yesterday) return 'Кеше'
  return dateOnly(iso)
}

/** Actor shown in journals: the technical "system" account is displayed in Kazakh. */
export function actorName(actor: string | null | undefined): string {
  return !actor || actor === 'system' ? 'жүйе' : actor
}
