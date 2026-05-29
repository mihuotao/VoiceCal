import { ref } from 'vue'

const BG_KEY = 'voicecal_custom_bg'
const customBg = ref<string | null>(null)

function loadCustomBg() {
  try {
    const stored = localStorage.getItem(BG_KEY)
    if (stored) customBg.value = stored
  } catch {
    // ignore
  }
}

function setCustomBg(dataUrl: string) {
  customBg.value = dataUrl
  try {
    localStorage.setItem(BG_KEY, dataUrl)
  } catch {
    // localStorage full, ignore
  }
}

function clearCustomBg() {
  customBg.value = null
  localStorage.removeItem(BG_KEY)
}

loadCustomBg()

export function useCustomBg() {
  return {
    customBg,
    setCustomBg,
    clearCustomBg
  }
}
