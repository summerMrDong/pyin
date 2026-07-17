import { httpRequest } from './http'
import { unwrapResult } from './result'

export async function fetchPermissions() {
  const result = await httpRequest('/api/permissions')
  return unwrapResult(result, '加载权限列表失败')
}
