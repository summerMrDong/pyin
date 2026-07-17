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
    throw new Error(result.message || '字典插件请求失败')
  }
  return result.data
}

export function fetchTypes() {
  return requestJson('/api/plugins/dict/admin/dict/types')
}

export function saveType(payload) {
  return requestJson('/api/plugins/dict/admin/dict/types', {
    method: 'POST',
    body: JSON.stringify(payload)
  })
}

export function deleteType(id) {
  return requestJson(`/api/plugins/dict/admin/dict/types/${id}`, {
    method: 'DELETE'
  })
}

export function fetchItems(typeId) {
  const suffix = typeId ? `?typeId=${encodeURIComponent(typeId)}` : ''
  return requestJson(`/api/plugins/dict/admin/dict/items${suffix}`)
}

export function fetchItemDetail(id) {
  return requestJson(`/api/plugins/dict/admin/dict/items/${id}`)
}

export function saveItem(payload) {
  return requestJson('/api/plugins/dict/admin/dict/items', {
    method: 'POST',
    body: JSON.stringify(payload)
  })
}

export function deleteItem(id) {
  return requestJson(`/api/plugins/dict/admin/dict/items/${id}`, {
    method: 'DELETE'
  })
}
