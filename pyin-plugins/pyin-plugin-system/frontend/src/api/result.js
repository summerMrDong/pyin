export function unwrapResult(result, fallbackMessage = '请求失败') {
  if (result?.success === false) {
    const error = new Error(result?.message || fallbackMessage)
    error.payload = result
    throw error
  }

  return result?.data
}
