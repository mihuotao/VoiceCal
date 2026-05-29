import { ref } from 'vue'
import request from '@/utils/request'
import { useAuth } from '@/composables/useAuth'
import { useThemeStore } from '@/stores/theme'
import type { ApiResponse } from '@/types/api'
import type { UserPreference, UpdatePreferenceRequest } from '@/types/preference'

const PREF_KEY = 'voicecal_preferences'

export interface PreferenceState {
  defaultView: string
  defaultCategory: string
  defaultReminder: number
  weekStartDay: number
  workingHoursStart: string
  workingHoursEnd: string
  ttsEnabled: boolean
  ttsVoiceType: string
  ttsSpeed: number
  notificationEnabled: boolean
}

const defaultPreferences: PreferenceState = {
  defaultView: 'month',
  defaultCategory: 'personal',
  defaultReminder: 30,
  weekStartDay: 0,
  workingHoursStart: '09:00',
  workingHoursEnd: '18:00',
  ttsEnabled: false,
  ttsVoiceType: 'zh-CN-XiaoxiaoNeural',
  ttsSpeed: 100,
  notificationEnabled: true
}

const prefs = ref<PreferenceState>({ ...defaultPreferences })
const loading = ref(false)

function loadLocal() {
  try {
    const raw = localStorage.getItem(PREF_KEY)
    if (raw) {
      const parsed = JSON.parse(raw)
      prefs.value = { ...defaultPreferences, ...parsed }
    }
  } catch {
    // ignore
  }
}

function saveLocal() {
  localStorage.setItem(PREF_KEY, JSON.stringify(prefs.value))
}

function applyThemeCSS(pref: UserPreference) {
  const theme = useThemeStore()
  if (pref.glassBlur !== undefined) theme.setGlassBlur(pref.glassBlur)
  if (pref.glassOpacity !== undefined) theme.setGlassOpacity(pref.glassOpacity)
  if (pref.primaryColor) theme.setPrimaryColor(pref.primaryColor)
  if (pref.bgGradient) theme.setBgGradient(pref.bgGradient)
  if (pref.bgColor) theme.setBgColor(pref.bgColor)
}

async function fetchPreferences() {
  const { isAuthenticated } = useAuth()
  if (!isAuthenticated.value) return

  loading.value = true
  try {
    const res = await request.get<ApiResponse<UserPreference>>('/preferences')
    if (res.code === 200) {
      const p = res.data
      if (p.defaultView) prefs.value.defaultView = p.defaultView
      if (p.defaultCategory) prefs.value.defaultCategory = p.defaultCategory
      if (p.defaultReminder !== undefined) prefs.value.defaultReminder = p.defaultReminder
      if (p.weekStartDay !== undefined) prefs.value.weekStartDay = p.weekStartDay
      if (p.workingHoursStart) prefs.value.workingHoursStart = p.workingHoursStart
      if (p.workingHoursEnd) prefs.value.workingHoursEnd = p.workingHoursEnd
      if (p.ttsEnabled !== undefined) prefs.value.ttsEnabled = p.ttsEnabled
      if (p.ttsVoiceType) prefs.value.ttsVoiceType = p.ttsVoiceType
      if (p.ttsSpeed !== undefined) prefs.value.ttsSpeed = p.ttsSpeed
      if (p.notificationEnabled !== undefined) prefs.value.notificationEnabled = p.notificationEnabled
      applyThemeCSS(p)
      saveLocal()
    }
  } catch {
    loadLocal()
  } finally {
    loading.value = false
  }
}

async function updatePreferences(update: UpdatePreferenceRequest) {
  const { isAuthenticated } = useAuth()

  if (update.glassBlur !== undefined) useThemeStore().setGlassBlur(update.glassBlur)
  if (update.glassOpacity !== undefined) useThemeStore().setGlassOpacity(update.glassOpacity)
  if (update.primaryColor) useThemeStore().setPrimaryColor(update.primaryColor)
  if (update.bgGradient) useThemeStore().setBgGradient(update.bgGradient)
  if (update.bgColor) useThemeStore().setBgColor(update.bgColor)

  if (update.defaultView) prefs.value.defaultView = update.defaultView
  if (update.defaultCategory) prefs.value.defaultCategory = update.defaultCategory
  if (update.defaultReminder !== undefined) prefs.value.defaultReminder = update.defaultReminder
  if (update.weekStartDay !== undefined) prefs.value.weekStartDay = update.weekStartDay
  if (update.workingHoursStart) prefs.value.workingHoursStart = update.workingHoursStart
  if (update.workingHoursEnd) prefs.value.workingHoursEnd = update.workingHoursEnd
  if (update.ttsEnabled !== undefined) prefs.value.ttsEnabled = update.ttsEnabled
  if (update.ttsVoiceType) prefs.value.ttsVoiceType = update.ttsVoiceType
  if (update.ttsSpeed !== undefined) prefs.value.ttsSpeed = update.ttsSpeed
  if (update.notificationEnabled !== undefined) prefs.value.notificationEnabled = update.notificationEnabled

  saveLocal()

  if (isAuthenticated.value) {
    try {
      await request.patch('/preferences', update)
    } catch {
      // silently fail - local changes are already saved
    }
  }
}

loadLocal()

export function usePreferences() {
  return {
    prefs,
    loading,
    fetchPreferences,
    updatePreferences
  }
}
