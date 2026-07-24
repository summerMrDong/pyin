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

export async function fetchRoles(params = {}) {
  const result = await httpRequest(`${SYSTEM_ADMIN_API}/roles${buildQuery(params)}`)
  return unwrapResult(result, '加载角色列表失败')
}

export async function fetchRoleOptions() {
  const result = await httpRequest(`${SYSTEM_ADMIN_API}/roles/options`)
  return unwrapResult(result, '加载角色选项失败')
}

export async function fetchRoleDetail(id) {
  const result = await httpRequest(`${SYSTEM_ADMIN_API}/roles/${id}`)
  return unwrapResult(result, '加载角色详情失败')
}

export async function createRole(payload) {
  const result = await httpRequest(`${SYSTEM_ADMIN_API}/roles`, {
    method: 'POST',
    body: payload
  })
  return unwrapResult(result, '创建角色失败')
}

export async function updateRole(id, payload) {
  const result = await httpRequest(`${SYSTEM_ADMIN_API}/roles/${id}`, {
    method: 'PUT',
    body: payload
  })
  return unwrapResult(result, '更新角色失败')
}

export async function deleteRole(id) {
  const result = await httpRequest(`${SYSTEM_ADMIN_API}/roles/${id}`, {
    method: 'DELETE'
  })
  return unwrapResult(result, '删除角色失败')
}

export async function fetchRolePermissions(id) {
  const result = await httpRequest(`${SYSTEM_ADMIN_API}/roles/${id}/permissions`)
  return unwrapResult(result, '加载角色权限失败')
}

export async function updateRolePermissions(id, permissionCodes) {
  const result = await httpRequest(`${SYSTEM_ADMIN_API}/roles/${id}/permissions`, {
    method: 'PUT',
    body: permissionCodes
  })
  return unwrapResult(result, '更新角色权限失败')
}

export async function fetchRoleResources(id) {
  const result = await httpRequest(`${SYSTEM_ADMIN_API}/roles/${id}/resources`)
  return unwrapResult(result, '加载角色资源失败')
}

export async function updateRoleResources(id, resourceKeys) {
  const result = await httpRequest(`${SYSTEM_ADMIN_API}/roles/${id}/resources`, {
    method: 'PUT',
    body: resourceKeys
  })
  return unwrapResult(result, '更新角色资源失败')
}

export async function fetchRoleUsers(id) {
  const result = await httpRequest(`${SYSTEM_ADMIN_API}/roles/${id}/users`)
  return unwrapResult(result, '加载角色用户失败')
}

export async function updateRoleUsers(id, userIds) {
  const result = await httpRequest(`${SYSTEM_ADMIN_API}/roles/${id}/users`, {
    method: 'PUT',
    body: userIds
  })
  return unwrapResult(result, '更新角色用户失败')
}
