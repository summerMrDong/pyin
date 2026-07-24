import { httpRequest } from './http'
import { unwrapResult } from './result'

const SYSTEM_ADMIN_API = '/plugins/system/admin'

export async function fetchPermissions() {
  const result = await httpRequest(`${SYSTEM_ADMIN_API}/permissions`)
  return unwrapResult(result, '加载权限列表失败')
}
