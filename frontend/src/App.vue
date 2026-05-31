<script setup lang="ts">
import { ref, shallowRef, onMounted, onUnmounted, watch } from 'vue'
import CalendarHeader from '@/components/calendar/CalendarHeader.vue'
import CalendarGrid from '@/components/calendar/CalendarGrid.vue'
import CalendarInfoBar from '@/components/calendar/CalendarInfoBar.vue'
import StarryBackground from '@/components/calendar/StarryBackground.vue'
import InlineToolbar from '@/components/calendar/InlineToolbar.vue'
import InlineNote from '@/components/calendar/InlineNote.vue'
import EventForm from '@/components/calendar/EventForm.vue'
import EventDetail from '@/components/calendar/EventDetail.vue'
import ConflictPanel from '@/components/calendar/ConflictPanel.vue'
import FestivalCard from '@/components/calendar/FestivalCard.vue'
import FestivalDetail from '@/components/calendar/FestivalDetail.vue'
import VoiceButton from '@/components/voice/VoiceButton.vue'
import VoiceOverlay from '@/components/voice/VoiceOverlay.vue'
import VoiceFunctionPanel from '@/components/voice/VoiceFunctionPanel.vue'
import QueryResultPanel from '@/components/voice/QueryResultPanel.vue'
import { useCalendar } from '@/composables/useCalendar'
import { useVoice } from '@/composables/useVoice'
import { useEvents } from '@/composables/useEvents'
import { useAuth } from '@/composables/useAuth'
import { usePreferences } from '@/composables/usePreferences'
import { useFestival } from '@/composables/useFestival'
import { useTts } from '@/composables/useTts'
import request from '@/utils/request'
import LoginPage from '@/components/auth/LoginPage.vue'
import SettingsPanel from '@/components/settings/SettingsPanel.vue'
import ProfilePanel from '@/components/profile/ProfilePanel.vue'
import type { CalendarEvent } from '@/types/event'

const {
  monthYearLabel,
  yearLabel,
  todayLabel,
  isCurrentMonth,
  isTransitioning,
  calendarCells,
  selectedDate,
  nextMonth,
  prevMonth,
  goToToday,
  selectDate
} = useCalendar()

  const {
    status,
    amplitude,
    isOverlayOpen,
    transcript,
    partialText,
    errorMessage,
    parsedIntent,
    openOverlay,
    closeOverlay,
    startRecording,
    stopRecording,
    clearPendingContext
  } = useVoice()

const {
  datesWithEvents,
  getEventsForDate,
  fetchEvents,
  createEvent,
  updateEvent,
  deleteEvent,
  checkConflicts,
  startAutoRefresh,
  stopAutoRefresh
} = useEvents()

const { isAuthenticated } = useAuth()
const { fetchPreferences } = usePreferences()
const { getFestivalForDate } = useFestival()
const { speak } = useTts()
const settingsOpen = ref(false)
const profileOpen = ref(false)

const weekdays = ['日', '一', '二', '三', '四', '五', '六']
const todayWeekday = new Date().getDay()

const toolbarActiveId = ref('month')
const noteDateKey = shallowRef('')
const noteText = ref('')
const noteOpen = ref(false)
const notes = ref<Record<string, string>>({})

const eventFormOpen = ref(false)
const eventDetailOpen = ref(false)
const editingEvent = ref<CalendarEvent | null>(null)
const formInitialDate = ref('')
const formInitialTitle = ref('')
const formInitialStartTime = ref('')
const formInitialEndTime = ref('')
const formInitialLocation = ref('')
const formInitialCategory = ref('personal')
const formInitialAllDay = ref(false)
const selectedEvent = ref<CalendarEvent | null>(null)
const conflicts = ref<CalendarEvent[]>([])

// 查询结果弹窗状态
const queryResultOpen = ref(false)
const queryEvents = ref<CalendarEvent[]>([])
const queryDate = ref('')
const queryResponseText = ref('')

// 节日详情弹窗状态
const festivalDetailOpen = ref(false)
const selectedFestival = ref<any>(null)

// 功能面板状态
const functionPanelMode = ref('')
const functionPanelTranscript = ref('')
const functionPanelEntities = ref<Record<string, any> | null>(null)

onMounted(() => {
  fetchEvents()
  fetchPreferences()
  startAutoRefresh() // 启动自动刷新
})

onUnmounted(() => {
  stopAutoRefresh() // 停止自动刷新
})

watch(isAuthenticated, (auth) => {
  if (auth) {
    fetchEvents()
    fetchPreferences()
  }
})

