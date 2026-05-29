export interface UserPreference {
  id?: number
  userId?: number
  defaultView?: string
  defaultCategory?: string
  defaultReminder?: number
  language?: string
  weekStartDay?: number
  workingHoursStart?: string
  workingHoursEnd?: string
  ttsEnabled?: boolean
  ttsVoiceType?: string
  ttsSpeed?: number
  notificationEnabled?: boolean
  theme?: string
  glassBlur?: number
  glassOpacity?: number
  primaryColor?: string
  bgGradient?: string
  bgColor?: string
  createdAt?: string
  updatedAt?: string
}

export interface UpdatePreferenceRequest {
  defaultView?: string
  defaultCategory?: string
  defaultReminder?: number
  language?: string
  weekStartDay?: number
  workingHoursStart?: string
  workingHoursEnd?: string
  ttsEnabled?: boolean
  ttsVoiceType?: string
  ttsSpeed?: number
  notificationEnabled?: boolean
  theme?: string
  glassBlur?: number
  glassOpacity?: number
  primaryColor?: string
  bgGradient?: string
  bgColor?: string
}
