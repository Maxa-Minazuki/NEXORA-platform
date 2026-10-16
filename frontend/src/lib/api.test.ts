import { afterEach, describe, expect, it, vi } from 'vitest'
import { api, ApiError, buildUrl, errorMessage } from './api'

afterEach(() => {
  vi.unstubAllGlobals()
  document.cookie = 'XSRF-TOKEN=; expires=Thu, 01 Jan 1970 00:00:00 GMT'
})

describe('api client', () => {
  it('builds URLs and drops empty query values', () => {
    expect(buildUrl('/inventory/products', { q: 'gloves', categoryId: '', page: 0, x: undefined })).toBe('/api/inventory/products?q=gloves&page=0')
  })

  it('sends the CSRF cookie value as X-XSRF-TOKEN on mutations', async () => {
    document.cookie = 'XSRF-TOKEN=abc123'
    const fetchMock = vi.fn().mockResolvedValue(new Response(JSON.stringify({ ok: true }), { status: 200 }))
    vi.stubGlobal('fetch', fetchMock)
    await api.post('/documents', { title: 'x' })
    const [, init] = fetchMock.mock.calls[0]
    expect(init.headers['X-XSRF-TOKEN']).toBe('abc123')
    expect(init.credentials).toBe('same-origin')
    expect(init.body).toBe('{"title":"x"}')
  })

  it('turns the backend error body into an ApiError with field errors', async () => {
    const body = { code: 'VALIDATION_FAILED', message: 'Request validation failed', fieldErrors: [{ field: 'reason', message: 'бос болмауы керек' }] }
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(JSON.stringify(body), { status: 400 })))
    const error = (await api.get('/anything').catch((e) => e)) as ApiError
    expect(error).toBeInstanceOf(ApiError)
    expect(error.status).toBe(400)
    expect(error.code).toBe('VALIDATION_FAILED')
    expect(errorMessage(error)).toBe('Себебі: бос болмауы керек')
  })

  it('reports an unreachable server clearly', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new TypeError('Failed to fetch')))
    const error = (await api.get('/dashboard').catch((e) => e)) as ApiError
    expect(error.code).toBe('NETWORK')
  })
})