function onToolbarSelect(id: string) {
  toolbarActiveId.value = id
  if (id === 'settings') {
    settingsOpen.value = !settingsOpen.value
  } else if (id === 'profile') {
    profileOpen.value = !profileOpen.value
  } else if (id === 'new') {
    openNewEvent(selectedDate.value)
  }
}

function onCellDblclick(dateKey: string) {
  noteDateKey.value = dateKey
  noteText.value = notes.value[dateKey] ?? ''
  noteOpen.value = true
}

function onNoteSave(dateKey: string, text: string) {
  if (text.trim()) {
    notes.value[dateKey] = text.trim()
  } else {
    delete notes.value[dateKey]
  }
}

function formatDateLabel(dateKey: string) {
  const [y, m, d] = dateKey.split('-')
  return `${y}年${parseInt(m)}月${parseInt(d)}日`
}

function openNewEvent(dateKey: string) {
  editingEvent.value = null
  formInitialDate.value = dateKey
  eventFormOpen.value = true
}

function openEditEvent(ev: CalendarEvent) {
  editingEvent.value = ev
  formInitialDate.value = ''
  eventFormOpen.value = true
}

async function handleSaveEvent(data: { title: string; description: string; date: string; startTime: string; endTime: string; allDay: boolean; location: string; color: string; category: string }) {
  console.log('handleSaveEvent 收到数据:', data)

  const startTime = data.allDay
    ? `${data.date} 00:00:00`
    : `${data.date} ${data.startTime}:00`
  const endTime = data.allDay
    ? `${data.date} 23:59:59`
    : `${data.date} ${data.endTime}:00`

  console.log('构建的时间:', { startTime, endTime })

  const eventData = {
    title: data.title,
    description: data.description,
    startTime,
    endTime,
    allDay: data.allDay,
    location: data.location,
    color: data.color,
    category: data.category
  }
  console.log('发送创建请求:', eventData)

  const result = await createEvent(eventData)
  console.log('创建结果:', result)

  if (result) {
    eventFormOpen.value = false
    await fetchEvents()
  }
}

function handleDeleteEvent(id: number) {
  deleteEvent(id)
  eventDetailOpen.value = false
  eventFormOpen.value = false
}

function handleConflictResolve() {
  conflicts.value = []
  eventFormOpen.value = true
}

function handleConflictIgnore() {
  conflicts.value = []
  handleSaveEventFinal()
}

let pendingSaveData: any = null

function handleSaveEventFinal() {
  if (pendingSaveData) {
    handleSaveEvent(pendingSaveData)
    pendingSaveData = null
  }
}

function onSelectEvent(ev: CalendarEvent) {
  selectedEvent.value = ev
  eventDetailOpen.value = true
}

const weekdayLabels = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']

function getWeekdayLabel(dateKey: string) {
  const [y, m, d] = dateKey.split('-').map(Number)
  return weekdayLabels[new Date(y, m - 1, d).getDay()]
}

function formatDateFull(dateKey: string) {
  const [y, m, d] = dateKey.split('-').map(Number)
  return `${y}年${m}月${d}日`
}

function formatEventTime(ev: CalendarEvent) {
  if (ev.allDay) return '全天'
  const s = new Date(ev.startTime)
  const e = new Date(ev.endTime)
  return `${s.getHours().toString().padStart(2, '0')}:${s.getMinutes().toString().padStart(2, '0')} - ${e.getHours().toString().padStart(2, '0')}:${e.getMinutes().toString().padStart(2, '0')}`
}

function handleVoiceConfirm() {
  const intent = parsedIntent.value
  closeOverlay()

  if (!intent) return

  switch (intent.action) {
    case 'query':
      // 查询意图：展示查询结果弹窗
      queryEvents.value = intent.events || []
      queryDate.value = intent.queryDate || ''
      queryResponseText.value = intent.responseText || ''
      queryResultOpen.value = true
      if (intent.responseText) {
        speak(intent.responseText)
      }
      break

    case 'created':
      // 创建成功：显示成功提示、TTS 播报、刷新日历
      if (intent.responseText) {
        speak(intent.responseText)
      }
      // 刷新日历事件列表
      fetchEvents()
      clearPendingContext()
      break

    case 'conflict':
      // 时间冲突：显示冲突提示，让用户确认
      if (intent.responseText) {
        speak(intent.responseText)
      }
      // 可以打开冲突面板或直接提示
      break

    case 'create':
      if (intent.title) {
        editingEvent.value = null
        formInitialDate.value = selectedDate.value
        formInitialTitle.value = intent.title
        eventFormOpen.value = true
        speak(`好的，已为你创建日程：${intent.title}`)
      }
      break

    case 'clarify':
      // 澄清意图：重新打开语音面板让用户补充
      if (intent.clarifyQuestion) {
        speak(intent.clarifyQuestion)
        // 延迟重新打开语音面板
        setTimeout(() => openOverlay(), 1500)
      }
      break

    case 'unknown':
      // 未知意图：播报提示
      if (intent.responseText) {
        speak(intent.responseText)
      }
      break

    default:
      // update/delete/reminder 等暂未实现的意图
      if (intent.responseText) {
        speak(intent.responseText)
      }
      break
  }
}

