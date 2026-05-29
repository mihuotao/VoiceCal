export interface CalendarEvent {
  id: number
  userId: number
  title: string
  description?: string
  startTime: string
  endTime: string
  allDay?: boolean
  location?: string
  category?: string
  color?: string
  priority?: number
  status?: string
}

export interface EventCreateRequest {
  title: string
  description?: string
  startTime: string
  endTime: string
  allDay?: boolean
  location?: string
  category?: string
  color?: string
  priority?: number
  reminderMinutes?: number
}
