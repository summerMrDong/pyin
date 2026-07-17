async function requestJson(url, options = {}) {
  const response = await fetch(url, {
    headers: {
      ...(options.headers ?? {})
    },
    ...options
  })

  const contentType = response.headers.get('content-type') || ''
  if (!contentType.includes('application/json')) {
    throw new Error('文件插件返回了非 JSON 响应')
  }

  const result = await response.json()
  if (!response.ok || !result.success) {
    throw new Error(result.message || '文件插件请求失败')
  }
  return result.data
}

export function fetchSummary() {
  return requestJson('/api/plugins/file/admin/summary')
}

export function fetchBuckets() {
  return requestJson('/api/plugins/file/admin/buckets')
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
  return requestJson(`/api/plugins/file/admin/files${suffix ? `?${suffix}` : ''}`)
}

export function fetchFileDetail(fileId) {
  return requestJson(`/api/plugins/file/admin/files/${fileId}`)
}

export function deleteFile(fileId) {
  return requestJson(`/api/plugins/file/admin/files/${fileId}`, {
    method: 'DELETE'
  })
}

export async function uploadFile(formData) {
  const response = await fetch('/api/plugins/file/admin/files/upload', {
    method: 'POST',
    body: formData
  })
  const result = await response.json()
  if (!response.ok || !result.success) {
    throw new Error(result.message || '文件上传失败')
  }
  return result.data
}
