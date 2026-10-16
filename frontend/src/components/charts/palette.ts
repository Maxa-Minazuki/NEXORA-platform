/**
 * Chart colors: deeper steps of the brand hues, validated with the dataviz palette checker
 * (lightness band, chroma, colour-blind separation of adjacent slots, 3:1 contrast on white).
 * Always assigned in this fixed order - never cycled.
 */
export const SERIES = ['#2F6FE0', '#C2691A', '#0E9F92', '#6C63FF', '#D1497C'] as const

export const SEVERITY_COLORS = { info: '#2F6FE0', success: '#0E9F92', warning: '#C2691A', critical: '#B4235A' } as const

/** Fixed color per status entity, so a status keeps its color when others disappear. */
export const STATUS_COLORS: Record<string, string> = {
  CONFIRMED: SERIES[2], PENDING: SERIES[1], CANCELLED: '#8A90A6', EXPIRED: SERIES[4],
  DRAFT: '#8A90A6', PENDING_APPROVAL: SERIES[1], IN_REVIEW: SERIES[0], APPROVED: SERIES[2], REJECTED: SERIES[4],
}

export const AXIS = { stroke: '#C9CDDD', tick: { fill: '#7B8198', fontSize: 11 } }
export const GRID = '#EEF0F7'
