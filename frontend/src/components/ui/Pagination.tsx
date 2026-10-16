import { ChevronLeft, ChevronRight } from 'lucide-react'

export function Pagination({ page, totalPages, totalElements, onPage }: {
  page: number; totalPages: number; totalElements: number; onPage: (p: number) => void
}) {
  if (totalElements === 0) return null
  return (
    <div className="flex flex-wrap items-center justify-between gap-3 border-t border-line px-5 py-3 text-xs text-muted">
      <span>Барлығы: {totalElements} &middot; {page + 1}/{Math.max(totalPages, 1)} бет</span>
      <div className="flex gap-1.5">
        <button disabled={page === 0} onClick={() => onPage(page - 1)} aria-label="Алдыңғы бет"
          className="grid size-8 place-items-center rounded-lg border border-line bg-white transition hover:text-primary disabled:opacity-40">
          <ChevronLeft className="size-4" />
        </button>
        <button disabled={page + 1 >= totalPages} onClick={() => onPage(page + 1)} aria-label="Келесі бет"
          className="grid size-8 place-items-center rounded-lg border border-line bg-white transition hover:text-primary disabled:opacity-40">
          <ChevronRight className="size-4" />
        </button>
      </div>
    </div>
  )
}
