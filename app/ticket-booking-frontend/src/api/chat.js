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

export const getActions = () => request.get('/ai/actions')
export const confirmAction = id => request.post(`/ai/actions/${id}/confirm`)
export const cancelAction = id => request.post(`/ai/actions/${id}/cancel`)

export async function streamMessage(data, onEvent, signal) {
  const base = (import.meta.env.VITE_API_BASE_URL || '').replace(/\/$/, '')
  const response = await fetch(`${base}/ai/chat/stream`, {
    method: 'POST', headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${localStorage.getItem('token') || ''}` },
    body: JSON.stringify(data), signal
  })
  if (!response.ok) throw new Error(`请求失败 (${response.status})`)
  const reader = response.body.getReader(), decoder = new TextDecoder()
  let buffer = '', completed = false
  try {
    while (true) {
      const { value, done } = await reader.read()
      buffer += decoder.decode(value, { stream: !done }).replace(/\r\n/g, '\n')
      let boundary
      while ((boundary = buffer.indexOf('\n\n')) >= 0) {
        const block = buffer.slice(0, boundary); buffer = buffer.slice(boundary + 2)
        let event = 'message'; const lines = []
        for (const line of block.split('\n')) {
          if (line.startsWith('event:')) event = line.slice(6).trim()
          if (line.startsWith('data:')) lines.push(line.slice(5).replace(/^ /, ''))
        }
        let payload = lines.join('\n')
        try { payload = JSON.parse(payload) } catch {}
        if (event === 'failure') throw new Error(String(payload))
        if (event === 'done') completed = true
        onEvent(event, payload)
      }
      if (done) break
    }
    if (!completed) throw new Error('连接中断，可重试回复；请先检查已有确认卡片')
  } finally { reader.releaseLock() }
}
