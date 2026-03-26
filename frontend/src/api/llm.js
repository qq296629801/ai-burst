import http from './http'

export function fetchLlmProviders() {
  return http.get('/llm/providers')
}

export function fetchLlmChannels() {
  return http.get('/llm/channels')
}

export function createLlmChannel(data) {
  return http.post('/llm/channels', data)
}

export function updateLlmChannel(data) {
  return http.put('/llm/channels', data)
}

export function deleteLlmChannel(id) {
  return http.delete(`/llm/channels/${id}`)
}

export function llmChat(data) {
  return http.post('/llm/chat', data)
}
