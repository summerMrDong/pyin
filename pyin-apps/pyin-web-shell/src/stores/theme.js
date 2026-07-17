import { defineStore } from 'pinia'

const STORAGE_KEY = 'pyin-web-shell.theme'

function resolveSystemTheme() {
  if (typeof window === 'undefined' || typeof window.matchMedia !== 'function') {
    return 'light'
  }
  return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
}

export const useThemeStore = defineStore('theme', {
  state: () => ({
    preference: 'system',
    activeTheme: 'light'
  }),
  actions: {
    initialize() {
      if (typeof window === 'undefined') {
        return
      }

      const savedPreference = window.localStorage.getItem(STORAGE_KEY)
      if (savedPreference === 'light' || savedPreference === 'dark' || savedPreference === 'system') {
        this.preference = savedPreference
      }

      this.applyTheme()

      const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)')
      mediaQuery.addEventListener('change', () => {
        if (this.preference === 'system') {
          this.applyTheme()
        }
      })
    },
    toggleTheme() {
      this.preference = this.activeTheme === 'dark' ? 'light' : 'dark'
      window.localStorage.setItem(STORAGE_KEY, this.preference)
      this.applyTheme()
    },
    applyTheme() {
      this.activeTheme = this.preference === 'system' ? resolveSystemTheme() : this.preference
      document.documentElement.dataset.theme = this.activeTheme
    }
  }
})
