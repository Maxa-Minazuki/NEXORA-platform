import { forwardRef, type ButtonHTMLAttributes, type ReactNode } from 'react'
import { LoaderCircle } from 'lucide-react'
import { cx } from './cx'

type Variant = 'primary' | 'secondary' | 'ghost' | 'danger' | 'soft' | 'success'
type Size = 'sm' | 'md' | 'lg'

const variants: Record<Variant, string> = {
  primary: 'bg-primary text-white shadow-[0_8px_20px_-8px_rgba(108,99,255,0.7)] hover:bg-primary-strong',
  secondary: 'bg-white text-ink border border-line hover:border-primary/40 hover:text-primary',
  ghost: 'text-muted hover:bg-primary-soft hover:text-primary',
  danger: 'bg-danger text-white hover:bg-danger/90 shadow-[0_8px_20px_-10px_rgba(229,72,77,0.8)]',
  soft: 'bg-primary-soft text-primary hover:bg-primary/15',
  success: 'bg-mint text-white hover:bg-mint/90 shadow-[0_8px_20px_-10px_rgba(34,176,125,0.8)]',
}

const sizes: Record<Size, string> = {
  sm: 'h-8 px-3 text-xs gap-1.5 rounded-lg',
  md: 'h-10 px-4 text-sm gap-2 rounded-xl',
  lg: 'h-12 px-5 text-sm gap-2 rounded-xl',
}

export interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: Variant
  size?: Size
  loading?: boolean
  icon?: ReactNode
}

export const Button = forwardRef<HTMLButtonElement, ButtonProps>(function Button(
  { variant = 'primary', size = 'md', loading, icon, className, children, disabled, type = 'button', ...rest }, ref,
) {
  return (
    <button
      ref={ref}
      type={type}
      disabled={disabled || loading}
      className={cx(
        'inline-flex shrink-0 items-center justify-center font-semibold transition duration-150 active:scale-[0.98]',
        'disabled:cursor-not-allowed disabled:opacity-55 disabled:active:scale-100',
        variants[variant], sizes[size], className,
      )}
      {...rest}
    >
      {loading ? <LoaderCircle className="size-4 animate-spin" aria-hidden /> : icon}
      {children}
    </button>
  )
})
