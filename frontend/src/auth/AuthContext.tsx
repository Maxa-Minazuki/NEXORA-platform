import { createContext, useCallback, useContext, useEffect, useMemo, type ReactNode } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { api, ApiError, ensureCsrf, onUnauthorized } from '../lib/api'
import type { Role, UserProfile } from '../lib/types'
import { hasRole } from '../lib/roles'

interface AuthState {
  user: UserProfile | null
  loading: boolean
  login: (username: string, password: string) => Promise<UserProfile>
  logout: () => Promise<void>
  can: (...roles: Role[]) => boolean
}

const AuthContext = createContext<AuthState | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient()
  const me = useQuery({
    queryKey: ['me'],
    queryFn: async () => {
      try {
        return await api.get<UserProfile>('/auth/me')
      } catch (e) {
        if (e instanceof ApiError && e.status === 401) return null
        throw e
      }
    },
    staleTime: Infinity,
    retry: false,
  })

  useEffect(() => {
    onUnauthorized(() => queryClient.setQueryData(['me'], null))
  }, [queryClient])

  /**
   * Drops every cached query of the previous user EXCEPT ['me']. The provider is subscribed to the
   * ['me'] query: removing it (queryClient.clear()) would detach that observer, so a later
   * setQueryData would never reach it and the app would bounce back to the login page.
   */
  const resetUserData = useCallback((profile: UserProfile | null) => {
    queryClient.removeQueries({ predicate: (q) => q.queryKey[0] !== 'me' })
    queryClient.setQueryData(['me'], profile)
  }, [queryClient])

  const login = useCallback(async (username: string, password: string) => {
    await ensureCsrf()
    const profile = await api.post<UserProfile>('/auth/login', { username, password })
    await ensureCsrf() // the token is rotated on login
    resetUserData(profile)
    return profile
  }, [resetUserData])

  const logout = useCallback(async () => {
    try {
      await api.post('/auth/logout')
    } finally {
      resetUserData(null)
      await ensureCsrf()
    }
  }, [resetUserData])

  const value = useMemo<AuthState>(() => ({
    user: me.data ?? null,
    loading: me.isLoading,
    login,
    logout,
    can: (...roles: Role[]) => hasRole(me.data?.role, roles),
  }), [me.data, me.isLoading, login, logout])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

// eslint-disable-next-line react-refresh/only-export-components
export function useAuth(): AuthState {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used inside AuthProvider')
  return ctx
}
