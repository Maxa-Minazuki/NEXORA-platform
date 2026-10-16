import { Link } from 'react-router'
import { Compass } from 'lucide-react'
import { EmptyState } from '../components/ui/States'
import { Button } from '../components/ui/Button'

export function NotFoundPage() {
  return (
    <div className="card">
      <EmptyState icon={<Compass className="size-6" />} title="Бет табылмады"
        description="Іздеген бетіңіз жоқ немесе басқа орынға көшірілген."
        action={<Link to="/"><Button>Басқару панеліне оралу</Button></Link>} />
    </div>
  )
}
