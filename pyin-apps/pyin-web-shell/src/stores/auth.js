import { defineStore } from 'pinia'
import { fetchCurrentUserApi, loginApi, logoutApi } from '../api/auth'
import { clearStoredToken, getStoredToken, setStoredToken } from '../api/session'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: '',
    currentUser: null,
    initializing: true
  }),
  getters: {
    isAuthenticated(state) {
      return Boolean(state.token && state.currentUser)
    }
  },
  actions: {
    async initialize() {
      this.token = getStoredToken()
      if (!this.token) {
        this.initializing = false
        return
      }

      try {
        await this.refreshCurrentUser()
      } catch {
        this.clearAuth()
      } finally {
        this.initializing = false
      }
    },
    async login(form) {
      const result = await loginApi(form)
      const token = result?.data?.token ?? ''
      setStoredToken(token)
      this.token = token
      await this.refreshCurrentUser()
      return result?.data
    },
    async refreshCurrentUser() {
      const result = await fetchCurrentUserApi()
      this.currentUser = result?.data ?? null
      return this.currentUser
    },
    async logout() {
      try {
        if (this.token) {
          await logoutApi()
        }
      } finally {
        this.clearAuth()
      }
    },
    clearAuth() {
      clearStoredToken()
      this.token = ''
      this.currentUser = null
    }
  }
})
