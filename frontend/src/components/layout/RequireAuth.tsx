import type { ReactNode } from 'react'
import { Navigate, useLocation } from 'react-router'
import { useAuth } from '../../auth/AuthContext'
import type { Role } from '../../lib/types'
import { Spinner } from '../ui/States'
import { EmptyState } from '../ui/States'
import { ShieldCheck } from 'lucide-react'
import { label } from '../../lib/i18n'

export function RequireAuth({ children, roles }: { children: ReactNode; roles?: Role[] }) {
  const { user, loading } = useAuth()
  const location = useLocation()
  if (loading) return <div className="grid min-h-screen place-items-center"><Spinner label="NEXORA іске қосылуда" /></div>
  if (!user) return <Navigate to="/login" replace state={{ from: location.pathname + location.search }} />
  if (roles && !roles.includes(user.role)) {
    return (
      <div className="card">
        <EmptyState icon={<ShieldCheck className="size-6" />} title="Бұл бөлім сіздің рөліңізге қолжетімсіз"
          description={`Қажетті рөлдер: ${roles.map(label).join(', ')}. Бұл ереже серверде де тексеріледі.`} />
      </div>
    )
  }
  return <>{children}</>
}
