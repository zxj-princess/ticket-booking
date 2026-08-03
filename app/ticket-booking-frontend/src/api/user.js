import request from '@/utils/request'

export function register(data) {
  return request.post('/user/register', data)
}

export function login(data) {
  return request.post('/user/login', data)
}

export function getUserInfo() {
  return request.get('/user/info')
}

export function updateUserInfo(data) {
  return request.put('/user/update', data)
}

export function listUsers(params) {
  return request.get('/user/list', { params })
}

export function updateUserStatus(id, status) {
  return request.put('/user/status', null, { params: { id, status } })
}

export function deleteUser(id) {
  return request.delete(`/user/${id}`)
}

export function logout() {
  return request.post('/user/logout')
}

export function changePassword(data) {
  return request.put('/user/password', data)
}

export function updateUserProfile(data) {
  return request.put('/user/update', data)
}

export function getMyPassengers() {
  return request.get('/user/passengers')
}

export function addPassenger(data) {
  return request.post('/user/passengers', data)
}

export function updatePassenger(id, data) {
  return request.put(`/user/passengers/${id}`, data)
}

export function deletePassenger(id) {
  return request.delete(`/user/passengers/${id}`)
}
