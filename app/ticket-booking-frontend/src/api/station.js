import request from '@/utils/request'

export function listStations() {
  return request.get('/station/list')
}

export function searchStations(params) {
  return request.get('/station/search', { params })
}

export function addStation(data) {
  return request.post('/station/add', data)
}

export function updateStation(data) {
  return request.put('/station/update', data)
}

export function deleteStation(id) {
  return request.delete(`/station/${id}`)
}
