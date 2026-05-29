export type VoiceStatus = 'idle' | 'listening' | 'recording' | 'processing' | 'result'

export interface ParsedIntent {
  action: 'create' | 'query' | 'search' | 'delete' | 'unknown'
  title?: string
  description?: string
  startTime?: string
  endTime?: string
  allDay?: boolean
  query?: string
  confidence: number
}

export interface ASRResult {
  text: string
  isFinal: boolean
}
