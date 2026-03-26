import http from './http'

export function fetchUsers(params) {
  return http.get('/system/users', { params })
}

export function createUser(data) {
  return http.post('/system/users', data)
}

export function updateUser(data) {
  return http.put('/system/users', data)
}

export function deleteUser(id) {
  return http.delete(`/system/users/${id}`)
}

export function resetUserPassword(id, password) {
  return http.post(`/system/users/${id}/reset-password`, { password })
}

export function fetchRoles() {
  return http.get('/system/roles')
}

export function createRole(data) {
  return http.post('/system/roles', data)
}

export function updateRole(data) {
  return http.put('/system/roles', data)
}

export function deleteRole(id) {
  return http.delete(`/system/roles/${id}`)
}

export function fetchMenuTree() {
  return http.get('/system/menus/tree')
}

export function createMenu(data) {
  return http.post('/system/menus', data)
}

export function updateMenu(data) {
  return http.put('/system/menus', data)
}

export function deleteMenu(id) {
  return http.delete(`/system/menus/${id}`)
}
