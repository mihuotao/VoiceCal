import { ref } from 'vue'

export type VoiceStatus = 'idle' | 'listening' | 'processing' | 'result'

export function useVoice() {
  const status = ref<VoiceStatus>('idle')
  const amplitude = ref(0)
  const isOverlayOpen = ref(false)

  let amplitudeInterval: ReturnType<typeof setInterval> | null = null

  function simulateAmplitude() {
    amplitudeInterval = setInterval(() => {
      amplitude.value = 0.2 + Math.random() * 0.8
    }, 80)
  }

  function stopAmplitude() {
    if (amplitudeInterval) {
      clearInterval(amplitudeInterval)
      amplitudeInterval = null
    }
    amplitude.value = 0
  }

  function openOverlay() {
    isOverlayOpen.value = true
    status.value = 'listening'
    simulateAmplitude()
  }

  function closeOverlay() {
    isOverlayOpen.value = false
    status.value = 'idle'
    stopAmplitude()
  }

  function setStatus(s: VoiceStatus) {
    status.value = s
    if (s === 'listening') {
      simulateAmplitude()
    } else if (s === 'idle') {
      stopAmplitude()
    }
  }

  function cleanup() {
    stopAmplitude()
  }

  return {
    status,
    amplitude,
    isOverlayOpen,
    openOverlay,
    closeOverlay,
    setStatus,
    cleanup
  }
}
