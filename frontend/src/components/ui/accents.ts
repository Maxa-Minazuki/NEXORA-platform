export type Accent = 'violet' | 'sky' | 'teal' | 'mint' | 'peach' | 'rose'

/** Module color language: inventory = sky/teal, clinic = mint, flights = sky/violet, documents = peach/violet, notifications = rose. */
export const accentStyles: Record<Accent, { gradient: string; soft: string; text: string; ring: string; solid: string }> = {
  violet: { gradient: 'from-[#6C63FF] to-[#8F87FF]', soft: 'bg-primary-soft', text: 'text-primary', ring: 'ring-primary/20', solid: '#6C63FF' },
  sky: { gradient: 'from-[#4385FF] to-[#26C6B8]', soft: 'bg-sky-soft', text: 'text-sky', ring: 'ring-sky/20', solid: '#4385FF' },
  teal: { gradient: 'from-[#26C6B8] to-[#4385FF]', soft: 'bg-teal-soft', text: 'text-teal', ring: 'ring-teal/20', solid: '#26C6B8' },
  mint: { gradient: 'from-[#22B07D] to-[#5FD9B0]', soft: 'bg-mint-soft', text: 'text-mint', ring: 'ring-mint/20', solid: '#22B07D' },
  peach: { gradient: 'from-[#FFB86B] to-[#6C63FF]', soft: 'bg-peach-soft', text: 'text-[#D9822B]', ring: 'ring-peach/30', solid: '#FFB86B' },
  rose: { gradient: 'from-[#FF7FA8] to-[#4385FF]', soft: 'bg-rose-soft', text: 'text-rose', ring: 'ring-rose/20', solid: '#FF7FA8' },
}
