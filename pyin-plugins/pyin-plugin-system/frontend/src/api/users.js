import { httpRequest } from './http'
import { unwrapResult } from './result'

const SYSTEM_ADMIN_API = '/plugins/system/admin'

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

export async function fetchUsers(params = {}) {
  const result = await httpRequest(`${SYSTEM_ADMIN_API}/users${buildQuery(params)}`)
  return unwrapResult(result, '加载用户列表失败')
}

export async function fetchUserDetail(id) {
  const result = await httpRequest(`${SYSTEM_ADMIN_API}/users/${id}`)
  return unwrapResult(result, '加载用户详情失败')
}

export async function createUser(payload) {
  const result = await httpRequest(`${SYSTEM_ADMIN_API}/users`, {
    method: 'POST',
    body: payload
  })
  return unwrapResult(result, '创建用户失败')
}

export async function updateUser(id, payload) {
  const result = await httpRequest(`${SYSTEM_ADMIN_API}/users/${id}`, {
    method: 'PUT',
    body: payload
  })
  return unwrapResult(result, '更新用户失败')
}

export async function resetUserPassword(id, payload) {
  const result = await httpRequest(`${SYSTEM_ADMIN_API}/users/${id}/reset-password`, {
    method: 'POST',
    body: payload
  })
  return unwrapResult(result, '重置密码失败')
}

export async function deleteUser(id) {
  const result = await httpRequest(`${SYSTEM_ADMIN_API}/users/${id}`, {
    method: 'DELETE'
  })
  return unwrapResult(result, '删除用户失败')
}
