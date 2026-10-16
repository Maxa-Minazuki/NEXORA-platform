import { initials } from '../../lib/format'
import { cx } from './cx'

const palette = ['from-[#6C63FF] to-[#8F87FF]', 'from-[#4385FF] to-[#26C6B8]', 'from-[#FF7FA8] to-[#FFB86B]',
  'from-[#22B07D] to-[#26C6B8]', 'from-[#FFB86B] to-[#FF7FA8]']

export function Avatar({ name, size = 'md' }: { name: string; size?: 'sm' | 'md' | 'lg' }) {
  const idx = [...name].reduce((s, c) => s + c.charCodeAt(0), 0) % palette.length
  const dims = { sm: 'size-7 text-[10px]', md: 'size-9 text-xs', lg: 'size-12 text-sm' }[size]
  return (
    <span className={cx('grid shrink-0 place-items-center rounded-full bg-gradient-to-br font-bold text-white', palette[idx], dims)}>
      {initials(name)}
    </span>
  )
}
