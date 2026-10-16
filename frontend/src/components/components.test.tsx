import { describe, expect, it } from 'vitest'
import { render, screen } from '@testing-library/react'
import { StatusBadge } from './ui/Badge'
import { ChainPreview } from '../pages/documents/ChainPreview'
import { WorkflowTrack } from '../pages/documents/WorkflowTrack'
import { EmptyState } from './ui/States'

describe('UI components', () => {
  it('renders a humanized status badge', () => {
    render(<StatusBadge status="PENDING_APPROVAL" />)
    expect(screen.getByText('Бекітуді күтуде')).toBeInTheDocument()
  })

  it('shows where the chain of responsibility stopped', () => {
    render(<ChainPreview result={{
      passed: false, failure: '«Сатып алуға өтінім» үшін сома міндетті', decision: null,
      trace: [{ handler: 'RequiredFieldsHandler', outcome: 'FAILED', message: '«Сатып алуға өтінім» үшін сома міндетті' }],
      route: [], handlers: ['RequiredFieldsHandler', 'DataIntegrityHandler'],
    }} />)
    expect(screen.getByText('Тоқтады: «Сатып алуға өтінім» үшін сома міндетті')).toBeInTheDocument()
    expect(screen.getByText(/жетпеді/)).toBeInTheDocument()
  })

  it('renders the approval track from real step data', () => {
    render(<WorkflowTrack items={[
      { key: 'created', label: 'Жасалды', state: 'DONE', actor: 'Тимур', at: '2026-10-16T05:00:00Z', note: null },
      { key: 'step-1', label: 'Менеджер тексеруі', state: 'CURRENT', actor: 'Менеджер', at: null, note: null },
      { key: 'completed', label: 'Аяқталды', state: 'PENDING', actor: null, at: null, note: null },
    ]} />)
    expect(screen.getByText('Менеджер тексеруі')).toBeInTheDocument()
    expect(screen.getAllByRole('listitem')).toHaveLength(3)
  })

  it('renders an empty state with its action', () => {
    render(<EmptyState title="Деректер жоқ" description="Әзірге бос" action={<button>Қосу</button>} />)
    expect(screen.getByRole('button', { name: 'Қосу' })).toBeInTheDocument()
  })
})
