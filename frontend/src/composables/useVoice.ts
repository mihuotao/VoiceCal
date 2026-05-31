import { ref } from 'vue'
import { useWebSocket } from '@/composables/useWebSocket'
import type { VoiceStatus, ParsedIntent } from '@/types/voice'

/**
 * 动态生成 WebSocket URL（每次调用时读取最新 token）
 */
function buildWsUrl(): string {
  const token = localStorage.getItem('voicecal_token') || ''
  const protocol = location.protocol === 'https:' ? 'wss:' : 'ws:'
  return `${protocol}//${location.host}/ws/voice?token=${encodeURIComponent(token)}`
}

/**
 * 检查 JWT token 是否过期
 */
function isTokenExpired(): boolean {
  const token = localStorage.getItem('voicecal_token')
  if (!token) return true
  try {
    const payload = JSON.parse(atob(token.split('.')[1]))
    const exp = payload.exp * 1000 // 转为毫秒
    return Date.now() >= exp
  } catch {
    return true
  }
}

const TARGET_SAMPLE_RATE = 16000
const BUFFER_SIZE = 2048
const SILENCE_TIMEOUT_MS = 2000
const ASR_TIMEOUT_MS = 8000
const VAD_THRESHOLD = 0.025

/**
 * 语音识别状态机
 * idle -> listening -> recording -> processing -> result/error
 *                                                   ↓
 *                                               idle (自动释放资源)
 */
