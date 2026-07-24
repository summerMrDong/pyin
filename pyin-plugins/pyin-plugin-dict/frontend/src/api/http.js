const TOKEN_STORAGE_KEY = 'pyin-web-shell.auth.token'

function clearStoredToken() {
  if (typeof window !== 'undefined') window.localStorage.removeItem(TOKEN_STORAGE_KEY)
}

function isJsonBody(body) {
  return body !== undefined && body !== null && typeof body !== 'string' && !(typeof FormData !== 'undefined' && body instanceof FormData)
}

async function parseJsonSafely(response) {
  const text = await response.text()
  if (!text) return null
  try {
    return JSON.parse(text)
  } catch {
    return { success: false, code: 'PYIN-HTTP-PARSE', message: text, data: null }
  }
}

export async function requestJson(url, options = {}) {
  const jsonBody = isJsonBody(options.body)
  const headers = new Headers(options.headers)
  const token = typeof window === 'undefined' ? '' : window.localStorage.getItem(TOKEN_STORAGE_KEY)
  if (token && !headers.has('Authorization')) headers.set('Authorization', token)
  if (jsonBody && !headers.has('Content-Type')) headers.set('Content-Type', 'application/json')

  const response = await fetch(url, {
    ...options,
    headers,
    body: jsonBody ? JSON.stringify(options.body) : options.body
  })
  const result = await parseJsonSafely(response)
  if (response.status === 401) {
    clearStoredToken()
    if (typeof window !== 'undefined') window.dispatchEvent(new CustomEvent('pyin-auth-expired'))
  }
  if (!response.ok || !result?.success) {
    const error = new Error(result?.message || '字典插件请求失败')
    error.status = response.status
    error.payload = result
    throw error
  }
  return result.data
}
