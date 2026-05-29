import { ref } from 'vue'
import { useWebSocket } from '@/composables/useWebSocket'
import type { VoiceStatus, ASRResult, ParsedIntent } from '@/types/voice'

const WS_URL = `${location.protocol === 'https:' ? 'wss:' : 'ws:'}//${location.host}/ws/voice`

export function useVoice() {
  const status = ref<VoiceStatus>('idle')
  const amplitude = ref(0)
  const isOverlayOpen = ref(false)
  const transcript = ref('')
  const partialText = ref('')
  const parsedIntent = ref<ParsedIntent | null>(null)

  let mediaRecorder: MediaRecorder | null = null
  let audioContext: AudioContext | null = null
  let analyserNode: AnalyserNode | null = null
  let audioDataArray: Uint8Array | null = null
  let rafId = 0
  let audioChunks: Blob[] = []
  let sessionId = ''

  const { wsStatus, connect, disconnect, send, onMessage } = useWebSocket(WS_URL)

  function startRealAmplitude(stream: MediaStream) {
    audioContext = new AudioContext()
    const source = audioContext.createMediaStreamSource(stream)
    analyserNode = audioContext.createAnalyser()
    analyserNode.fftSize = 128
    source.connect(analyserNode)
    audioDataArray = new Uint8Array(analyserNode.frequencyBinCount)

    function updateAmplitude() {
      if (!analyserNode || !audioDataArray) return
      analyserNode.getByteFrequencyData(audioDataArray)
      const avg = Array.from(audioDataArray).reduce((a, b) => a + b, 0) / audioDataArray.length
      amplitude.value = Math.min(1, avg / 128)
      rafId = requestAnimationFrame(updateAmplitude)
    }
    updateAmplitude()
  }

  function stopRealAmplitude() {
    cancelAnimationFrame(rafId)
    if (audioContext) {
      audioContext.close().catch(() => {})
      audioContext = null
    }
    analyserNode = null
    audioDataArray = null
    amplitude.value = 0
  }

  function stopAmplitude() {
    stopRealAmplitude()
  }

  function generateSessionId() {
    return `s_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`
  }

  function setupWsHandlers() {
    onMessage((data: string) => {
      try {
        const msg = JSON.parse(data)
        switch (msg.type) {
          case 'intermediate_result':
            if (msg.payload.isFinal) {
              transcript.value = msg.payload.text
              partialText.value = ''
            } else {
              partialText.value = msg.payload.text
            }
            break

          case 'final_result':
            status.value = 'result'
            transcript.value = transcript.value || msg.payload.title || ''
            parsedIntent.value = {
              action: msg.payload.action || 'create',
              title: msg.payload.title || transcript.value,
              description: msg.payload.description || '',
              startTime: msg.payload.startTime || new Date(Date.now() + 86400000).toISOString(),
              endTime: msg.payload.endTime || new Date(Date.now() + 86400000 + 3600000).toISOString(),
              confidence: msg.payload.confidence || 0.9
            }
            break

          case 'error':
            console.warn('ASR error:', msg.payload.message)
            fallbackToMock()
            break
        }
      } catch {
        // ignore parse errors
      }
    })
  }

  function fallbackToMock() {
    setTimeout(() => {
      const mockResult: ASRResult = {
        text: mockASR(),
        isFinal: true
      }
      transcript.value = mockResult.text
      parsedIntent.value = parseIntent(mockResult.text)
      status.value = 'result'
    }, 800 + Math.random() * 600)
  }

  async function startRecording(): Promise<boolean> {
    let stream: MediaStream | null = null
    try {
      stream = await navigator.mediaDevices.getUserMedia({ audio: true })
    } catch {
      return false
    }

    audioChunks = []
    sessionId = generateSessionId()
    startRealAmplitude(stream)

    setupWsHandlers()
    if (wsStatus.value !== 'open') {
      connect()
    }

    const mimeType = MediaRecorder.isTypeSupported('audio/webm;codecs=opus')
      ? 'audio/webm;codecs=opus'
      : 'audio/webm'

    mediaRecorder = new MediaRecorder(stream, { mimeType })
    mediaRecorder.ondataavailable = (e) => {
      if (e.data.size > 0) audioChunks.push(e.data)
    }
    mediaRecorder.onstop = () => {
      stream?.getTracks().forEach(t => t.stop())
      stopRealAmplitude()
      const blob = new Blob(audioChunks, { type: mimeType })
      sendToASR(blob)
    }
    mediaRecorder.start()
    status.value = 'recording'
    return true
  }

  function stopRecording() {
    if (mediaRecorder && mediaRecorder.state !== 'inactive') {
      mediaRecorder.stop()
    }
    mediaRecorder = null
    status.value = 'processing'
    stopAmplitude()
  }

  async function sendToASR(audioBlob: Blob) {
    if (wsStatus.value === 'open') {
      try {
        const buffer = await audioBlob.arrayBuffer()
        const base64 = arrayBufferToBase64(buffer)
        send(JSON.stringify({
          type: 'audio_data',
          sessionId,
          payload: {
            audioBase64: base64,
            format: 'webm',
            sampleRate: 48000
          }
        }))
        return
      } catch {
        // fall through to mock
      }
    }
    fallbackToMock()
  }

  function arrayBufferToBase64(buffer: ArrayBuffer): string {
    const bytes = new Uint8Array(buffer)
    let binary = ''
    for (let i = 0; i < bytes.length; i++) {
      binary += String.fromCharCode(bytes[i])
    }
    return btoa(binary)
  }

  function mockASR(): string {
    const samples = [
      '明天上午十点开会',
      '下周一早上九点去医院体检',
      '这周五晚上七点跟朋友吃饭',
      '搜索明天的北京天气',
      '今天下午三点约了客户',
      '后天下午去健身房'
    ]
    return samples[Math.floor(Math.random() * samples.length)]
  }

  function parseIntent(_text: string): ParsedIntent {
    return {
      action: 'create',
      title: _text,
      description: '',
      startTime: new Date(Date.now() + 86400000).toISOString(),
      endTime: new Date(Date.now() + 86400000 + 3600000).toISOString(),
      confidence: 0.85
    }
  }

  function openOverlay() {
    isOverlayOpen.value = true
    status.value = 'listening'
    transcript.value = ''
    partialText.value = ''
    parsedIntent.value = null
  }

  function closeOverlay() {
    isOverlayOpen.value = false
    status.value = 'idle'
    stopAmplitude()
    if (mediaRecorder && mediaRecorder.state !== 'inactive') {
      mediaRecorder.stop()
    }
    mediaRecorder = null
    disconnect()
  }

  function setStatus(s: VoiceStatus) {
    status.value = s
  }

  function setPartial(text: string) {
    partialText.value = text
  }

  function cleanup() {
    stopRealAmplitude()
    if (mediaRecorder && mediaRecorder.state !== 'inactive') {
      mediaRecorder.stop()
    }
    mediaRecorder = null
    disconnect()
  }

  return {
    status,
    amplitude,
    isOverlayOpen,
    transcript,
    partialText,
    parsedIntent,
    startRecording,
    stopRecording,
    openOverlay,
    closeOverlay,
    setStatus,
    setPartial,
    cleanup
  }
}
