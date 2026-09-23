import request from '@/utils/request.js'

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