function handleOpenForm() {
  const intent = parsedIntent.value
  closeOverlay()

  if (!intent || intent.action !== 'preview') return

  // 从预览数据中获取事件信息
  const eventData = intent.event as Record<string, any> || {}

  // 预填充表单字段
  editingEvent.value = null
  formInitialDate.value = (eventData.date as string) || selectedDate.value
  formInitialTitle.value = (eventData.title as string) || intent.title || ''
  formInitialStartTime.value = (eventData.startTime as string) || ''
  formInitialEndTime.value = (eventData.endTime as string) || ''
  formInitialLocation.value = (eventData.location as string) || ''
  formInitialCategory.value = (eventData.category as string) || 'personal'
  formInitialAllDay.value = (eventData.allDay as boolean) || false

  // 打开表单
  eventFormOpen.value = true

  // TTS 播报
  speak('已为您准备好事件信息，请确认后创建')
}

// 功能面板处理函数
function handleFunctionPanelSelectMode(mode: string) {
  functionPanelMode.value = mode
  functionPanelTranscript.value = ''
  functionPanelEntities.value = null
}

async function handleFunctionPanelExecute() {
  if (!functionPanelMode.value || !functionPanelEntities.value) return

  const mode = functionPanelMode.value
  const entities = functionPanelEntities.value

  if (mode === 'CREATE') {
    // 创建模式：直接调用创建接口
    try {
      const eventDate = entities.date || selectedDate.value
      const eventData: any = {
        title: entities.title || '新事件',
        startTime: `${eventDate} ${entities.startTime || '09:00'}:00`,
        endTime: `${eventDate} ${entities.endTime || '10:00'}:00`
      }
      // 可选字段
      if (entities.location) eventData.location = entities.location
      if (entities.category) eventData.category = entities.category
      if (entities.allDay !== undefined) eventData.allDay = entities.allDay
      if (entities.description) eventData.description = entities.description
      if (entities.color) eventData.color = entities.color
      if (entities.priority !== undefined) eventData.priority = entities.priority

      console.log('创建事件:', eventData)
      const result: any = await request.post('/events', eventData)
      console.log('创建结果:', result)
      if (result.code === 201) {
        fetchEvents()
        speak(`已创建事件「${eventData.title}」`)
      } else {
        speak('创建失败，请重试')
      }
    } catch (error) {
      console.error('创建失败:', error)
      speak('创建失败，请重试')
    }
  } else if (mode === 'QUERY') {
    // 查询模式：直接调用查询接口
    try {
      const queryDate = entities.date || new Date().toISOString().slice(0, 10)
      const result: any = await request.get('/events', {
        params: {
          startDate: queryDate,
          endDate: queryDate
        }
      })
      if (result.code === 200) {
        const events = result.data?.records || []
        queryEvents.value = events
        queryDate.value = queryDate
        queryResponseText.value = `找到 ${events.length} 个事件`
        queryResultOpen.value = true
        speak(`找到 ${events.length} 个事件`)
      }
    } catch (error) {
      console.error('查询失败:', error)
      speak('查询失败，请重试')
    }
  } else if (mode === 'UPDATE') {
    // 修改模式：直接调用更新接口
    try {
      // 1. 先查询事件
      const queryDate = entities.date || new Date().toISOString().slice(0, 10)
      const queryResult: any = await request.get('/events', {
        params: {
          startDate: queryDate,
          endDate: queryDate
        }
      })

      if (queryResult.code === 200) {
        const events = queryResult.data?.records || []
        queryEvents.value = events
        queryDate.value = queryDate
        queryResponseText.value = `找到 ${events.length} 个事件`
        queryResultOpen.value = true
        speak(`找到 ${events.length} 个事件`)
      }
    } catch (error) {
      console.error('查询失败:', error)
      speak('查询失败，请重试')
    }
  }

  // 清除状态
  functionPanelEntities.value = null
  functionPanelTranscript.value = ''
}

