import { ref, computed } from 'vue'
import request from '@/utils/request'
import type { ApiResponse } from '@/types/api'
import type { LoginRequest, RegisterRequest, LoginUserVO, User, AuthState } from '@/types/auth'

const AUTH_KEY = 'voicecal_token'
const USER_KEY = 'voicecal_user'

const state = ref<AuthState>({
  user: null,
  token: null,
  isAuthenticated: false,
  loading: false,
  error: null
})

function loadFromStorage() {
  const token = localStorage.getItem(AUTH_KEY)
  const userStr = localStorage.getItem(USER_KEY)
  if (token && userStr) {
    try {
      const user = JSON.parse(userStr) as User
      state.value.token = token
      state.value.user = user
      state.value.isAuthenticated = true
    } catch {
      clearAuth()
    }
  }
}

function saveAuth(token: string, user: User) {
  localStorage.setItem(AUTH_KEY, token)
  localStorage.setItem(USER_KEY, JSON.stringify(user))
  state.value.token = token
  state.value.user = user
  state.value.isAuthenticated = true
  state.value.error = null
}

function clearAuth() {
  localStorage.removeItem(AUTH_KEY)
  localStorage.removeItem(USER_KEY)
  state.value.token = null
  state.value.user = null
  state.value.isAuthenticated = false
  state.value.error = null
}

loadFromStorage()

window.addEventListener('auth:unauthorized', () => {
  clearAuth()
})

export function useAuth() {
  async function login(req: LoginRequest) {
    state.value.loading = true
    state.value.error = null
    try {
      const res = await request.post<ApiResponse<LoginUserVO>>('/auth/login', req)
      if (res.code !== 200) {
        state.value.error = res.message || '登录失败'
        return false
      }
      const vo = res.data
      const user: User = {
        id: vo.userId,
        username: vo.username,
        nickname: vo.nickname || vo.username
      }
      saveAuth(vo.token, user)
      return true
    } catch (e: any) {
      state.value.error = e.response?.data?.message || '网络错误，请稍后重试'
      return false
    } finally {
      state.value.loading = false
    }
  }

  async function register(req: RegisterRequest) {
    state.value.loading = true
    state.value.error = null
    try {
      const res = await request.post<ApiResponse<LoginUserVO>>('/auth/register', req)
      if (res.code !== 200 && res.code !== 201) {
        state.value.error = res.message || '注册失败'
        return false
      }
      const vo = res.data
      const user: User = {
        id: vo.userId,
        username: vo.username,
        nickname: vo.nickname || vo.username
      }
      saveAuth(vo.token, user)
      return true
    } catch (e: any) {
      state.value.error = e.response?.data?.message || '网络错误，请稍后重试'
      return false
    } finally {
      state.value.loading = false
    }
  }

  async function logout() {
    try {
      await request.post('/auth/logout', { token: state.value.token })
    } catch {
      // ignore network errors on logout
    }
    clearAuth()
  }

  function clearError() {
    state.value.error = null
  }

  // 头像本地存储
  const AVATAR_KEY = 'voicecal_avatar'

  function loadAvatar(): string {
    return localStorage.getItem(AVATAR_KEY) || ''
  }

  function saveAvatar(base64: string) {
    localStorage.setItem(AVATAR_KEY, base64)
    if (state.value.user) {
      state.value.user = { ...state.value.user, avatar: base64 }
    }
  }

  async function changePassword(oldPassword: string, newPassword: string) {
    try {
      const res = await request.patch<ApiResponse<void>>('/users/me/password', { oldPassword, newPassword })
      if (res.code === 200) {
        return { success: true }
      }
      return { success: false, message: res.message || '修改失败' }
    } catch (e: any) {
      return { success: false, message: e.response?.data?.message || '网络错误' }
    }
  }

  return {
    state: computed(() => state.value),
    isAuthenticated: computed(() => state.value.isAuthenticated),
    user: computed(() => state.value.user),
    token: computed(() => state.value.token),
    loading: computed(() => state.value.loading),
    error: computed(() => state.value.error),
    login,
    register,
    logout,
    clearError,
    loadAvatar,
    saveAvatar,
    changePassword
  }
}
