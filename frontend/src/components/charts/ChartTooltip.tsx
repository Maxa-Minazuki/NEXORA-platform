interface Entry { name?: string | number; value?: number | string; color?: string; dataKey?: string | number }

/** Tooltip in text tokens; the colored swatch carries series identity. */
export function ChartTooltip({ active, payload, label, labelFormatter, nameFormatter }: {
  active?: boolean; payload?: Entry[]; label?: string | number; labelFormatter?: (l: string) => string
  nameFormatter?: (n: string) => string
}) {
  if (!active || !payload?.length) return null
  return (
    <div className="rounded-xl border border-line bg-white px-3 py-2 text-xs shadow-pop">
      {label !== undefined && <p className="mb-1 font-semibold text-ink">{labelFormatter ? labelFormatter(String(label)) : label}</p>}
      {payload.map((p) => (
        <p key={String(p.dataKey ?? p.name)} className="flex items-center gap-2 text-muted">
          <span className="size-2 rounded-sm" style={{ background: p.color }} />
          <span>{nameFormatter ? nameFormatter(String(p.name)) : p.name}</span>
          <span className="ml-auto pl-3 font-semibold text-ink tabular-nums">{p.value}</span>
        </p>
      ))}
    </div>
  )
}
