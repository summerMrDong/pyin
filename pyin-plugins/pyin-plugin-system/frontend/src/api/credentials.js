import { httpRequest } from './http'
import { unwrapResult } from './result'

function buildQuery(params = {}) {
  const search = new URLSearchParams()

  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') {
      search.set(key, value)
    }
  })

  const query = search.toString()
  return query ? `?${query}` : ''
}

export async function fetchClientCredentials(params = {}) {
  const result = await httpRequest(`/api/core/client-credentials${buildQuery(params)}`)
  return unwrapResult(result, '加载接入凭证失败')
}

export async function createClientCredential(payload) {
  const result = await httpRequest('/api/core/client-credentials', {
    method: 'POST',
    body: payload
  })
  return unwrapResult(result, '创建接入凭证失败')
}

export async function enableClientCredential(id) {
  const result = await httpRequest(`/api/core/client-credentials/${id}/enable`, {
    method: 'POST'
  })
  return unwrapResult(result, '启用接入凭证失败')
}

export async function disableClientCredential(id) {
  const result = await httpRequest(`/api/core/client-credentials/${id}/disable`, {
    method: 'POST'
  })
  return unwrapResult(result, '停用接入凭证失败')
}

export async function rotateClientCredentialSecret(id) {
  const result = await httpRequest(`/api/core/client-credentials/${id}/rotate-secret`, {
    method: 'POST'
  })
  return unwrapResult(result, '轮换密钥失败')
}

export async function fetchClientCredentialRequestLogs(id, params = {}) {
  const result = await httpRequest(`/api/core/client-credentials/${id}/request-logs${buildQuery(params)}`)
  return unwrapResult(result, '加载请求日志失败')
}