export function useVoice() {
  const status = ref<VoiceStatus>('idle')
  const amplitude = ref(0)
  const isOverlayOpen = ref(false)
  const transcript = ref('')
  const partialText = ref('')
  const errorMessage = ref('')
  const parsedIntent = ref<ParsedIntent | null>(null)

  // 音频资源引用
  let audioContext: AudioContext | null = null
  let scriptNode: ScriptProcessorNode | null = null
  let analyserNode: AnalyserNode | null = null
  let mediaStream: MediaStream | null = null
  let rafId = 0

  // 会话状态
  let sessionId = ''
  let isRecording = false
  let asrTimeout: ReturnType<typeof setTimeout> | null = null
  let lastVoiceTime = 0
  let vadTimer: ReturnType<typeof setInterval> | null = null
  let audioChunksSent = 0
  let isProcessing = false

  // WebSocket 连接（不传固定 URL，使用动态 urlProvider）
  const {
    state: wsState,
    connect,
    disconnect,
    send,
    onMessage,
    onBinaryMessage,
    resetReconnect,
    setUrlProvider
  } = useWebSocket()

  // 设置动态 URL 提供函数：每次连接时读取最新 token
  setUrlProvider(buildWsUrl)

  // ==================== 资源管理 ====================

  /**
   * 释放所有音频资源
   * 确保麦克风、AudioContext、ScriptNode 等被正确清理
   */
  function releaseAudioResources() {
    console.log('Voice: 释放音频资源')

    isRecording = false

    if (scriptNode) {
      scriptNode.disconnect()
      scriptNode.onaudioprocess = null
      scriptNode = null
    }

    if (analyserNode) {
      analyserNode.disconnect()
      analyserNode = null
    }

    if (mediaStream) {
      mediaStream.getTracks().forEach(track => {
        track.stop()
        console.log('Voice: 停止麦克风轨道')
      })
      mediaStream = null
    }

    if (audioContext) {
      audioContext.close().catch(() => {})
      audioContext = null
    }

    stopAmplitudeLoop()
    stopVad()
  }

  /**
   * 释放 WebSocket 连接
   */
  function releaseWsConnection() {
    console.log('Voice: 释放 WebSocket 连接')
    disconnect()
  }

  /**
   * 释放所有资源（音频 + WebSocket）
   * 在终态（result/error/idle）时自动调用
   */
  function releaseAllResources() {
    console.log('Voice: 释放所有资源')
    releaseAudioResources()
    releaseWsConnection()

    if (asrTimeout) {
      clearTimeout(asrTimeout)
      asrTimeout = null
    }

    audioChunksSent = 0
    isProcessing = false
  }

  /**
   * 安全的状态转换
   * 到达终态时自动释放资源
   */
  function transitionTo(newStatus: VoiceStatus) {
    const oldStatus = status.value
    console.log(`Voice: ${oldStatus} -> ${newStatus}`)
    status.value = newStatus

    // 终态自动释放资源
    if (newStatus === 'result' || newStatus === 'error' || newStatus === 'idle') {
      releaseAllResources()
    }
  }

  // ==================== 音频处理 ====================

  function updateAmplitude() {
    if (!isRecording || !analyserNode) return
    const dataArray = new Uint8Array(analyserNode.frequencyBinCount)
    analyserNode.getByteFrequencyData(dataArray)
    const avg = Array.from(dataArray).reduce((a, b) => a + b, 0) / dataArray.length
    amplitude.value = Math.min(1, avg / 128)
    rafId = requestAnimationFrame(updateAmplitude)
  }

  function stopAmplitudeLoop() {
    cancelAnimationFrame(rafId)
    amplitude.value = 0
  }

  function resample(audioData: Float32Array, fromRate: number, toRate: number): Float32Array {
    if (fromRate === toRate) return audioData
    const ratio = toRate / fromRate
    const newLength = Math.round(audioData.length * ratio)
    const result = new Float32Array(newLength)
    for (let i = 0; i < newLength; i++) {
      const pos = i / ratio
      const idx = Math.floor(pos)
      const frac = pos - idx
      if (idx + 1 < audioData.length) {
        result[i] = audioData[idx] * (1 - frac) + audioData[idx + 1] * frac
      } else {
        result[i] = audioData[idx]
      }
    }
    return result
  }

  function float32ToInt16(float32Array: Float32Array): ArrayBuffer {
    const int16Array = new Int16Array(float32Array.length)
    for (let i = 0; i < float32Array.length; i++) {
      const s = Math.max(-1, Math.min(1, float32Array[i]))
      int16Array[i] = s < 0 ? s * 0x8000 : s * 0x7FFF
    }
    return int16Array.buffer
  }

  function generateSessionId() {
    return `s_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`
  }

  // ==================== VAD（语音活动检测） ====================

  function startVad() {
    lastVoiceTime = Date.now()
    vadTimer = setInterval(() => {
      if (!isRecording) return
      if (Date.now() - lastVoiceTime > SILENCE_TIMEOUT_MS) {
        console.log('VAD: 静默超时，自动停止录音')
        stopRecording()
      }
    }, 200)
  }

  function stopVad() {
    if (vadTimer) {
      clearInterval(vadTimer)
      vadTimer = null
    }
  }

  // ==================== WebSocket 消息处理 ====================

  function setupWsHandlers() {
    onMessage((data: string) => {
      try {
        const msg = JSON.parse(data)
        console.log('Voice WS recv:', msg.type, msg.payload?.text?.slice(0, 30))

        switch (msg.type) {
          case 'pong':
            // 心跳响应，忽略
            break

          case 'intermediate_result':
            if (msg.payload?.isFinal) {
              transcript.value = msg.payload.text || ''
              partialText.value = ''
            } else {
              if (status.value === 'result' || status.value === 'idle') return
              partialText.value = msg.payload?.text || ''
            }
            break

          case 'final_result':
            if (asrTimeout) { clearTimeout(asrTimeout); asrTimeout = null }
            partialText.value = ''
            transcript.value = transcript.value || msg.payload?.text || ''

            // 检查是否需要澄清
            if (msg.payload?.requiresClarify && msg.payload?.clarifyQuestion) {
              // 需要澄清：展示问题，等待用户补充
              parsedIntent.value = {
                action: 'clarify',
                clarifyQuestion: msg.payload.clarifyQuestion,
                confidence: msg.payload?.confidence || 0.3
              }
              transitionTo('result')
              break
            }

            // 根据意图类型解析结果
            {
              const actionType = msg.payload?.action?.type
              if (actionType === 'query') {
                // 查询意图
                parsedIntent.value = {
                  action: 'query',
                  queryDate: msg.payload.action.queryDate || '',
                  queryEndDate: msg.payload.action.queryEndDate || '',
                  events: msg.payload.action.events || [],
                  responseText: msg.payload?.responseText || '',
                  confidence: msg.payload?.confidence || 0.9
                }
              } else if (actionType === 'unknown') {
                // 未知意图
                parsedIntent.value = {
                  action: 'unknown',
                  responseText: msg.payload?.responseText || '抱歉，我没有理解您的指令',
                  confidence: msg.payload?.confidence || 0.3
                }
              } else {
                // 创建/更新/删除/提醒意图
                parsedIntent.value = {
                  action: actionType || 'create',
                  title: msg.payload?.action?.event?.title || msg.payload?.title || transcript.value,
                  description: msg.payload?.description || '',
                  startTime: msg.payload?.action?.event?.startTime || msg.payload?.startTime || new Date(Date.now() + 86400000).toISOString(),
                  endTime: msg.payload?.action?.event?.endTime || msg.payload?.endTime || new Date(Date.now() + 86400000 + 3600000).toISOString(),
                  confidence: msg.payload?.confidence || 0.9
                }
              }
            }
            // ✅ 关键：收到最终结果后立即转换到终态，自动释放资源
            transitionTo('result')
            break

          case 'error':
            if (asrTimeout) { clearTimeout(asrTimeout); asrTimeout = null }
            errorMessage.value = msg.payload?.message || '语音识别失败'
            partialText.value = '⚠️ ' + errorMessage.value
            console.warn('ASR error:', msg.payload?.message)
            // ✅ 关键：收到错误后立即转换到终态，自动释放资源
            transitionTo('error')
            break
        }
      } catch (e) {
        console.warn('Voice WS: 消息解析失败', e)
      }
    })

    onBinaryMessage((_data: ArrayBuffer) => {
      // reserved for future use
    })
  }

  /**
   * 等待 WebSocket 连接就绪
   */
  async function waitForWsOpen(): Promise<boolean> {
    if (wsState.value === 'open') return true

    // 检查 token 是否过期
    if (isTokenExpired()) {
      console.warn('Voice: Token 已过期，请重新登录')
      errorMessage.value = '登录已过期，请重新登录'
      return false
    }

    resetReconnect()  // 重置重连计数
    connect()  // connect 内部会调用 urlProvider 获取最新 token

    return new Promise((resolve) => {
      let checks = 0
      const interval = setInterval(() => {
        if (wsState.value === 'open') {
          clearInterval(interval)
          resolve(true)
        } else if (wsState.value === 'closed' || ++checks > 50) {
          clearInterval(interval)
          resolve(false)
        }
      }, 100)
    })
  }

  // ==================== 核心业务方法 ====================

  /**
   * 开始录音
   */
  async function startRecording(): Promise<boolean> {
    // 清理之前的资源
    releaseAllResources()

    let stream: MediaStream | null = null
    try {
      stream = await navigator.mediaDevices.getUserMedia({
        audio: {
          channelCount: 1,
          echoCancellation: true,
          noiseSuppression: true
        }
      })
    } catch (err) {
      console.error('getUserMedia failed:', err)
      errorMessage.value = '无法访问麦克风'
      return false
    }

    mediaStream = stream
    sessionId = generateSessionId()
    audioChunksSent = 0
    errorMessage.value = ''
    partialText.value = ''

    // 注册消息处理器
    setupWsHandlers()

    // 等待 WebSocket 连接
    const wsReady = await waitForWsOpen()

    if (!wsReady) {
      console.warn('WebSocket 连接失败')
      errorMessage.value = '无法连接语音服务'
      releaseAllResources()
      return false
    }

    // 发送开始信号
    send(JSON.stringify({ type: 'start', sessionId }))
    console.log('Voice start sent, sessionId:', sessionId)

    // 创建音频上下文
    const ctx = new AudioContext()
    await ctx.resume()
    console.log('AudioContext created, sampleRate:', ctx.sampleRate, 'state:', ctx.state)

    const source = ctx.createMediaStreamSource(stream)

    // 设置频率分析器（用于 UI 动画）
    analyserNode = ctx.createAnalyser()
    analyserNode.fftSize = 128
    source.connect(analyserNode)
    updateAmplitude()

    // 设置音频处理节点
    scriptNode = ctx.createScriptProcessor(BUFFER_SIZE, 1, 1)

    const actualSampleRate = ctx.sampleRate

    scriptNode.onaudioprocess = (e) => {
      if (!isRecording) return
      const float32 = e.inputBuffer.getChannelData(0)

      // VAD：计算 RMS
      let sumSq = 0
      for (let i = 0; i < float32.length; i++) {
        sumSq += float32[i] * float32[i]
      }
      const rms = Math.sqrt(sumSq / float32.length)

      if (rms > VAD_THRESHOLD) {
        lastVoiceTime = Date.now()
      }

      // 发送音频数据
      if (wsState.value === 'open') {
        const resampled = actualSampleRate !== TARGET_SAMPLE_RATE
          ? resample(float32, actualSampleRate, TARGET_SAMPLE_RATE)
          : float32

        const pcm16 = float32ToInt16(resampled)
        send(pcm16)
        audioChunksSent++
        if (audioChunksSent % 50 === 0) {
          console.log('Audio chunks sent:', audioChunksSent)
        }
      }
    }

    // 连接音频节点
    source.connect(scriptNode)
    const muteGain = ctx.createGain()
    muteGain.gain.value = 0
    scriptNode.connect(muteGain)
    muteGain.connect(ctx.destination)

    audioContext = ctx

    // 开始录音
    isRecording = true
    status.value = 'recording'
    startVad()

    return true
  }

  /**
   * 停止录音
   */
  async function stopRecording() {
    if (isProcessing || !isRecording) return
    isProcessing = true
    console.log('stopRecording, chunks sent:', audioChunksSent)

    // 停止录音和 VAD
    isRecording = false
    stopVad()
    stopAmplitudeLoop()

    // 暂停音频上下文
    if (audioContext && audioContext.state === 'running') {
      await audioContext.suspend()
    }

    // 断开音频节点（但不关闭 audioContext，稍后统一清理）
    if (scriptNode) {
      scriptNode.disconnect()
      scriptNode.onaudioprocess = null
      scriptNode = null
    }
    if (analyserNode) {
      analyserNode.disconnect()
      analyserNode = null
    }

    // 设置状态为处理中
    status.value = 'processing'

    // 发送结束信号
    if (wsState.value === 'open') {
      send(JSON.stringify({ type: 'end', sessionId }))
      console.log('Voice end sent')

      // 设置超时保护
      asrTimeout = setTimeout(() => {
        if (status.value === 'processing') {
          console.warn('ASR timeout')
          errorMessage.value = '识别超时，请重试'
          transitionTo('error')
        }
      }, ASR_TIMEOUT_MS)
    } else {
      console.warn('WS not open for end')
      errorMessage.value = '连接已断开'
      transitionTo('error')
    }

    isProcessing = false
  }

  /**
   * 打开语音覆盖层
   */
  function openOverlay() {
    isOverlayOpen.value = true
    status.value = 'listening'
    transcript.value = ''
    partialText.value = ''
    errorMessage.value = ''
    parsedIntent.value = null
  }

  /**
   * 关闭语音覆盖层
   */
  function closeOverlay() {
    isOverlayOpen.value = false
    // ✅ 关键：关闭时释放所有资源
    releaseAllResources()
    status.value = 'idle'
    // 重置状态，防止残留数据
    parsedIntent.value = null
    transcript.value = ''
    partialText.value = ''
    errorMessage.value = ''
  }

  /**
   * 清理（等同于 closeOverlay）
   */
  function cleanup() {
    closeOverlay()
  }

  return {
    status,
    amplitude,
    isOverlayOpen,
    transcript,
    partialText,
    errorMessage,
    parsedIntent,
    startRecording,
    stopRecording,
    openOverlay,
    closeOverlay,
    cleanup
  }
}
