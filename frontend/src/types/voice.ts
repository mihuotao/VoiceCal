export type VoiceStatus = 'idle' | 'listening' | 'recording' | 'processing' | 'result' | 'error'

export interface ParsedIntent {
  action: 'create' | 'query' | 'search' | 'delete' | 'unknown'
  title?: string
  description?: string
  startTime?: string
  endTime?: string
  allDay?: boolean
  query?: string
  confidence: number
  // 查询结果字段
  queryDate?: string
  events?: import('@/types/event').CalendarEvent[]
  responseText?: string
}

export interface ASRResult {
  text: string
  isFinal: boolean
}
