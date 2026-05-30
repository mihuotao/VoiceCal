<script setup lang="ts">
import { ref, shallowRef, onMounted, watch } from 'vue'
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
import VoiceButton from '@/components/voice/VoiceButton.vue'
import VoiceOverlay from '@/components/voice/VoiceOverlay.vue'
import QueryResultPanel from '@/components/voice/QueryResultPanel.vue'
import { useCalendar } from '@/composables/useCalendar'
import { useVoice } from '@/composables/useVoice'
import { useEvents } from '@/composables/useEvents'
import { useAuth } from '@/composables/useAuth'
import { usePreferences } from '@/composables/usePreferences'
import { useFestival } from '@/composables/useFestival'
import { useTts } from '@/composables/useTts'
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
    stopRecording
  } = useVoice()

const {
  datesWithEvents,
  getEventsForDate,
  fetchEvents,
  createEvent,
  updateEvent,
  deleteEvent,
  checkConflicts
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
const selectedEvent = ref<CalendarEvent | null>(null)
const conflicts = ref<CalendarEvent[]>([])

// 查询结果弹窗状态
const queryResultOpen = ref(false)
const queryEvents = ref<CalendarEvent[]>([])
const queryDate = ref('')
const queryResponseText = ref('')

onMounted(() => {
  fetchEvents()
  fetchPreferences()
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
  const startTime = data.allDay
    ? new Date(data.date + 'T00:00:00').toISOString()
    : new Date(data.date + 'T' + data.startTime + ':00').toISOString()
  const endTime = data.allDay
    ? new Date(data.date + 'T23:59:59').toISOString()
    : new Date(data.date + 'T' + data.endTime + ':00').toISOString()

  const eventConflicts = checkConflicts(startTime, endTime, editingEvent.value?.id)
  conflicts.value = eventConflicts

  if (eventConflicts.length > 0) return

  if (editingEvent.value) {
    await updateEvent(editingEvent.value.id, {
      title: data.title,
      description: data.description,
      startTime,
      endTime,
      allDay: data.allDay,
      location: data.location,
      color: data.color,
      category: data.category
    })
  } else {
    await createEvent({
      title: data.title,
      description: data.description,
      startTime,
      endTime,
      allDay: data.allDay,
      location: data.location,
      color: data.color,
      category: data.category
    })
  }
  eventFormOpen.value = false
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

  if (intent.action === 'query') {
    // 查询意图：展示查询结果弹窗
    queryEvents.value = intent.events || []
    queryDate.value = intent.queryDate || ''
    queryResponseText.value = intent.responseText || ''
    queryResultOpen.value = true
    // TTS 播报
    if (intent.responseText) {
      speak(intent.responseText)
    }
  } else if (intent.title) {
    // 创建意图：打开事件表单
    editingEvent.value = null
    formInitialDate.value = selectedDate.value
    formInitialTitle.value = intent.title
    eventFormOpen.value = true
    speak(`好的，已为你创建日程：${intent.title}`)
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

        <FestivalCard :festival="getFestivalForDate(selectedDate)" />

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
    />

    <QueryResultPanel
      :open="queryResultOpen"
      :date="queryDate"
      :events="queryEvents"
      :response-text="queryResponseText"
      @close="queryResultOpen = false"
    />

    <EventForm
      :open="eventFormOpen"
      :event="editingEvent"
      :initial-date="formInitialDate"
      :initial-title="formInitialTitle"
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
