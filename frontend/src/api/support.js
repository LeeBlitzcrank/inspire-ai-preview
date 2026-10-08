import request from '@/utils/request.js'

export function askSupport(query, topK = 6) {
  return request.post('/rag/support/ask', {query, topK}, {timeout: 120000})
}

export function searchSupport(query, topK = 6) {
  return request.post('/rag/support/search', {query, topK})
}

export function getSupportStatus() {
  return request.get('/rag/support/status')
}

export function createSupportHandoff(issue, contact = '', attachments = []) {
  return request.post('/rag/support/handoff', {issue, contact, attachments})
}

export function getSupportTicket(ticketNo, accessToken) {
  return request.get(`/rag/support/ticket/${encodeURIComponent(ticketNo)}`, {
    headers: accessToken ? {'X-Support-Ticket-Token': accessToken} : {}
  })
}

export function uploadSupportAttachment(formData, onUploadProgress) {
  return request.post('/file/support/upload', formData, {
    headers: {'Content-Type': 'multipart/form-data'},
    timeout: 180000,
    maxBodyLength: Infinity,
    maxContentLength: Infinity,
    onUploadProgress
  })
}

export function adminSupportTicketList(params) {
  return request.get('/admin/support/list', {params})
}

export function adminSupportTicketDetail(id) {
  return request.get(`/admin/support/${id}`)
}

export function adminSupportTicketClaim(id) {
  return request.post(`/admin/support/${id}/claim`)
}

export function adminSupportTicketReply(id, content) {
  return request.post(`/admin/support/${id}/reply`, {content})
}

export function adminSupportTicketClose(id) {
  return request.post(`/admin/support/${id}/close`)
}
