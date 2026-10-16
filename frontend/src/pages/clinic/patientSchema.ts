import { z } from 'zod'

export const patientSchema = z.object({
  firstName: z.string().trim().min(1, 'Міндетті өріс').max(60),
  lastName: z.string().trim().min(1, 'Міндетті өріс').max(60),
  dateOfBirth: z.string().min(1, 'Міндетті өріс').refine((d) => new Date(d) < new Date(), 'Өткен күн болуы керек'),
  gender: z.enum(['FEMALE', 'MALE', 'OTHER']),
  phone: z.string().max(40).regex(/^[+0-9 ()-]*$/, 'Тек цифрлар, бос орын және + ( ) - белгілері').optional(),
  email: z.union([z.literal(''), z.email('E-mail қате').max(120)]).optional(),
  notes: z.string().max(1000).optional(),
})
