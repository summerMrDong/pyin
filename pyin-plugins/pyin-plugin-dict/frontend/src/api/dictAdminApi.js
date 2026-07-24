import { requestJson } from './http'

export function fetchTypes() {
  return requestJson('/plugins/dict/admin/dict/types')
}

export function saveType(payload) {
  return requestJson('/plugins/dict/admin/dict/types', {
    method: 'POST',
    body: payload
  })
}

export function deleteType(id) {
  return requestJson(`/plugins/dict/admin/dict/types/${id}`, {
    method: 'DELETE'
  })
}

export function fetchItems(typeId) {
  const suffix = typeId ? `?typeId=${encodeURIComponent(typeId)}` : ''
  return requestJson(`/plugins/dict/admin/dict/items${suffix}`)
}

export function fetchItemDetail(id) {
  return requestJson(`/plugins/dict/admin/dict/items/${id}`)
}

export function saveItem(payload) {
  return requestJson('/plugins/dict/admin/dict/items', {
    method: 'POST',
    body: payload
  })
}

export function deleteItem(id) {
  return requestJson(`/plugins/dict/admin/dict/items/${id}`, {
    method: 'DELETE'
  })
}
