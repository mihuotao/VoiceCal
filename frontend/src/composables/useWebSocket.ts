import { ref, onUnmounted } from 'vue'

/**
 * WebSocket 连接状态
 * idle -> connecting -> open -> closing -> closed
 */
export type WSState = 'idle' | 'connecting' | 'open' | 'closing' | 'closed'

/**
 * 企业级 WebSocket 连接管理
 * - 状态机管理连接生命周期
 * - 指数退避重连策略
 * - 心跳保活机制
 * - 资源自动释放
 */
export function useWebSocket(url: string) {
  const state = ref<WSState>('idle')
  const reconnectCount = ref(0)
  const lastError = ref<string>('')

  let ws: WebSocket | null = null
  let reconnectTimer: ReturnType<typeof setTimeout> | null = null
  let heartbeatTimer: ReturnType<typeof setInterval> | null = null

  let onMessageHandler: ((data: string) => void) | null = null
  let onBinaryMessageHandler: ((data: ArrayBuffer) => void) | null = null
  let onStateChangeHandler: ((state: WSState) => void) | null = null

  // 配置参数
  const config = {
    maxReconnectAttempts: 5,
    baseReconnectDelay: 1000,
    maxReconnectDelay: 30000,
    heartbeatInterval: 30000,
    heartbeatTimeout: 5000,
  }

  // 状态转换规则
  const allowedTransitions: Record<WSState, WSState[]> = {
    idle: ['connecting'],
    connecting: ['open', 'closed'],
    open: ['closing', 'closed'],
    closing: ['closed'],
    closed: ['connecting', 'idle'],
  }

  /**
   * 检查状态转换是否合法
   */
  function canTransition(to: WSState): boolean {
    return allowedTransitions[state.value]?.includes(to) ?? false
  }

  /**
   * 执行状态转换
   */
  function transitionTo(newState: WSState) {
    const oldState = state.value
    if (oldState === newState) return

    if (!canTransition(newState)) {
      console.warn(`WS: 非法状态转换 ${oldState} -> ${newState}`)
      return
    }

    state.value = newState
    console.log(`WS: ${oldState} -> ${newState}`)
    onStateChangeHandler?.(newState)
  }

  /**
   * 计算指数退避延迟
   */
  function getReconnectDelay(): number {
    const delay = config.baseReconnectDelay * Math.pow(2, reconnectCount.value)
    // 添加随机抖动，避免重连风暴
    const jitter = Math.random() * 1000
    return Math.min(delay + jitter, config.maxReconnectDelay)
  }

  /**
   * 启动心跳
   */
  function startHeartbeat() {
    stopHeartbeat()
    heartbeatTimer = setInterval(() => {
      if (ws?.readyState === WebSocket.OPEN) {
        try {
          ws.send(JSON.stringify({ type: 'ping' }))
        } catch (e) {
          console.warn('WS: 心跳发送失败', e)
        }
      }
    }, config.heartbeatInterval)
  }

  /**
   * 停止心跳
   */
  function stopHeartbeat() {
    if (heartbeatTimer) {
      clearInterval(heartbeatTimer)
      heartbeatTimer = null
    }
  }

  /**
   * 清理重连定时器
   */
  function clearReconnectTimer() {
    if (reconnectTimer) {
      clearTimeout(reconnectTimer)
      reconnectTimer = null
    }
  }

  /**
   * 建立连接
   */
  function connect() {
    // 如果已经连接或正在连接，忽略
    if (state.value === 'open' || state.value === 'connecting') {
      console.log('WS: 已连接或正在连接，忽略')
      return
    }

    // 关闭现有连接
    if (ws) {
      cleanup()
    }

    transitionTo('connecting')
    lastError.value = ''

    try {
      ws = new WebSocket(url)
      ws.binaryType = 'arraybuffer'

      ws.onopen = () => {
        transitionTo('open')
        reconnectCount.value = 0  // 重置重连计数
        startHeartbeat()
      }

      ws.onclose = (event) => {
        stopHeartbeat()

        if (state.value === 'closing') {
          // 主动关闭，不重连
          transitionTo('closed')
          return
        }

        // 非正常关闭
        transitionTo('closed')

        if (event.code !== 1000) {  // 非正常关闭码
          lastError.value = event.reason || '连接关闭'
          attemptReconnect()
        }
      }

      ws.onerror = (event) => {
        console.error('WS: 连接错误', event)
        lastError.value = '连接错误'
        // onclose 会随后触发
      }

      ws.onmessage = (evt) => {
        if (typeof evt.data === 'string') {
          onMessageHandler?.(evt.data)
        } else if (evt.data instanceof ArrayBuffer) {
          onBinaryMessageHandler?.(evt.data)
        }
      }
    } catch (error) {
      console.error('WS: 创建连接失败', error)
      lastError.value = '创建连接失败'
      transitionTo('closed')
      attemptReconnect()
    }
  }

  /**
   * 尝试重连
   */
  function attemptReconnect() {
    if (reconnectCount.value >= config.maxReconnectAttempts) {
      console.warn(`WS: 已达最大重连次数 ${config.maxReconnectAttempts}`)
      return
    }

    const delay = getReconnectDelay()
    reconnectCount.value++
    console.log(`WS: 将在 ${Math.round(delay)}ms 后重连 (第 ${reconnectCount.value} 次)`)

    clearReconnectTimer()
    reconnectTimer = setTimeout(() => {
      if (state.value === 'closed') {
        connect()
      }
    }, delay)
  }

  /**
   * 清理内部资源
   */
  function cleanup() {
    clearReconnectTimer()
    stopHeartbeat()

    if (ws) {
      // 移除事件监听，防止触发重连
      ws.onopen = null
      ws.onclose = null
      ws.onerror = null
      ws.onmessage = null

      if (ws.readyState === WebSocket.OPEN || ws.readyState === WebSocket.CONNECTING) {
        ws.close(1000, 'Cleanup')
      }
      ws = null
    }
  }

  /**
   * 主动断开连接
   */
  function disconnect() {
    console.log('WS: 主动断开连接')

    // 阻止自动重连
    clearReconnectTimer()
    reconnectCount.value = config.maxReconnectAttempts

    if (ws && (state.value === 'open' || state.value === 'connecting')) {
      transitionTo('closing')
      ws.close(1000, 'Client disconnect')
    }

    cleanup()
    transitionTo('closed')
  }

  /**
   * 发送数据
   */
  function send(data: ArrayBuffer | string): boolean {
    if (ws?.readyState === WebSocket.OPEN) {
      try {
        ws.send(data)
        return true
      } catch (e) {
        console.error('WS: 发送失败', e)
        return false
      }
    }
    console.warn('WS: 连接未就绪，无法发送')
    return false
  }

  /**
   * 注册消息处理器
   */
  function onMessage(handler: (data: string) => void) {
    onMessageHandler = handler
  }

  /**
   * 注册二进制消息处理器
   */
  function onBinaryMessage(handler: (data: ArrayBuffer) => void) {
    onBinaryMessageHandler = handler
  }

  /**
   * 注册状态变化处理器
   */
  function onStateChange(handler: (state: WSState) => void) {
    onStateChangeHandler = handler
  }

  /**
   * 重置重连计数（允许重新重连）
   */
  function resetReconnect() {
    reconnectCount.value = 0
  }

  // 组件卸载时清理
  onUnmounted(() => {
    disconnect()
    onMessageHandler = null
    onBinaryMessageHandler = null
    onStateChangeHandler = null
  })

  return {
    state,
    reconnectCount,
    lastError,
    connect,
    disconnect,
    send,
    onMessage,
    onBinaryMessage,
    onStateChange,
    resetReconnect,
    isConnected: () => state.value === 'open',
  }
}
