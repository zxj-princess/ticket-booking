import request from '@/utils/request'

export function queryTrains(params) {
  return request.get('/train/search', { params })
}

// 兼容旧名称
export const searchTrains = queryTrains

export function getTrainById(id) {
  return request.get(`/train/${id}`)
}

export function listTrains(params) {
  return request.get('/train/list', { params })
}

export function addTrain(data) {
  return request.post('/train/add', data)
}

export function updateTrain(data) {
  return request.put('/train/update', data)
}

export function deleteTrain(id) {
  return request.delete(`/train/${id}`)
}
