import axios from 'axios'
import type { AxiosRequestConfig } from 'axios'
import type { ApiResponse } from '@/types/api'

const instance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api/v1',
  timeout: 10000
})

instance.interceptors.request.use((config) => {
  const token = localStorage.getItem('voicecal_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

instance.interceptors.response.use(
  (res) => res,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('voicecal_token')
      localStorage.removeItem('voicecal_user')
      window.dispatchEvent(new CustomEvent('auth:unauthorized'))
    }
    return Promise.reject(error)
  }
)

async function get<T>(url: string, config?: AxiosRequestConfig): Promise<T> {
  const res = await instance.get<T>(url, config)
  return res.data
}

async function post<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
  const res = await instance.post<T>(url, data, config)
  return res.data
}

async function put<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
  const res = await instance.put<T>(url, data, config)
  return res.data
}

async function patch<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
  const res = await instance.patch<T>(url, data, config)
  return res.data
}

async function del<T = void>(url: string, config?: AxiosRequestConfig): Promise<T> {
  const res = await instance.delete<T>(url, config)
  return res.data
}

const request = { get, post, put, patch, delete: del }

export default request
export type { ApiResponse }
