import { onUnmounted, ref } from 'vue'
import { useUserStore } from '@/stores/user'

/**
 * 与技术方案 §8 对齐：连接 `/ws/mag?token=...`，发送 SUBSCRIBE / PING。
 */
export function useMagWebSocket() {
  const connected = ref(false)
  const lastEvent = ref(null)
  let ws
  let reconnectTimer

  function wsBaseUrl() {
    const proto = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
    return `${proto}//${window.location.host}`
  }

  function connect(handlers = {}) {
    const user = useUserStore()
    if (!user.token) return
    disconnect()
    const url = `${wsBaseUrl()}/ws/mag?token=${encodeURIComponent(user.token)}`
    ws = new WebSocket(url)
    ws.onopen = () => {
      connected.value = true
      if (typeof handlers.onOpen === 'function') handlers.onOpen(ws)
    }
    ws.onclose = () => {
      connected.value = false
      if (typeof handlers.onClose === 'function') handlers.onClose()
    }
    ws.onerror = () => {
      if (typeof handlers.onError === 'function') handlers.onError()
    }
    ws.onmessage = (ev) => {
      try {
        const data = JSON.parse(ev.data)
        lastEvent.value = data
        if (typeof handlers.onMessage === 'function') handlers.onMessage(data, ev)
      } catch {
        lastEvent.value = { raw: ev.data }
      }
    }
  }

  function disconnect() {
    if (reconnectTimer) {
      clearTimeout(reconnectTimer)
      reconnectTimer = null
    }
    if (ws) {
      ws.onopen = null
      ws.onclose = null
      ws.onmessage = null
      ws.onerror = null
      try {
        ws.close()
      } catch {
        /* ignore */
      }
      ws = null
    }
    connected.value = false
  }

  function subscribeProject(projectId) {
    if (ws && ws.readyState === WebSocket.OPEN) {
      ws.send(JSON.stringify({ op: 'SUBSCRIBE', channel: `project:${projectId}` }))
    }
  }

  function ping() {
    if (ws && ws.readyState === WebSocket.OPEN) {
      ws.send(JSON.stringify({ op: 'PING' }))
    }
  }

  onUnmounted(disconnect)

  return { connected, lastEvent, connect, disconnect, subscribeProject, ping }
}
