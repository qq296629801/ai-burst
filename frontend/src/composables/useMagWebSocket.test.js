import { defineComponent, nextTick } from 'vue'
import { mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

const tokenRef = { token: 'test-token' }

vi.mock('@/stores/user', () => ({
  useUserStore: () => tokenRef,
}))

import { useMagWebSocket } from './useMagWebSocket'

describe('useMagWebSocket', () => {
  let sockets = []

  beforeEach(() => {
    tokenRef.token = 'test-token'
    sockets = []
    global.WebSocket = class MockWebSocket {
      static OPEN = 1

      constructor(url) {
        this.url = url
        this.readyState = 0
        sockets.push(this)
        queueMicrotask(() => {
          this.readyState = MockWebSocket.OPEN
          this.onopen?.()
        })
      }

      send = vi.fn()
      close = vi.fn(() => {
        this.readyState = 3
        this.onclose?.()
      })
    }
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  function mountWs() {
    const Comp = defineComponent({
      setup() {
        return useMagWebSocket()
      },
      template: '<div />',
    })
    return mount(Comp)
  }

  it('无 token 时不建立连接', async () => {
    tokenRef.token = ''
    const w = mountWs()
    w.vm.connect()
    await nextTick()
    expect(sockets).toHaveLength(0)
    w.unmount()
  })

  it('connect 使用 /ws/mag 与 query token，SUBSCRIBE / PING 报文格式正确', async () => {
    const w = mountWs()
    const onOpen = vi.fn()
    w.vm.connect({ onOpen })
    await nextTick()

    expect(sockets).toHaveLength(1)
    const ws = sockets[0]
    expect(ws.url).toContain('/ws/mag?token=')
    expect(decodeURIComponent(ws.url.split('token=')[1] || '')).toBe('test-token')
    expect(w.vm.connected).toBe(true)
    expect(onOpen).toHaveBeenCalled()

    w.vm.subscribeProject(42)
    expect(ws.send).toHaveBeenCalledWith(
      JSON.stringify({ op: 'SUBSCRIBE', channel: 'project:42' }),
    )

    w.vm.ping()
    expect(ws.send).toHaveBeenCalledWith(JSON.stringify({ op: 'PING' }))

    ws.onmessage({ data: JSON.stringify({ op: 'TASK', id: 1 }) })
    expect(w.vm.lastEvent).toEqual({ op: 'TASK', id: 1 })

    w.unmount()
  })

  it('disconnect 后不再发送', async () => {
    const w = mountWs()
    w.vm.connect()
    await nextTick()
    const ws = sockets[0]
    w.vm.disconnect()
    ws.send.mockClear()
    w.vm.subscribeProject(1)
    w.vm.ping()
    expect(ws.send).not.toHaveBeenCalled()
    w.unmount()
  })
})
