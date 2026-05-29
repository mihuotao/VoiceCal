export interface ThemeState {
  glassBlur: number
  glassOpacity: number
  primaryColor: string
  bgGradient: string
  bgColor: string
}

export const DEFAULT_THEME: ThemeState = {
  glassBlur: 32,
  glassOpacity: 0.45,
  primaryColor: '#6366f1',
  bgGradient: 'linear-gradient(135deg, #0f0c29, #302b63, #24243e)',
  bgColor: '#0f0c29'
}