// 监听语音识别结果 - 当语音识别完成时自动提取实体
watch(parsedIntent, (intent) => {
  if (!intent || !functionPanelMode.value) return

  // 语音识别完成后，调用 LLM 提取实体
  if (intent.action === 'preview' || intent.action === 'query' || intent.action === 'unknown') {
    const text = transcript.value
    if (text) {
      functionPanelTranscript.value = text
      extractEntitiesFromText(functionPanelMode.value, text)
    }
  }
})

async function extractEntitiesFromText(intent: string, text: string) {
  try {
    const result: any = await request.post('/voice/test/extract', { intent, text })
    if (result.code === 200) {
      const data = result.data
      const entities = data.entities || data
      console.log('提取的实体:', entities)
      functionPanelEntities.value = entities
    }
  } catch (error) {
    console.error('实体提取失败:', error)
  }
}

function handleFestivalClick() {
  const festival = getFestivalForDate(selectedDate.value)
  if (festival) {
    selectedFestival.value = festival
    festivalDetailOpen.value = true
  }
}
</script>

<template>
  <LoginPage v-if="!isAuthenticated" />

  <div v-else class="app-shell">
    <StarryBackground />

    <main class="calendar-body">
      <div class="calendar-left">
        <div class="calendar-top-row">
          <CalendarInfoBar />
          <div class="calendar-nav-wrapper">
            <CalendarHeader
              :month-year-label="monthYearLabel"
              :year-label="yearLabel"
              :today-label="todayLabel"
              :is-current-month="isCurrentMonth"
              :is-transitioning="isTransitioning"
              @prev-month="prevMonth"
              @next-month="nextMonth"
              @go-to-today="goToToday"
            />
          </div>
        </div>

        <div class="weekday-row">
          <div
            v-for="(w, i) in weekdays"
            :key="w"
            class="weekday-cell"
            :class="{ 'weekday-cell--today': i === todayWeekday }"
          >
            {{ w }}
          </div>
        </div>

        <CalendarGrid
          :cells="calendarCells"
          :selected-date="selectedDate"
          :dates-with-events="datesWithEvents"
          @select-date="selectDate"
          @cell-dblclick="onCellDblclick"
        />
      </div>

      <aside class="calendar-right">
        <InlineToolbar
          :active-id="toolbarActiveId"
          @select="onToolbarSelect"
        />

        <FestivalCard
          :festival="getFestivalForDate(selectedDate)"
          @click="handleFestivalClick"
        />

        <div class="right-date-section">
          <div class="right-date-header">
            <span class="right-date-day">{{ selectedDate.split('-')[2] }}</span>
            <div class="right-date-info">
              <span class="right-date-weekday">{{ getWeekdayLabel(selectedDate) }}</span>
              <span class="right-date-full">{{ formatDateFull(selectedDate) }}</span>
            </div>
          </div>

          <div class="right-events-list">
            <div v-if="getEventsForDate(selectedDate).length === 0" class="right-events-empty">
              当天暂无日程安排
            </div>
            <div
              v-for="ev in getEventsForDate(selectedDate)"
              :key="ev.id"
              class="right-event-item"
              @click="onSelectEvent(ev)"
            >
              <span class="right-event-dot" :style="{ background: ev.color || '#6366f1' }" />
              <div class="right-event-info">
                <span class="right-event-title">{{ ev.title }}</span>
                <span class="right-event-time">{{ formatEventTime(ev) }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 功能面板 -->
        <VoiceFunctionPanel
          :transcript="functionPanelTranscript"
          :extracted-entities="functionPanelEntities"
          @select-mode="handleFunctionPanelSelectMode"
          @execute="handleFunctionPanelExecute"
        />

        <div class="voice-center">
          <VoiceButton
            :status="status === 'listening' ? 'recording' : status === 'processing' ? 'processing' : 'idle'"
            :amplitude="amplitude"
            :size="80"
            @click="openOverlay"
          />
        </div>
      </aside>
    </main>

    <ConflictPanel
      :conflicts="conflicts"
      @resolve="handleConflictResolve"
      @ignore="handleConflictIgnore"
    />

    <SettingsPanel
      :open="settingsOpen"
      @close="settingsOpen = false"
    />

    <ProfilePanel
      :open="profileOpen"
      @close="profileOpen = false"
    />

    <VoiceOverlay
      :open="isOverlayOpen"
      :status="status"
      :amplitude="amplitude"
      :transcript="transcript"
      :partial-text="partialText"
      :error-message="errorMessage"
      :intent="parsedIntent"
      @close="closeOverlay"
      @confirm-create="handleVoiceConfirm"
      @start-record="startRecording"
      @stop-record="stopRecording"
      @retry="startRecording"
      @edit-result="closeOverlay"
      @open-form="handleOpenForm"
    />

    <QueryResultPanel
      :open="queryResultOpen"
      :date="queryDate"
      :events="queryEvents"
      :response-text="queryResponseText"
      @close="queryResultOpen = false"
    />

    <FestivalDetail
      :open="festivalDetailOpen"
      :festival="selectedFestival"
      @close="festivalDetailOpen = false"
    />

    <EventForm
      :open="eventFormOpen"
      :event="editingEvent"
      :initial-date="formInitialDate"
      :initial-title="formInitialTitle"
      :initial-start-time="formInitialStartTime"
      :initial-end-time="formInitialEndTime"
      :initial-location="formInitialLocation"
      :initial-category="formInitialCategory"
      :initial-all-day="formInitialAllDay"
      @close="eventFormOpen = false"
      @save="handleSaveEvent"
      @delete="handleDeleteEvent"
    />

    <EventDetail
      :open="eventDetailOpen"
      :event="selectedEvent"
      @close="eventDetailOpen = false"
      @edit="openEditEvent"
      @delete="handleDeleteEvent"
    />

    <InlineNote
      :open="noteOpen"
      :date-key="noteDateKey"
      :date-label="formatDateLabel(noteDateKey)"
      :model-value="noteText"
      @update:model-value="noteText = $event"
      @save="onNoteSave"
      @close="noteOpen = false"
    />
  </div>
</template>

<style scoped lang="scss">
@use '@/styles/variables' as *;

.app-shell {
  position: relative;
  width: 100%;
  height: 100vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.calendar-body {
  flex: 1;
  display: flex;
  flex-direction: row;
  gap: $space-3;
  padding: $space-6 $space-4 $space-3;
  position: relative;
  z-index: 1;
  overflow: hidden;
  max-height: calc(100vh - 40px);
}

.calendar-left {
  flex: 5;
  display: flex;
  flex-direction: column;
  min-width: 0;
  min-height: 0;
}

.calendar-top-row {
  display: flex;
  align-items: center;
  gap: $space-3;
  margin-bottom: $space-2;
}

.calendar-top-row > :first-child {
  flex: 1;
  min-width: 0;
}

.calendar-nav-wrapper {
  flex-shrink: 0;
}

.calendar-right {
  flex: 5;
  display: flex;
  flex-direction: column;
  gap: $space-3;
  overflow-y: auto;
  padding-left: $space-4;
  border-left: 1px solid rgba(255, 255, 255, 0.06);
  min-width: 0;
}

.weekday-row {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  padding: $space-1;
  margin-bottom: $space-1;
  background: rgba(255, 255, 255, 0.03);
  border-radius: $radius-md;
  border: 1px solid rgba(255, 255, 255, 0.04);
}

.weekday-cell {
  text-align: center;
  font-size: $font-size-sm;
  font-weight: $font-weight-medium;
  color: $color-text-tertiary;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  padding: $space-2 0;
  border-radius: $radius-sm;
  transition: background $transition-fast, color $transition-fast;

  &--today {
    color: $color-primary-light;
    background: rgba($color-primary, 0.15);
    font-weight: $font-weight-semibold;
  }
}

.voice-center {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: $space-4 0 $space-2;
  margin-top: auto;
}

.right-date-section {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
  padding: $space-3 $space-2;
}

.right-date-header {
  display: flex;
  align-items: center;
  gap: $space-3;
  padding-bottom: $space-3;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
  margin-bottom: $space-3;
}

.right-date-day {
  font-size: 36px;
  font-weight: $font-weight-light;
  color: white;
  line-height: 1;
  font-family: $font-family-display;
}

.right-date-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.right-date-weekday {
  font-size: $font-size-base;
  font-weight: $font-weight-medium;
  color: $color-text-primary;
}

.right-date-full {
  font-size: $font-size-xs;
  color: $color-text-tertiary;
}

.right-events-list {
  display: flex;
  flex-direction: column;
  gap: $space-2;
}

.right-events-empty {
  font-size: $font-size-base;
  color: $color-text-tertiary;
  text-align: center;
  padding: $space-6 0;
}

.right-event-item {
  display: flex;
  align-items: center;
  gap: $space-3;
  padding: $space-3;
  border-radius: $radius-md;
  cursor: pointer;
  transition: background $transition-fast;

  &:hover {
    background: rgba(255, 255, 255, 0.05);
  }
}

.right-event-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.right-event-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.right-event-title {
  font-size: $font-size-base;
  font-weight: $font-weight-medium;
  color: $color-text-primary;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.right-event-time {
  font-size: $font-size-sm;
  color: $color-text-tertiary;
}

</style>
