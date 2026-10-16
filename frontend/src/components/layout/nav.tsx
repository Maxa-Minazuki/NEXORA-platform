import { Bell, FileText, FlaskConical, LayoutDashboard, Package, Plane, ScrollText, Settings, Stethoscope } from 'lucide-react'
import type { ReactNode } from 'react'
import type { Role } from '../../lib/types'
import { t, type MessageKey } from '../../lib/i18n'
import type { Accent } from '../ui/accents'

export interface NavItem {
  to: string
  key: MessageKey
  icon: ReactNode
  accent: Accent
  roles?: Role[]
}

export const NAV: NavItem[] = [
  { to: '/', key: 'nav.overview', icon: <LayoutDashboard className="size-[18px]" />, accent: 'violet' },
  { to: '/inventory', key: 'nav.inventory', icon: <Package className="size-[18px]" />, accent: 'sky', roles: ['ADMIN', 'MANAGER', 'EMPLOYEE'] },
  { to: '/clinic', key: 'nav.clinic', icon: <Stethoscope className="size-[18px]" />, accent: 'mint' },
  { to: '/flights', key: 'nav.flights', icon: <Plane className="size-[18px]" />, accent: 'sky' },
  { to: '/documents', key: 'nav.documents', icon: <FileText className="size-[18px]" />, accent: 'peach' },
  { to: '/notifications', key: 'nav.notifications', icon: <Bell className="size-[18px]" />, accent: 'rose' },
  { to: '/audit', key: 'nav.audit', icon: <ScrollText className="size-[18px]" />, accent: 'violet', roles: ['ADMIN', 'MANAGER'] },
  { to: '/lab', key: 'nav.lab', icon: <FlaskConical className="size-[18px]" />, accent: 'teal' },
  { to: '/settings', key: 'nav.settings', icon: <Settings className="size-[18px]" />, accent: 'violet' },
]

export function navLabel(item: NavItem) {
  return t(item.key)
}
