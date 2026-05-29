import { ref, computed } from 'vue'
import axios from 'axios'
import type { LoginRequest, RegisterRequest, LoginUserVO, User, AuthState } from '@/types/auth'

const AUTH_KEY = 'voicecal_token'
const USER_KEY = 'voicecal_user'
const API_BASE = '/api/v1/auth'

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
      setAuthHeader(token)
    } catch {
      clearAuth()
    }
  }
}

function setAuthHeader(token: string | null) {
  if (token) {
    axios.defaults.headers.common['Authorization'] = `Bearer ${token}`
  } else {
    delete axios.defaults.headers.common['Authorization']
  }
}

function saveAuth(token: string, user: User) {
  localStorage.setItem(AUTH_KEY, token)
  localStorage.setItem(USER_KEY, JSON.stringify(user))
  state.value.token = token
  state.value.user = user
  state.value.isAuthenticated = true
  state.value.error = null
  setAuthHeader(token)
}

function clearAuth() {
  localStorage.removeItem(AUTH_KEY)
  localStorage.removeItem(USER_KEY)
  state.value.token = null
  state.value.user = null
  state.value.isAuthenticated = false
  state.value.error = null
  setAuthHeader(null)
}

axios.interceptors.response.use(
  response => response,
  error => {
    if (error.response?.status === 401 && state.value.isAuthenticated) {
      clearAuth()
    }
    return Promise.reject(error)
  }
)

loadFromStorage()

export function useAuth() {
  async function login(req: LoginRequest) {
    state.value.loading = true
    state.value.error = null
    try {
      const res = await axios.post<{ code: number; message: string; data: LoginUserVO }>(`${API_BASE}/login`, req)
      if (res.data.code !== 0 && res.data.code !== 200) {
        state.value.error = res.data.message || '登录失败'
        return false
      }
      const vo = res.data.data
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
      const res = await axios.post<{ code: number; message: string; data: LoginUserVO }>(`${API_BASE}/register`, req)
      if (res.data.code !== 0 && res.data.code !== 201 && res.data.code !== 200) {
        state.value.error = res.data.message || '注册失败'
        return false
      }
      const vo = res.data.data
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
      await axios.post(`${API_BASE}/logout`, { token: state.value.token })
    } catch {
      // ignore network errors on logout
    }
    clearAuth()
  }

  function clearError() {
    state.value.error = null
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
    clearError
  }
}
