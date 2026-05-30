import { ref } from 'vue'
import { useWebSocket } from '@/composables/useWebSocket'
import type { VoiceStatus, ASRResult, ParsedIntent } from '@/types/voice'

const WS_URL = `${location.protocol === 'https:' ? 'wss:' : 'ws:'}//${location.host}/ws/voice`
const TARGET_SAMPLE_RATE = 16000
const BUFFER_SIZE = 2048
const SILENCE_TIMEOUT_MS = 2000
const ASR_TIMEOUT_MS = 8000
const VAD_THRESHOLD = 0.025

export function useVoice() {
  const status = ref<VoiceStatus>('idle')
  const amplitude = ref(0)
  const isOverlayOpen = ref(false)
  const transcript = ref('')
  const partialText = ref('')
  const errorMessage = ref('')
  const parsedIntent = ref<ParsedIntent | null>(null)

  let audioContext: AudioContext | null = null
  let scriptNode: ScriptProcessorNode | null = null
  let analyserNode: AnalyserNode | null = null
  let mediaStream: MediaStream | null = null
  let rafId = 0
  let sessionId = ''
  let isRecording = false
  let asrTimeout: ReturnType<typeof setTimeout> | null = null
  let lastVoiceTime = 0
  let vadTimer: ReturnType<typeof setInterval> | null = null
  let audioChunksSent = 0

  const { wsStatus, connect, disconnect, send, onMessage, onBinaryMessage } = useWebSocket(WS_URL)

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

  function setupWsHandlers() {
    onMessage((data: string) => {
      try {
        const msg = JSON.parse(data)
        console.log('Voice WS recv:', msg.type, msg.payload?.text?.slice(0, 30))
        switch (msg.type) {
          case 'intermediate_result':
            if (msg.payload?.isFinal) {
              transcript.value = msg.payload.text || ''
              partialText.value = ''
            } else {
              partialText.value = msg.payload?.text || ''
            }
            break

          case 'final_result':
            if (asrTimeout) { clearTimeout(asrTimeout); asrTimeout = null }
            status.value = 'result'
            partialText.value = ''
            transcript.value = transcript.value || msg.payload?.title || ''
            parsedIntent.value = {
              action: msg.payload?.action || 'create',
              title: msg.payload?.title || transcript.value,
              description: msg.payload?.description || '',
              startTime: msg.payload?.startTime || new Date(Date.now() + 86400000).toISOString(),
              endTime: msg.payload?.endTime || new Date(Date.now() + 86400000 + 3600000).toISOString(),
              confidence: msg.payload?.confidence || 0.9
            }
            break

          case 'error':
            if (asrTimeout) { clearTimeout(asrTimeout); asrTimeout = null }
            errorMessage.value = msg.payload?.message || '语音识别失败'
            partialText.value = '⚠️ ' + errorMessage.value
            console.warn('ASR error:', msg.payload?.message)
            fallbackToMock()
            break
        }
      } catch {
        // ignore
      }
    })

    onBinaryMessage((_data: ArrayBuffer) => {
      // reserved for future use
    })
  }

  function fallbackToMock() {
    if (asrTimeout) { clearTimeout(asrTimeout); asrTimeout = null }
    stopVad()
    setTimeout(() => {
      const mockResult: ASRResult = {
        text: mockASR(),
        isFinal: true
      }
      transcript.value = mockResult.text
      partialText.value = ''
      errorMessage.value = ''
      parsedIntent.value = parseIntent(mockResult.text)
      status.value = 'result'
    }, 800 + Math.random() * 600)
  }

  function mockASR(): string {
    const samples = [
      '明天上午十点开会',
      '下周一早上九点去医院体检',
      '这周五晚上七点跟朋友吃饭',
      '今天下午三点约了客户',
      '后天下午去健身房'
    ]
    return samples[Math.floor(Math.random() * samples.length)]
  }

  function parseIntent(text: string): ParsedIntent {
    const now = new Date()
    let startTime = new Date(now.getFullYear(), now.getMonth(), now.getDate() + 1, 9, 0)
    let endTime = new Date(now.getFullYear(), now.getMonth(), now.getDate() + 1, 10, 0)

    if (text.includes('今天') || text.includes('今日')) {
      startTime = new Date(now.getFullYear(), now.getMonth(), now.getDate(), now.getHours() + 1, 0)
      endTime = new Date(now.getFullYear(), now.getMonth(), now.getDate(), now.getHours() + 2, 0)
    } else if (text.includes('明天') || text.includes('明日')) {
      startTime = new Date(now.getFullYear(), now.getMonth(), now.getDate() + 1, 9, 0)
      endTime = new Date(now.getFullYear(), now.getMonth(), now.getDate() + 1, 10, 0)
    } else if (text.includes('后天')) {
      startTime = new Date(now.getFullYear(), now.getMonth(), now.getDate() + 2, 9, 0)
      endTime = new Date(now.getFullYear(), now.getMonth(), now.getDate() + 2, 10, 0)
    } else if (text.includes('下周')) {
      startTime = new Date(now.getFullYear(), now.getMonth(), now.getDate() + 7, 9, 0)
      endTime = new Date(now.getFullYear(), now.getMonth(), now.getDate() + 7, 10, 0)
    }

    const hourMatch = text.match(/(\d{1,2})[点时:：](\d{0,2})[分]?/)
    if (hourMatch) {
      const h = parseInt(hourMatch[1])
      const m = hourMatch[2] ? parseInt(hourMatch[2]) : 0
      if (h >= 0 && h <= 23) {
        startTime.setHours(h, m, 0, 0)
        endTime = new Date(startTime.getTime() + 3600000)
      }
    }

    return {
      action: 'create',
      title: text,
      description: '',
      startTime: startTime.toISOString(),
      endTime: endTime.toISOString(),
      confidence: 0.85
    }
  }

  async function waitForWsOpen(): Promise<boolean> {
    if (wsStatus.value === 'open') return true
    connect()
    return new Promise((resolve) => {
      let checks = 0
      const interval = setInterval(() => {
        if (wsStatus.value === 'open') {
          clearInterval(interval)
          resolve(true)
        } else if (++checks > 50) {
          clearInterval(interval)
          resolve(false)
        }
      }, 100)
    })
  }

  function startVad() {
    lastVoiceTime = Date.now()
    vadTimer = setInterval(() => {
      if (!isRecording) return
      if (Date.now() - lastVoiceTime > SILENCE_TIMEOUT_MS) {
        console.log('VAD: silence timeout, auto-stopping')
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

  async function startRecording(): Promise<boolean> {
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
      return false
    }

    mediaStream = stream
    sessionId = generateSessionId()
    audioChunksSent = 0
    errorMessage.value = ''
    partialText.value = ''

    const wsReady = await waitForWsOpen()
    setupWsHandlers()

    if (!wsReady) {
      console.warn('WebSocket not ready')
      stream.getTracks().forEach(t => t.stop())
      mediaStream = null
      return false
    }

    send(JSON.stringify({ type: 'start', sessionId }))
    console.log('Voice start sent, sessionId:', sessionId)

    const ctx = new AudioContext()
    await ctx.resume()
    console.log('AudioContext created, sampleRate:', ctx.sampleRate, 'state:', ctx.state)

    const source = ctx.createMediaStreamSource(stream)

    analyserNode = ctx.createAnalyser()
    analyserNode.fftSize = 128
    source.connect(analyserNode)
    updateAmplitude()

    scriptNode = ctx.createScriptProcessor(BUFFER_SIZE, 1, 1)

    const actualSampleRate = ctx.sampleRate

    scriptNode.onaudioprocess = (e) => {
      if (!isRecording) return
      const float32 = e.inputBuffer.getChannelData(0)

      let sumSq = 0
      for (let i = 0; i < float32.length; i++) {
        sumSq += float32[i] * float32[i]
      }
      const rms = Math.sqrt(sumSq / float32.length)

      if (rms > VAD_THRESHOLD) {
        lastVoiceTime = Date.now()
      }

      if (wsStatus.value === 'open') {
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

    source.connect(scriptNode)
    const muteGain = ctx.createGain()
    muteGain.gain.value = 0
    scriptNode.connect(muteGain)
    muteGain.connect(ctx.destination)

    audioContext = ctx

    isRecording = true
    status.value = 'recording'
    startVad()

    return true
  }

  async function stopRecording() {
    console.log('stopRecording, chunks sent:', audioChunksSent)

    if (audioContext && audioContext.state === 'running') {
      await audioContext.suspend()
    }

    isRecording = false
    stopVad()
    stopAmplitudeLoop()

    if (scriptNode) {
      scriptNode.disconnect()
      scriptNode = null
    }
    if (analyserNode) {
      analyserNode.disconnect()
      analyserNode = null
    }
    if (mediaStream) {
      mediaStream.getTracks().forEach(t => t.stop())
      mediaStream = null
    }
    if (audioContext) {
      audioContext.close().catch(() => {})
      audioContext = null
    }

    audioChunksSent = 0

    status.value = 'processing'

    if (wsStatus.value === 'open') {
      send(JSON.stringify({ type: 'end', sessionId }))
      console.log('Voice end sent')
      asrTimeout = setTimeout(() => {
        if (status.value === 'processing') {
          console.warn('ASR timeout, falling back to mock')
          errorMessage.value = '识别超时，使用离线识别'
          fallbackToMock()
        }
      }, ASR_TIMEOUT_MS)
    } else {
      console.warn('WS not open for end, mock immediately')
      fallbackToMock()
    }
  }

  function openOverlay() {
    isOverlayOpen.value = true
    status.value = 'listening'
    transcript.value = ''
    partialText.value = ''
    errorMessage.value = ''
    parsedIntent.value = null
  }

  function closeOverlay() {
    isOverlayOpen.value = false
    status.value = 'idle'
    isRecording = false
    audioChunksSent = 0
    stopVad()
    if (asrTimeout) { clearTimeout(asrTimeout); asrTimeout = null }
    stopAmplitudeLoop()
    if (scriptNode) { scriptNode.disconnect(); scriptNode = null }
    if (analyserNode) { analyserNode.disconnect(); analyserNode = null }
    if (mediaStream) { mediaStream.getTracks().forEach(t => t.stop()); mediaStream = null }
    if (audioContext) { audioContext.close().catch(() => {}); audioContext = null }
    disconnect()
  }

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
