import request from '@/utils/request'

export function sendMessage(data) {
  return request.post('/ai/chat/send', data)
}

export function getSessionRecords(sessionId) {
  return request.get(`/ai/chat/session/${sessionId}`)
}

export function getMySessions() {
  return request.get('/ai/chat/my')
}

export function deleteChatRecord(id) {
  return request.delete(`/ai/chat/${id}`)
}
