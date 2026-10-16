import { afterEach, describe, expect, it, vi } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { AuthProvider } from '../auth/AuthContext'
import { ToastProvider } from '../components/ui/Toast'
import { LoginPage } from './LoginPage'

function renderLogin(fetchImpl: (url: string, init?: RequestInit) => Promise<Response>) {
  const fetchMock = vi.fn(fetchImpl)
  vi.stubGlobal('fetch', fetchMock)
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  render(
    <QueryClientProvider client={qc}>
      <MemoryRouter initialEntries={['/login']}>
        <ToastProvider><AuthProvider><LoginPage /></AuthProvider></ToastProvider>
      </MemoryRouter>
    </QueryClientProvider>,
  )
  return fetchMock
}

const json = (body: unknown, status = 200) => Promise.resolve(new Response(JSON.stringify(body), { status }))
const anonymous = (url: string) =>
  url.includes('/auth/csrf') ? json({ headerName: 'X-XSRF-TOKEN', token: 't' }) : json({ code: 'UNAUTHENTICATED' }, 401)
const calledLogin = (m: ReturnType<typeof vi.fn>) => m.mock.calls.some(([u]) => String(u).includes('/auth/login'))

afterEach(() => vi.unstubAllGlobals())

describe('LoginPage (қазақша)', () => {
  it('shows the Kazakh welcome and the five demo roles', async () => {
    renderLogin(anonymous)
    expect(await screen.findByText('NEXORA жүйесіне қош келдіңіз')).toBeInTheDocument()
    for (const role of ['Әкімші', 'Менеджер', 'Дәрігер', 'Қызметкер', 'Пайдаланушы']) {
      expect(screen.getByText(role)).toBeInTheDocument()
    }
  })

  it('validates required fields in Kazakh before calling the API', async () => {
    const fetchMock = renderLogin(anonymous)
    await userEvent.click(await screen.findByRole('button', { name: /жүйеге кіру/i }))
    expect(await screen.findByText('Пайдаланушы атын енгізіңіз')).toBeInTheDocument()
    expect(screen.getByText('Құпиясөзді енгізіңіз')).toBeInTheDocument()
    expect(calledLogin(fetchMock)).toBe(false)
  })

  it('demo row only fills the fields and never logs in by itself', async () => {
    const fetchMock = renderLogin(anonymous)
    await userEvent.click(await screen.findByRole('button', { name: /Менеджер аккаунтымен/ }))
    expect(screen.getByLabelText('Пайдаланушы аты')).toHaveValue('manager')
    expect(screen.getByLabelText('Құпиясөз')).toHaveValue('Manager#2026')
    expect(calledLogin(fetchMock)).toBe(false)
  })

  it('shows the Kazakh server message for wrong credentials', async () => {
    renderLogin((url) => {
      if (url.includes('/auth/login')) return json({ code: 'BAD_CREDENTIALS', message: 'Пайдаланушы аты немесе құпиясөз қате' }, 401)
      return anonymous(url)
    })
    await userEvent.type(await screen.findByLabelText('Пайдаланушы аты'), 'manager')
    await userEvent.type(screen.getByLabelText('Құпиясөз'), 'wrong')
    await userEvent.click(screen.getByRole('button', { name: /жүйеге кіру/i }))
    await waitFor(() => expect(screen.getByRole('alert')).toHaveTextContent('Пайдаланушы аты немесе құпиясөз қате'))
  })
})
