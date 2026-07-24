const TOKEN_STORAGE_KEY = 'pyin-web-shell.auth.token'

export async function request<T>(url: string, options: Omit<RequestInit, 'body'> & { body?: any } = {}): Promise<T> {
  const headers = new Headers(options.headers)
  const token = window.localStorage.getItem(TOKEN_STORAGE_KEY)
  if (token && !headers.has('Authorization')) headers.set('Authorization', token)
  const json = options.body && !(options.body instanceof FormData) && typeof options.body !== 'string'
  if (json) headers.set('Content-Type', 'application/json')
  const response = await fetch(url, { ...options, headers, body: json ? JSON.stringify(options.body) : options.body })
  const payload = await response.json().catch(() => null)
  if (!response.ok || !payload?.success) throw new Error(payload?.message || '导出工坊请求失败')
  return payload.data as T
}
