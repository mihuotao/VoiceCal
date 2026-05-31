import { ref, computed, onMounted, onUnmounted } from 'vue'
import request from '@/utils/request'
import type { CalendarEvent, EventCreateRequest } from '@/types/event'

const API_BASE = '/events'
const AUTO_REFRESH_INTERVAL = 5000 // 5秒自动刷新

export function useEvents() {
  const events = ref<CalendarEvent[]>([])
  const loading = ref(false)
  const error = ref<string | null>(null)
  let refreshTimer: ReturnType<typeof setInterval> | null = null

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
      const res: any = await request.get(API_BASE, { params })
      if (res.code === 200) {
        events.value = res.data || []
      }
    } catch (err) {
      console.error('获取事件失败:', err)
    } finally {
      loading.value = false
    }
  }

  async function createEvent(req: EventCreateRequest): Promise<CalendarEvent | null> {
    try {
      const res: any = await request.post(API_BASE, req)
      if (res.code === 201) {
        events.value.push(res.data)
        return res.data
      }
      return null
    } catch (err) {
      console.error('创建事件失败:', err)
      return null
    }
  }

  async function updateEvent(id: number, req: Partial<EventCreateRequest>): Promise<boolean> {
    try {
      const res: any = await request.put(`${API_BASE}/${id}`, req)
      if (res.code === 200) {
        await fetchEvents()
        return true
      }
      return false
    } catch (err) {
      console.error('更新事件失败:', err)
      return false
    }
  }

  async function deleteEvent(id: number): Promise<boolean> {
    try {
      const res: any = await request.delete(`${API_BASE}/${id}`)
      if (res.code === 200) {
        events.value = events.value.filter(e => e.id !== id)
        return true
      }
      return false
    } catch (err) {
      console.error('删除事件失败:', err)
      return false
    }
  }

  function checkConflicts(startTime: string, endTime: string, excludeId?: number): CalendarEvent[] {
    // 解析 yyyy-MM-dd HH:mm:ss 格式
    const parseTime = (t: string) => {
      if (t.includes('T')) return new Date(t).getTime()
      // yyyy-MM-dd HH:mm:ss -> 可以被 Date 正确解析
      return new Date(t.replace(' ', 'T')).getTime()
    }
    const start = parseTime(startTime)
    const end = parseTime(endTime)
    return events.value.filter(ev => {
      if (excludeId !== undefined && ev.id === excludeId) return false
      const es = parseTime(ev.startTime)
      const ee = parseTime(ev.endTime)
      return start < ee && end > es
    })
  }

  // 启动自动刷新
  function startAutoRefresh() {
    if (refreshTimer) return
    refreshTimer = setInterval(() => {
      fetchEvents()
    }, AUTO_REFRESH_INTERVAL)
  }

  // 停止自动刷新
  function stopAutoRefresh() {
    if (refreshTimer) {
      clearInterval(refreshTimer)
      refreshTimer = null
    }
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
    startAutoRefresh,
    stopAutoRefresh
  }
}
