const TOKEN_STORAGE_KEY = 'pyin-web-shell.auth.token'

export async function requestJson(url, options = {}) {
  const headers = new Headers(options.headers)
  const token = window.localStorage.getItem(TOKEN_STORAGE_KEY)
  if (token && !headers.has('Authorization')) headers.set('Authorization', token)
  if (options.body !== undefined && !headers.has('Content-Type')) headers.set('Content-Type', 'application/json')
  const response = await fetch(url, { ...options, headers, body: options.body === undefined ? undefined : JSON.stringify(options.body) })
  const payload = await response.json().catch(() => null)
  if (!response.ok || !payload?.success) throw new Error(payload?.message || '状态机插件请求失败')
  return payload.data
}
