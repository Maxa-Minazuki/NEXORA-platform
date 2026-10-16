import { describe, expect, it } from 'vitest'
import { productSchema } from './productSchema'

const valid = {
  sku: 'TEST-1', name: 'Test', description: '', categoryId: '1', supplierId: '', purchasePrice: '100.00',
  retailPrice: '150.00', quantity: '5', minStock: '2', unit: 'pcs', replenishmentStrategy: 'FIXED_LOT' as const,
}

describe('product form validation', () => {
  it('accepts a valid product and coerces numbers', () => {
    const r = productSchema.safeParse(valid)
    expect(r.success).toBe(true)
    expect(r.data?.quantity).toBe(5)
  })
  it('rejects retail price below purchase price', () => {
    const r = productSchema.safeParse({ ...valid, retailPrice: '50.00' })
    expect(r.success).toBe(false)
    expect(r.error?.issues[0].path).toEqual(['retailPrice'])
  })
  it('rejects invalid SKU characters and negative stock', () => {
    const r = productSchema.safeParse({ ...valid, sku: 'bad sku!', quantity: '-3' })
    expect(r.success).toBe(false)
    const paths = r.error!.issues.map((i) => i.path[0])
    expect(paths).toContain('sku')
    expect(paths).toContain('quantity')
  })
})
