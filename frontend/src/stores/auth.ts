import { defineStore } from 'pinia'
import { getCurrentProfile, login, logout, register } from '../api/community'
import { ApiError, clearCsrfResponse } from '../api/http'
import type { UserProfile } from '../types'

function readProfile(): UserProfile | null {
  localStorage.removeItem('community.accessToken')
  const stored = localStorage.getItem('community.profile')
  if (!stored) return null
  try {
    return JSON.parse(stored) as UserProfile
  } catch {
    localStorage.removeItem('community.profile')
    return null
  }
}

export const useAuthStore = defineStore('auth', {
  state: () => ({
    profile: readProfile(),
    sessionInitialized: false,
    sessionError: '',
  }),
  getters: {
    isAuthenticated: (state) => Boolean(state.profile),
  },
  actions: {
    async signIn(username: string, password: string) {
      const session = await login({ username, password })
      this.setProfile(session.user)
      this.sessionInitialized = true
      this.sessionError = ''
    },
    async signUp(input: {
      username: string
      email: string
      password: string
      displayName: string
    }) {
      const session = await register(input)
      this.setProfile(session.user)
      this.sessionInitialized = true
      this.sessionError = ''
    },
    async restoreSession() {
      if (this.sessionInitialized) return
      this.sessionError = ''
      try {
        this.setProfile(await getCurrentProfile())
      } catch (cause) {
        if (cause instanceof ApiError && cause.status === 401) {
          this.clearSession()
        } else {
          this.sessionError = cause instanceof ApiError
            ? cause.message
            : '无法确认登录状态，请稍后重试。'
        }
      } finally {
        this.sessionInitialized = true
      }
    },
    setProfile(profile: UserProfile) {
      this.profile = profile
      localStorage.setItem('community.profile', JSON.stringify(profile))
    },
    updateProfile(profile: UserProfile) {
      this.setProfile(profile)
    },
    async signOut() {
      await logout()
      this.clearSession()
      clearCsrfResponse()
    },
    clearSession() {
      this.profile = null
      this.sessionInitialized = true
      this.sessionError = ''
      localStorage.removeItem('community.profile')
      localStorage.removeItem('community.accessToken')
    },
  },
})
