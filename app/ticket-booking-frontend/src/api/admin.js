import request from '@/utils/request'

// 数据统计
export function getStats() {
  return request.get('/admin/stats')
}
export const getDashboardStats = getStats

// AI对话管理
export function listChatRecords(params) {
  return request.get('/admin/chat/records', { params })
}

export function getChatIntents() {
  return request.get('/admin/chat/intents')
}
export const getTopIntents = getChatIntents

// 用户管理(管理端)
export function listUsersAdmin(params) {
  return request.get('/user/list', { params })
}

// 车次管理(管理端)
export function listTrainsAdmin(params) {
  return request.get('/train/list', { params })
}

export function createTrain(data) {
  return request.post('/train/add', data)
}

export function updateTrain(data) {
  return request.put('/train/update', data)
}

export function deleteTrain(id) {
  return request.delete(`/train/${id}`)
}

// 站点管理(管理端)
export function listStationsAdmin() {
  return request.get('/station/list')
}

export function createStation(data) {
  return request.post('/station/add', data)
}

export function updateStation(data) {
  return request.put('/station/update', data)
}

export function deleteStation(id) {
  return request.delete(`/station/${id}`)
}

// 订单管理(管理端)
export function listOrdersAdmin(params) {
  return request.get('/order/list', { params })
}
