import { z } from 'zod'

const money = z.string().trim().regex(/^\d{1,10}(\.\d{1,2})?$/, 'Ең көбі 2 ондық белгі')

export const productSchema = z.object({
  sku: z.string().trim().min(1, 'Міндетті өріс').max(40, 'Тым ұзын').regex(/^[A-Za-z0-9-]+$/, 'Тек латын әріптері, цифрлар және сызықша'),
  name: z.string().trim().min(1, 'Міндетті өріс').max(120, 'Тым ұзын'),
  description: z.string().max(1000).optional(),
  categoryId: z.string().min(1, 'Санатты таңдаңыз'),
  supplierId: z.string().optional(),
  purchasePrice: money,
  retailPrice: money,
  quantity: z.coerce.number<string>().int('Бүтін сан болуы керек').min(0, 'Теріс болмауы керек').max(1_000_000, 'Тым үлкен'),
  minStock: z.coerce.number<string>().int('Бүтін сан болуы керек').min(0, 'Теріс болмауы керек').max(100_000, 'Тым үлкен'),
  unit: z.string().trim().min(1, 'Міндетті өріс').max(20, 'Тым ұзын'),
  replenishmentStrategy: z.enum(['TOP_UP_TO_TARGET', 'FIXED_LOT', 'DEMAND_BASED']),
}).refine((v) => Number(v.retailPrice) >= Number(v.purchasePrice), { path: ['retailPrice'], message: 'Бөлшек баға сатып алу бағасынан төмен болмауы керек' })
