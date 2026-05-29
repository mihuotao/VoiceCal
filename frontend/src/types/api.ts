export interface ApiResponse<T> {
  code: number
  message: string
  data: T
  page?: PageInfo
  errors?: FieldError[]
  timestamp: number
}

export interface PageInfo {
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface FieldError {
  field: string
  message: string
}
