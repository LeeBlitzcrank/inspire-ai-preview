import request from '@/utils/request.js'
import {getAccessToken} from '@/utils/tokenStorage.js'

const API_BASE = import.meta.env.VITE_API_BASE ? import.meta.env.VITE_API_BASE + '/api' : '/api'

export const sendMessage = (toUserId, content, type = 'text', extraJson = null) =>
  request.post('/message/send', { toUserId: String(toUserId), content, type, extraJson })
export const getConversations = () => request.get('/message/conversations')
export const getMessages = (conversationId, page = 1, size = 20) => request.get('/message/list', { params: { conversationId: String(conversationId), page, size } })
export const markMessageRead = (conversationId) => request.post('/message/read', { conversationId: String(conversationId) })
export const getMessageUnreadCount = () => request.get('/message/unread')

export const deleteConversation = (id) => request.delete(`/message/conversation/${id}`)

export const startConversation = (toUserId) => request.post('/message/start', { toUserId })

export const deleteAllConversations = () => request.delete('/message/conversations')
export const recallMessage = (conversationId, id) => request.post(`/message/${conversationId}/${id}/recall`)

export function connectMessageStream(onEvent, onError) {
  const controller = new AbortController()
  const token = getAccessToken()
  if (!token) {
    onError?.(new Error('No access token'))
    return () => controller.abort()
  }

  fetch(`${API_BASE}/message/stream`, {
    headers: {
      Authorization: `Bearer ${token}`,
      Accept: 'text/event-stream'
    },
    signal: controller.signal
  }).then(async response => {
    if (!response.ok || !response.body) {
      throw new Error(`SSE HTTP ${response.status}`)
    }
    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''
    while (true) {
      const {value, done} = await reader.read()
      if (done) break
      buffer += decoder.decode(value, {stream: true}).replace(/\r\n/g, '\n')
      let boundary = buffer.indexOf('\n\n')
      while (boundary >= 0) {
        const block = buffer.slice(0, boundary)
        buffer = buffer.slice(boundary + 2)
        const event = {name: 'message', data: ''}
        for (const line of block.split('\n')) {
          if (line.startsWith('event:')) event.name = line.slice(6).trim()
          if (line.startsWith('data:')) event.data += line.slice(5).trim()
        }
        if (event.data) {
          try {
            onEvent?.(event.name, JSON.parse(event.data))
          } catch (e) {
            onEvent?.(event.name, event.data)
          }
        }
        boundary = buffer.indexOf('\n\n')
      }
    }
  }).catch(error => {
    if (error.name !== 'AbortError') onError?.(error)
  })
  return () => controller.abort()
}
