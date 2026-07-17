import { httpRequest } from './http'

export function loginApi(form) {
  return httpRequest('/api/auth/login', {
    method: 'POST',
    body: form
  })
}

export function logoutApi() {
  return httpRequest('/api/auth/logout', {
    method: 'POST'
  })
}

export function fetchCurrentUserApi() {
  return httpRequest('/api/auth/current-user')
}
