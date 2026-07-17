async function requestJson(url, options = {}) {
  const response = await fetch(url, {
    headers: {
      'Content-Type': 'application/json',
      ...(options.headers ?? {})
    },
    ...options
  })
  const result = await response.json()
  if (!response.ok || !result.success) {
    throw new Error(result.message || '配置插件请求失败')
  }
  return result.data
}

export function fetchNamespaces() {
  return requestJson('/api/plugins/config/admin/config/namespaces')
}

export function saveNamespace(payload) {
  return requestJson('/api/plugins/config/admin/config/namespaces', {
    method: 'POST',
    body: JSON.stringify(payload)
  })
}

export function deleteNamespace(id) {
  return requestJson(`/api/plugins/config/admin/config/namespaces/${id}`, {
    method: 'DELETE'
  })
}

export function fetchItems(namespaceId, keyword) {
  const params = new URLSearchParams()
  if (namespaceId) {
    params.set('namespaceId', String(namespaceId))
  }
  if (keyword) {
    params.set('keyword', keyword)
  }
  const suffix = params.toString()
  return requestJson(`/api/plugins/config/admin/config/items${suffix ? `?${suffix}` : ''}`)
}

export function fetchItemDetail(id) {
  return requestJson(`/api/plugins/config/admin/config/items/${id}`)
}

export function saveItem(payload) {
  return requestJson('/api/plugins/config/admin/config/items', {
    method: 'POST',
    body: JSON.stringify(payload)
  })
}

export function deleteItem(id) {
  return requestJson(`/api/plugins/config/admin/config/items/${id}`, {
    method: 'DELETE'
  })
}
