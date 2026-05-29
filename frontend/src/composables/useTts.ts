import { ref } from 'vue'

const speaking = ref(false)
const supported = ref(false)

function init() {
  supported.value = typeof window !== 'undefined' && 'speechSynthesis' in window
}

function speak(text: string, lang = 'zh-CN', rate = 1, pitch = 1) {
  if (!supported.value) return
  stop()

  const utterance = new SpeechSynthesisUtterance(text)
  utterance.lang = lang
  utterance.rate = rate
  utterance.pitch = pitch
  utterance.volume = 1

  const voices = speechSynthesis.getVoices()
  const zhVoice = voices.find(v => v.lang.startsWith('zh'))
  if (zhVoice) utterance.voice = zhVoice

  utterance.onstart = () => { speaking.value = true }
  utterance.onend = () => { speaking.value = false }
  utterance.onerror = () => { speaking.value = false }

  speechSynthesis.speak(utterance)
}

function stop() {
  if (speechSynthesis.speaking) {
    speechSynthesis.cancel()
  }
  speaking.value = false
}

init()

export function useTts() {
  return {
    speaking,
    supported,
    speak,
    stop
  }
}
