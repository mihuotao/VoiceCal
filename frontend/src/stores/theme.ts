import { defineStore } from 'pinia'
import { reactive } from 'vue'
import type { ThemeState } from '@/types/theme'
import { DEFAULT_THEME } from '@/types/theme'

export const useThemeStore = defineStore('theme', () => {
  const state = reactive<ThemeState>({ ...DEFAULT_THEME })

  function setGlassBlur(blur: number) {
    state.glassBlur = blur
    document.documentElement.style.setProperty('--glass-blur', `${blur}px`)
  }

  function setGlassOpacity(opacity: number) {
    state.glassOpacity = opacity
    document.documentElement.style.setProperty('--glass-opacity', String(opacity))
  }

  function setPrimaryColor(color: string) {
    state.primaryColor = color
    document.documentElement.style.setProperty('--color-primary', color)
  }

  function setBgGradient(gradient: string) {
    state.bgGradient = gradient
    document.documentElement.style.setProperty('--bg-gradient', gradient)
  }

  function setBgColor(color: string) {
    state.bgColor = color
    document.documentElement.style.setProperty('--bg-color', color)
  }

  function resetTheme() {
    setGlassBlur(DEFAULT_THEME.glassBlur)
    setGlassOpacity(DEFAULT_THEME.glassOpacity)
    setPrimaryColor(DEFAULT_THEME.primaryColor)
    setBgGradient(DEFAULT_THEME.bgGradient)
    setBgColor(DEFAULT_THEME.bgColor)
  }

  return {
    state,
    setGlassBlur,
    setGlassOpacity,
    setPrimaryColor,
    setBgGradient,
    setBgColor,
    resetTheme
  }
})
