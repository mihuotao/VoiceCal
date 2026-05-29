import { ref, computed } from 'vue'
import axios from 'axios'
import type { CalendarEvent, EventCreateRequest } from '@/types/event'

const API_BASE = '/api/events'

let nextId = 1

export function useEvents() {
  const events = ref<CalendarEvent[]>([])
  const loading = ref(false)
  const error = ref<string | null>(null)

  const eventsByDate = computed(() => {
    const map: Record<string, CalendarEvent[]> = {}
    for (const ev of events.value) {
      const dateKey = ev.startTime.slice(0, 10)
      if (!map[dateKey]) map[dateKey] = []
      map[dateKey].push(ev)
    }
    return map
  })

  const datesWithEvents = computed(() => new Set(Object.keys(eventsByDate.value)))

  function getEventsForDate(dateKey: string): CalendarEvent[] {
    return eventsByDate.value[dateKey] ?? []
  }

  async function fetchEvents(start?: string, end?: string) {
    loading.value = true
    error.value = null
    try {
      const params: Record<string, string> = {}
      if (start) params.start = start
      if (end) params.end = end
      const res = await axios.get<CalendarEvent[]>(API_BASE, { params })
      events.value = res.data
    } catch {
      if (events.value.length === 0) {
        seedMockEvents()
      }
    } finally {
      loading.value = false
    }
  }

  async function createEvent(req: EventCreateRequest): Promise<CalendarEvent | null> {
    try {
      const res = await axios.post<CalendarEvent>(API_BASE, req)
      events.value.push(res.data)
      return res.data
    } catch {
      const mock: CalendarEvent = {
        id: nextId++,
        userId: 1,
        title: req.title,
        description: req.description,
        startTime: req.startTime,
        endTime: req.endTime,
        allDay: req.allDay,
        location: req.location,
        category: req.category,
        color: req.color,
        priority: req.priority
      }
      events.value.push(mock)
      return mock
    }
  }

  async function updateEvent(id: number, req: Partial<EventCreateRequest>): Promise<boolean> {
    try {
      await axios.put(`${API_BASE}/${id}`, req)
      const idx = events.value.findIndex(e => e.id === id)
      if (idx >= 0) Object.assign(events.value[idx], req)
      return true
    } catch {
      const idx = events.value.findIndex(e => e.id === id)
      if (idx >= 0) Object.assign(events.value[idx], req)
      return true
    }
  }

  async function deleteEvent(id: number): Promise<boolean> {
    try {
      await axios.delete(`${API_BASE}/${id}`)
      events.value = events.value.filter(e => e.id !== id)
      return true
    } catch {
      events.value = events.value.filter(e => e.id !== id)
      return true
    }
  }

  function seedMockEvents() {
    const today = new Date()
    const mocks: CalendarEvent[] = [
      {
        id: nextId++, userId: 1, title: '团队周会',
        startTime: new Date(today.getFullYear(), today.getMonth(), today.getDate(), 10, 0).toISOString(),
        endTime: new Date(today.getFullYear(), today.getMonth(), today.getDate(), 11, 0).toISOString(),
        color: '#6366f1', category: 'work'
      },
      {
        id: nextId++, userId: 1, title: '午餐约会',
        startTime: new Date(today.getFullYear(), today.getMonth(), today.getDate(), 12, 0).toISOString(),
        endTime: new Date(today.getFullYear(), today.getMonth(), today.getDate(), 13, 0).toISOString(),
        color: '#22c55e', category: 'personal'
      },
      {
        id: nextId++, userId: 1, title: '项目评审',
        startTime: new Date(today.getFullYear(), today.getMonth(), today.getDate() + 1, 14, 0).toISOString(),
        endTime: new Date(today.getFullYear(), today.getMonth(), today.getDate() + 1, 16, 0).toISOString(),
        color: '#f59e0b', category: 'work'
      }
    ]
    events.value = mocks
  }

  function checkConflicts(startTime: string, endTime: string, excludeId?: number): CalendarEvent[] {
    const start = new Date(startTime).getTime()
    const end = new Date(endTime).getTime()
    return events.value.filter(ev => {
      if (excludeId !== undefined && ev.id === excludeId) return false
      const es = new Date(ev.startTime).getTime()
      const ee = new Date(ev.endTime).getTime()
      return start < ee && end > es
    })
  }

  return {
    events,
    loading,
    error,
    eventsByDate,
    datesWithEvents,
    getEventsForDate,
    fetchEvents,
    createEvent,
    updateEvent,
    deleteEvent,
    checkConflicts,
    seedMockEvents
  }
}
