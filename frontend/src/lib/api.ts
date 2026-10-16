import { errorText, fieldLabel } from './i18n'

/**
 * Central API client. Uses the session cookie set by the backend and echoes the XSRF-TOKEN cookie
 * in the X-XSRF-TOKEN header (Spring Security double-submit CSRF protection).
 */
export interface FieldError { field: string; message: string }

export class ApiError extends Error {
  readonly status: number
  readonly code: string
  readonly fieldErrors: FieldError[]

  constructor(status: number, code: string, message: string, fieldErrors: FieldError[] = []) {
    super(message)
    this.status = status
    this.code = code
    this.fieldErrors = fieldErrors
  }
}

type Query = Record<string, string | number | boolean | null | undefined>

function readCookie(name: string): string | null {
  const match = document.cookie.split('; ').find((c) => c.startsWith(name + '='))
  return match ? decodeURIComponent(match.split('=')[1]) : null
}

export function buildUrl(path: string, query?: Query): string {
  const url = path.startsWith('/api') ? path : '/api' + path
  if (!query) return url
  const params = new URLSearchParams()
  for (const [k, v] of Object.entries(query)) {
    if (v !== undefined && v !== null && v !== '') params.set(k, String(v))
  }
  const qs = params.toString()
  return qs ? `${url}?${qs}` : url
}

let unauthorizedHandler: (() => void) | null = null
export function onUnauthorized(handler: () => void) {
  unauthorizedHandler = handler
}

export async function ensureCsrf(): Promise<void> {
  await fetch('/api/auth/csrf', { credentials: 'same-origin' })
}

async function request<T>(method: string, path: string, body?: unknown, query?: Query, retried = false): Promise<T> {
  const headers: Record<string, string> = { Accept: 'application/json' }
  if (body !== undefined) headers['Content-Type'] = 'application/json'
  if (method !== 'GET') {
    if (!readCookie('XSRF-TOKEN')) await ensureCsrf()
    const token = readCookie('XSRF-TOKEN')
    if (token) headers['X-XSRF-TOKEN'] = token
  }
  let res: Response
  try {
    res = await fetch(buildUrl(path, query), {
      method,
      headers,
      credentials: 'same-origin',
      body: body === undefined ? undefined : JSON.stringify(body),
    })
  } catch {
    throw new ApiError(0, 'NETWORK', errorText('NETWORK'))
  }
  if (res.status === 204) return undefined as T
  const text = await res.text()
  const data = text ? safeJson(text) : null
  if (!res.ok) {
    // A rotated or missing CSRF token: refresh it once and retry.
    if (res.status === 403 && method !== 'GET' && !retried && String(data?.message ?? '').includes('CSRF')) {
      await ensureCsrf()
      return request<T>(method, path, body, query, true)
    }
    if (res.status === 401 && !path.includes('/auth/')) unauthorizedHandler?.()
    throw new ApiError(res.status, data?.code ?? 'HTTP_' + res.status, data?.message ?? res.statusText, data?.fieldErrors ?? [])
  }
  return data as T
}

function safeJson(text: string) {
  try {
    return JSON.parse(text)
  } catch {
    return { message: text }
  }
}

export const api = {
  get: <T>(path: string, query?: Query) => request<T>('GET', path, undefined, query),
  post: <T>(path: string, body?: unknown, query?: Query) => request<T>('POST', path, body, query),
  put: <T>(path: string, body?: unknown) => request<T>('PUT', path, body),
  patch: <T>(path: string, body?: unknown, query?: Query) => request<T>('PATCH', path, body, query),
}

export function errorMessage(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.fieldErrors.length) {
      return error.fieldErrors.map((f) => `${fieldLabel(f.field)}: ${f.message}`).join('; ')
    }
    return errorText(error.code, error.message)
  }
  return error instanceof Error ? error.message : 'Күтпеген қате'
}
