import { useState } from 'react'
import { Navigate, useLocation, useNavigate } from 'react-router'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { Bell, FileText, KeyRound, LogIn, Package, Plane, ShieldCheck, Stethoscope, UserRound } from 'lucide-react'
import { useAuth } from '../auth/AuthContext'
import { errorMessage } from '../lib/api'
import type { Role } from '../lib/types'
import { Button } from '../components/ui/Button'
import { Field } from '../components/ui/Field'
import { Badge, type Tone } from '../components/ui/Badge'
import { Logo } from '../components/layout/Sidebar'
import { label, t } from '../lib/i18n'
import { cx } from '../components/ui/cx'

const schema = z.object({
  username: z.string().trim().min(1, 'Пайдаланушы атын енгізіңіз').max(50, 'Тым ұзын'),
  password: z.string().min(1, 'Құпиясөзді енгізіңіз').max(100, 'Тым ұзын'),
})
type FormValues = z.infer<typeof schema>

/**
 * Local demo accounts of this educational build (also documented in the README). Clicking a row only
 * fills the form - the user still performs a real login; nothing is requested from the server.
 */
const DEMO: { username: string; password: string; role: Role; tone: Tone }[] = [
  { username: 'admin', password: 'Admin#2026', role: 'ADMIN', tone: 'violet' },
  { username: 'manager', password: 'Manager#2026', role: 'MANAGER', tone: 'peach' },
  { username: 'doctor', password: 'Doctor#2026', role: 'DOCTOR', tone: 'mint' },
  { username: 'employee', password: 'Employee#2026', role: 'EMPLOYEE', tone: 'sky' },
  { username: 'user', password: 'User#2026', role: 'USER', tone: 'rose' },
]

const modules = [
  { icon: <Package className="size-4" />, label: t('nav.inventory') },
  { icon: <Stethoscope className="size-4" />, label: t('nav.clinic') },
  { icon: <Plane className="size-4" />, label: t('nav.flights') },
  { icon: <FileText className="size-4" />, label: t('nav.documents') },
  { icon: <Bell className="size-4" />, label: t('nav.notifications') },
]

export function LoginPage() {
  const { user, login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [error, setError] = useState<string | null>(null)
  const [selected, setSelected] = useState<string | null>(null)
  const { register, handleSubmit, setValue, setFocus, formState: { errors, isSubmitting } } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { username: '', password: '' },
  })

  if (user) return <Navigate to="/" replace />
  const from = (location.state as { from?: string } | null)?.from ?? '/'

  const onSubmit = async (values: FormValues) => {
    setError(null)
    try {
      await login(values.username, values.password)
      navigate(from, { replace: true })
    } catch (e) {
      setError(errorMessage(e))
    }
  }

  const fill = (a: (typeof DEMO)[number]) => {
    setValue('username', a.username, { shouldValidate: true })
    setValue('password', a.password, { shouldValidate: true })
    setSelected(a.username)
    setError(null)
    setFocus('password')
  }

  return (
    <div className="grid min-h-screen lg:grid-cols-[1.05fr_1fr]">
      <section className="relative hidden overflow-hidden bg-gradient-to-br from-[#6C63FF] via-[#5B7BFF] to-[#26C6B8] p-12 text-white lg:flex lg:flex-col">
        <div className="absolute -top-32 -right-24 size-[28rem] rounded-full bg-white/10 blur-3xl" />
        <div className="absolute -bottom-40 -left-20 size-[26rem] rounded-full bg-[#FF7FA8]/25 blur-3xl" />
        <div className="relative flex items-center gap-3">
          <img src="/nexora.svg" alt="" className="size-11 rounded-2xl ring-2 ring-white/30" />
          <span className="font-display text-xl font-extrabold tracking-[0.18em]">NEXORA</span>
        </div>
        <div className="relative mt-auto max-w-lg">
          <h1 className="text-4xl leading-tight font-extrabold">Бір-бірімен байланысқан бизнес-үдерістер — бір платформада.</h1>
          <p className="mt-4 text-white/85">
            {t('app.tagline')}. Қор азайса — толықтыру өтінімі жасалады, брондау расталса — құжат шығады, әр шешім туралы
            тиісті адамдар дереу хабардар болады.
          </p>
          <div className="mt-8 flex flex-wrap gap-2">
            {modules.map((m) => (
              <span key={m.label} className="inline-flex items-center gap-2 rounded-full bg-white/15 px-3 py-1.5 text-sm font-semibold backdrop-blur">
                {m.icon}{m.label}
              </span>
            ))}
          </div>
        </div>
        <p className="relative mt-10 text-xs text-white/70">
          «Жобалау паттерндері» пәні бойынша оқу жобасы. Барлық деректер демонстрациялық: нақты пациенттер, төлемдер және әуе
          билеттері жоқ.
        </p>
      </section>

      <section className="flex items-center justify-center px-4 py-10 sm:px-8">
        <div className="w-full max-w-md animate-rise">
          <div className="mb-8 lg:hidden"><Logo /></div>
          <h2 className="text-3xl font-extrabold">NEXORA жүйесіне қош келдіңіз</h2>
          <p className="mt-1.5 text-sm text-muted">Барлық жұмыс үдерістерін бір платформадан басқарыңыз</p>

          <form onSubmit={handleSubmit(onSubmit)} className="mt-8 space-y-4" noValidate>
            <Field label="Пайдаланушы аты" error={errors.username?.message} htmlFor="username">
              <div className="relative">
                <UserRound className="pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-muted" />
                <input id="username" autoComplete="username" className="input pl-9" {...register('username')} />
              </div>
            </Field>
            <Field label="Құпиясөз" error={errors.password?.message} htmlFor="password">
              <div className="relative">
                <KeyRound className="pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-muted" />
                <input id="password" type="password" autoComplete="current-password" className="input pl-9" {...register('password')} />
              </div>
            </Field>
            {error && <p className="rounded-xl bg-danger-soft px-3 py-2 text-sm font-medium text-danger" role="alert">{error}</p>}
            <Button type="submit" size="lg" className="w-full" loading={isSubmitting} icon={<LogIn className="size-4" />}>Жүйеге кіру</Button>
          </form>

          <div className="mt-8 rounded-2xl border border-line bg-white p-4">
            <div className="mb-3 flex items-center gap-2 text-xs font-bold tracking-wide text-muted uppercase">
              <ShieldCheck className="size-4 text-primary" /> Демо-аккаунттар
            </div>
            <ul className="divide-y divide-line/70">
              {DEMO.map((a) => (
                <li key={a.username}>
                  <button type="button" onClick={() => fill(a)} aria-label={`${label(a.role)} аккаунтымен өрістерді толтыру`}
                    className={cx('flex w-full items-center gap-3 rounded-xl px-2 py-2 text-left transition hover:bg-primary-soft/50',
                      selected === a.username && 'bg-primary-soft/60')}>
                    <Badge tone={a.tone} className="w-28 justify-center">{label(a.role)}</Badge>
                    <code className="flex-1 truncate text-sm font-semibold text-ink">{a.username}</code>
                    <code className="text-xs text-muted">{a.password}</code>
                  </button>
                </li>
              ))}
            </ul>
            <p className="mt-3 text-xs text-muted">Жолды басқанда өрістер ғана толтырылады — кіру үшін «Жүйеге кіру» батырмасын басыңыз.</p>
          </div>
        </div>
      </section>
    </div>
  )
}
