import { ref, onUnmounted } from 'vue'

export type WSStatus = 'closed' | 'connecting' | 'open'

export function useWebSocket(url: string) {
  const wsStatus = ref<WSStatus>('closed')
  let ws: WebSocket | null = null
  let reconnectTimer: ReturnType<typeof setTimeout> | null = null

  let onMessageHandler: ((data: string) => void) | null = null
  let autoReconnect = true
  let isManualClose = false

  function connect() {
    if (ws?.readyState === WebSocket.OPEN) return
    isManualClose = false
    wsStatus.value = 'connecting'
    try {
      ws = new WebSocket(url)
    } catch {
      wsStatus.value = 'closed'
      return
    }
    ws.binaryType = 'arraybuffer'
    ws.onopen = () => {
      wsStatus.value = 'open'
    }
    ws.onclose = () => {
      wsStatus.value = 'closed'
      ws = null
      if (autoReconnect && !isManualClose) {
        reconnectTimer = setTimeout(() => connect(), 3000)
      }
    }
    ws.onerror = () => {
      ws?.close()
    }
    ws.onmessage = (evt) => {
      if (typeof evt.data === 'string' && onMessageHandler) {
        onMessageHandler(evt.data)
      }
    }
  }

  function disconnect() {
    isManualClose = true
    autoReconnect = false
    if (reconnectTimer) {
      clearTimeout(reconnectTimer)
      reconnectTimer = null
    }
    ws?.close()
    ws = null
    wsStatus.value = 'closed'
  }

  function send(data: ArrayBuffer | string) {
    if (ws?.readyState === WebSocket.OPEN) {
      ws.send(data)
    }
  }

  function onMessage(handler: (data: string) => void) {
    onMessageHandler = handler
  }

  onUnmounted(() => {
    disconnect()
  })

  return {
    wsStatus,
    connect,
    disconnect,
    send,
    onMessage
  }
}
