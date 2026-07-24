import { requestJson } from './http'

export function fetchNamespaces() { return requestJson('/plugins/config/admin/config/namespaces') }
export function saveNamespace(payload) { return requestJson('/plugins/config/admin/config/namespaces', { method: 'POST', body: payload }) }
export function deleteNamespace(id) { return requestJson(`/plugins/config/admin/config/namespaces/${id}`, { method: 'DELETE' }) }
export function fetchDirectoryTree(namespaceId) { return requestJson(`/plugins/config/admin/config/namespaces/${namespaceId}/directories/tree`) }
export function saveDirectory(payload) { return requestJson('/plugins/config/admin/config/directories', { method: 'POST', body: payload }) }
export function updateDirectory(id, payload) { return requestJson(`/plugins/config/admin/config/directories/${id}`, { method: 'PUT', body: payload }) }
export function moveDirectory(id, payload) { return requestJson(`/plugins/config/admin/config/directories/${id}/move`, { method: 'PUT', body: payload }) }
export function deleteDirectory(id) { return requestJson(`/plugins/config/admin/config/directories/${id}`, { method: 'DELETE' }) }

export function fetchItems(namespaceId, keyword, directoryId) {
  const params = new URLSearchParams()
  if (namespaceId) params.set('namespaceId', String(namespaceId))
  if (keyword) params.set('keyword', keyword)
  if (directoryId) params.set('directoryId', String(directoryId))
  const suffix = params.toString()
  return requestJson(`/plugins/config/admin/config/items${suffix ? `?${suffix}` : ''}`)
}

export function fetchItemDetail(id) { return requestJson(`/plugins/config/admin/config/items/${id}`) }
export function saveItem(payload) { return requestJson('/plugins/config/admin/config/items', { method: 'POST', body: payload }) }
export function deleteItem(id) { return requestJson(`/plugins/config/admin/config/items/${id}`, { method: 'DELETE' }) }
