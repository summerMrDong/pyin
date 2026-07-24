import { httpRequest } from './http'
import { unwrapResult } from './result'

const SYSTEM_ADMIN_API = '/plugins/system/admin'

export async function fetchResourceTree() {
  const result = await httpRequest(`${SYSTEM_ADMIN_API}/resources/tree`)
  return unwrapResult(result, '加载资源树失败')
}
