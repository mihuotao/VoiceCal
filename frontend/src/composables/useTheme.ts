import { computed } from 'vue'
import { useThemeStore } from '@/stores/theme'

export function useTheme() {
  const store = useThemeStore()

  const glassStyle = computed(() => ({
    '--glass-blur': `${store.state.glassBlur}px`,
    '--glass-opacity': String(store.state.glassOpacity),
    '--color-primary': store.state.primaryColor
  }))

  const backgroundStyle = computed(() => ({
    background: store.state.bgGradient,
    backgroundColor: store.state.bgColor
  }))

  return {
    state: store.state,
    glassStyle,
    backgroundStyle,
    setGlassBlur: store.setGlassBlur,
    setGlassOpacity: store.setGlassOpacity,
    setPrimaryColor: store.setPrimaryColor,
    setBgGradient: store.setBgGradient,
    setBgColor: store.setBgColor,
    resetTheme: store.resetTheme
  }
}
