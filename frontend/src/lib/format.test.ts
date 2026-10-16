import { describe, expect, it } from 'vitest'
import { compactMoney, dateTime, duration, humanize, initials, longDate, money, relative, shortDay, weekdayDate } from './format'
import { errorText, fieldLabel, label, t } from './i18n'

describe('kk-KZ formatting', () => {
  it('formats tenge amounts with Kazakh grouping', () => {
    expect(money(2272000)).toBe('2 272 000 ₸')
    expect(money(null)).toBe('-')
  })
  it('formats flight durations in Kazakh', () => {
    expect(duration(105)).toBe('1 сағ 45 мин')
    expect(duration(45)).toBe('45 мин')
  })
  it('uses Kazakh month names', () => {
    expect(shortDay('2026-10-16')).toMatch(/^16 қаз/)
  })
  it('formats dates in Kazakh without relying on browser ICU data', () => {
    const iso = '2026-10-16T04:30:00Z' // 09:30 in Asia/Almaty (UTC+5)
    expect(weekdayDate(iso)).toBe('жм, 16 қаз.')
    expect(dateTime(iso)).toBe('16 қаз., 09:30')
    expect(longDate(new Date(iso))).toBe('Жұма, 16 қазан 2026 ж.')
    expect(compactMoney(7592300)).toBe('7,6 млн ₸')
    expect(relative(new Date(Date.now() - 3 * 3600_000).toISOString())).toBe('3 сағат бұрын')
    expect(relative(new Date(Date.now() - 26 * 3600_000).toISOString())).toBe('кеше')
  })
  it('builds initials without a title', () => {
    expect(initials('Айгерім Нұрланова')).toBe('АН')
  })
})

describe('central Kazakh dictionary', () => {
  it('translates internal codes but keeps unknown codes', () => {
    expect(humanize('PENDING_APPROVAL')).toBe('Бекітуді күтуде')
    expect(label('CANCELLED')).toBe('Болдырылмаған')
    expect(label('ADMIN')).toBe('Әкімші')
    expect(label('EVENT_LowStockEvent')).toBe('Қор аз')
    expect(label('SOMETHING_NEW')).toBe('SOMETHING_NEW')
  })
  it('names navigation items in Kazakh', () => {
    expect(t('nav.inventory')).toBe('Қойма')
    expect(t('nav.audit')).toBe('Әрекеттер журналы')
  })
  it('prefers the (already Kazakh) server message and falls back to the table', () => {
    expect(errorText('NETWORK')).toContain('серверіне')
    expect(errorText('BAD_CREDENTIALS', 'Сервер хабары')).toBe('Сервер хабары')
    expect(fieldLabel('reason')).toBe('Себебі')
  })
})
