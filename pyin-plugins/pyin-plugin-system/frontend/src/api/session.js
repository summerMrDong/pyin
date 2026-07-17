const TOKEN_STORAGE_KEY = 'pyin-web-shell.auth.token'

export function getStoredToken() {
  if (typeof window === 'undefined') {
    return ''
  }
  return window.localStorage.getItem(TOKEN_STORAGE_KEY) ?? ''
}

export function setStoredToken(token) {
  if (typeof window === 'undefined') {
    return
  }
  if (token) {
    window.localStorage.setItem(TOKEN_STORAGE_KEY, token)
    return
  }
  window.localStorage.removeItem(TOKEN_STORAGE_KEY)
}

export function clearStoredToken() {
  setStoredToken('')
}

export { TOKEN_STORAGE_KEY }
