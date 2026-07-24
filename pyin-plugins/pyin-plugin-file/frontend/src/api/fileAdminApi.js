import { requestJson } from './http'

export function fetchSummary() {
  return requestJson('/plugins/file/admin/summary')
}

export function fetchBuckets() {
  return requestJson('/plugins/file/admin/buckets')
}

export function fetchFiles(bizType, bizId) {
  const params = new URLSearchParams()
  if (bizType) {
    params.set('bizType', bizType)
  }
  if (bizId) {
    params.set('bizId', bizId)
  }
  const suffix = params.toString()
  return requestJson(`/plugins/file/admin/files${suffix ? `?${suffix}` : ''}`)
}

export function fetchFileDetail(fileId) {
  return requestJson(`/plugins/file/admin/files/${fileId}`)
}

export function deleteFile(fileId) {
  return requestJson(`/plugins/file/admin/files/${fileId}`, {
    method: 'DELETE'
  })
}

export function uploadFile(formData) {
  return requestJson('/plugins/file/admin/files/upload', {
    method: 'POST',
    body: formData
  })
}
