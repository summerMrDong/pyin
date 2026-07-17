import { clearStoredToken, getStoredToken } from './session'

function buildHeaders(customHeaders = {}, hasBody = false) {
  const headers = new Headers(customHeaders)
  const token = getStoredToken()

  if (token && !headers.has('Authorization')) {
    headers.set('Authorization', token)
  }

  if (hasBody && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json')
  }

  return headers
}

async function parseJsonSafely(response) {
  const text = await response.text()
  if (!text) {
    return null
  }

  try {
    return JSON.parse(text)
  } catch {
    return { success: false, code: 'PYIN-HTTP-PARSE', message: text, data: null }
  }
}

export async function httpRequest(url, options = {}) {
  const hasJsonBody = options.body !== undefined && options.body !== null && typeof options.body !== 'string'
  const response = await fetch(url, {
    ...options,
    headers: buildHeaders(options.headers, options.body !== undefined && options.body !== null),
    body: hasJsonBody ? JSON.stringify(options.body) : options.body
  })

  const payload = await parseJsonSafely(response)

  if (response.status === 401) {
    clearStoredToken()
    if (typeof window !== 'undefined') {
      window.dispatchEvent(new CustomEvent('pyin-auth-expired'))
    }
  }

  if (!response.ok) {
    const error = new Error(payload?.message ?? `Request failed with status ${response.status}`)
    error.status = response.status
    error.payload = payload
    throw error
  }

  return payload
}
