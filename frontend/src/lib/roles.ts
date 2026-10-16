import type { Role } from './types'

export const STAFF: Role[] = ['ADMIN', 'MANAGER', 'EMPLOYEE']
export const APPROVERS: Role[] = ['ADMIN', 'MANAGER']
export const AUTHORS: Role[] = ['ADMIN', 'MANAGER', 'EMPLOYEE', 'DOCTOR']

export function hasRole(role: Role | undefined, allowed: Role[]): boolean {
  return !!role && allowed.includes(role)
}
