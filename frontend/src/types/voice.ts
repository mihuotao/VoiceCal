export type VoiceStatus = 'idle' | 'listening' | 'recording' | 'processing' | 'result' | 'error'

export interface ParsedIntent {
  action: 'create' | 'created' | 'preview' | 'query' | 'update' | 'delete' | 'reminder' | 'clarify' | 'conflict' | 'unknown'
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
  // 预览/创建成功字段
  event?: Record<string, unknown>
  // 澄清字段
  clarifyQuestion?: string
  missingField?: string
  // 冲突字段
  conflicts?: import('@/types/event').CalendarEvent[]
}

export interface ASRResult {
  text: string
  isFinal: boolean
}
