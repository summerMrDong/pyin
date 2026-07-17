import { httpRequest } from './http'
import { unwrapResult } from './result'

export async function fetchResourceTree() {
  const result = await httpRequest('/api/resources/tree')
  return unwrapResult(result, '加载资源树失败')
}
