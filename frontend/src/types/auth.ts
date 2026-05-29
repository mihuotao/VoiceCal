export interface LoginRequest {
  username: string
  password: string
}

export interface RegisterRequest {
  username: string
  password: string
  nickname?: string
  email?: string
  phone?: string
}

export interface LoginUserVO {
  token: string
  tokenType?: string
  expiresIn: number
  userId: number
  username: string
  nickname?: string
  lastLoginAt?: string
}

export interface User {
  id: number
  username: string
  nickname: string
  email?: string
  phone?: string
  avatar?: string
}

export interface AuthState {
  user: User | null
  token: string | null
  isAuthenticated: boolean
  loading: boolean
  error: string | null
}
