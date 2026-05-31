export type VoiceStatus = 'idle' | 'listening' | 'recording' | 'processing' | 'result' | 'error'

export interface ParsedIntent {
  action: 'create' | 'query' | 'update' | 'delete' | 'reminder' | 'clarify' | 'unknown'
  title?: string
  description?: string
  startTime?: string
  endTime?: string
  allDay?: boolean
  query?: string
  confidence: number
  // 查询结果字段
  queryDate?: string
  queryEndDate?: string
  events?: import('@/types/event').CalendarEvent[]
  responseText?: string
  // 澄清字段
  clarifyQuestion?: string
}

export interface ASRResult {
  text: string
  isFinal: boolean
}
