<script setup lang="ts">
import { ref, shallowRef, onMounted } from 'vue'
import CalendarHeader from '@/components/calendar/CalendarHeader.vue'
import CalendarGrid from '@/components/calendar/CalendarGrid.vue'
import BottomToolbar from '@/components/calendar/BottomToolbar.vue'
import InlineNote from '@/components/calendar/InlineNote.vue'
import EventList from '@/components/calendar/EventList.vue'
import EventForm from '@/components/calendar/EventForm.vue'
import EventDetail from '@/components/calendar/EventDetail.vue'
import ConflictPanel from '@/components/calendar/ConflictPanel.vue'
import FestivalCard from '@/components/calendar/FestivalCard.vue'
import SearchPanel from '@/components/search/SearchPanel.vue'
import VoiceButton from '@/components/voice/VoiceButton.vue'
import VoiceOverlay from '@/components/voice/VoiceOverlay.vue'
import { useCalendar } from '@/composables/useCalendar'
import { useVoice } from '@/composables/useVoice'
import { useEvents } from '@/composables/useEvents'
import { useSearch } from '@/composables/useSearch'
import { useAuth } from '@/composables/useAuth'
import { usePreferences } from '@/composables/usePreferences'
import { useFestival } from '@/composables/useFestival'
import { useTts } from '@/composables/useTts'
import LoginPage from '@/components/auth/LoginPage.vue'
import SettingsPanel from '@/components/settings/SettingsPanel.vue'
import type { CalendarEvent } from '@/types/event'
import type { SearchResultItem } from '@/types/search'

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

const s = useSearch()
const searchResults = s.results
const searchSummary = s.summary
const searchQueryText = s.searchQuery
const isSearching = s.isSearching
const search = s.search
const clearSearch = s.clearSearch

const { isAuthenticated, logout } = useAuth()
const { fetchPreferences } = usePreferences()
const { getFestivalForDate } = useFestival()
const { speak } = useTts()
const settingsOpen = ref(false)

const weekdays = ['日', '一', '二', '三', '四', '五', '六']

const toolbarActiveId = ref('month')
const searchOpen = ref(false)
const noteDateKey = shallowRef('')
const noteText = ref('')
const noteOpen = ref(false)
const notes = ref<Record<string, string>>({})

const eventFormOpen = ref(false)
const eventDetailOpen = ref(false)
const editingEvent = ref<CalendarEvent | null>(null)
const formInitialDate = ref('')
const selectedEvent = ref<CalendarEvent | null>(null)
const conflicts = ref<CalendarEvent[]>([])

onMounted(() => {
  fetchEvents()
  fetchPreferences()
})

function onToolbarSelect(id: string) {
  toolbarActiveId.value = id
  if (id === 'search') {
    searchOpen.value = true
  } else if (id === 'settings') {
    settingsOpen.value = !settingsOpen.value
  } else if (id === 'profile') {
    logout()
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

function handleSearch(query: string) {
  if (!query.trim()) return
  search(query)
}

function handleSearchCreateEvent(_item: SearchResultItem) {
  editingEvent.value = null
  formInitialDate.value = selectedDate.value
  eventFormOpen.value = true
  searchOpen.value = false
}

function handleCloseSearch() {
  searchOpen.value = false
  clearSearch()
}

function handleVoiceConfirm() {
  closeOverlay()
  const intent = parsedIntent.value
  if (intent) {
    speak(`好的，已为你${intent.action === 'search' ? '搜索' : '创建日程'}：${intent.title}`)
  }
}
</script>

<template>
  <LoginPage v-if="!isAuthenticated" />

  <div v-else class="app-shell">
    <div class="bg-orbs">
      <div class="bg-orb bg-orb--1" />
      <div class="bg-orb bg-orb--2" />
      <div class="bg-orb bg-orb--3" />
    </div>

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

    <main class="calendar-body">
      <div class="weekday-row">
        <div v-for="w in weekdays" :key="w" class="weekday-cell">
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

      <FestivalCard :festival="getFestivalForDate(selectedDate)" />

      <EventList
        :events="getEventsForDate(selectedDate)"
        :selected-date="selectedDate"
        @select="onSelectEvent"
        @add="openNewEvent"
      />
    </main>

    <ConflictPanel
      :conflicts="conflicts"
      @resolve="handleConflictResolve"
      @ignore="handleConflictIgnore"
    />

    <div class="voice-center">
      <VoiceButton
        :status="status === 'listening' ? 'recording' : status === 'processing' ? 'processing' : 'idle'"
        :amplitude="amplitude"
        :size="80"
        @click="openOverlay"
      />
    </div>

    <SearchPanel
      :open="searchOpen"
      :query="searchQueryText"
      :results="searchResults"
      :summary="searchSummary"
      :is-searching="isSearching"
      @close="handleCloseSearch"
      @search="handleSearch"
      @create-event="handleSearchCreateEvent"
    />

    <SettingsPanel
      :open="settingsOpen"
      @close="settingsOpen = false"
    />

    <VoiceOverlay
      :open="isOverlayOpen"
      :status="status"
      :amplitude="amplitude"
      :transcript="transcript"
      :partial-text="partialText"
      :intent="parsedIntent"
      @close="closeOverlay"
      @confirm-create="handleVoiceConfirm"
      @start-record="startRecording"
      @stop-record="stopRecording"
      @retry="startRecording"
      @edit-result="closeOverlay"
    />

    <EventForm
      :open="eventFormOpen"
      :event="editingEvent"
      :initial-date="formInitialDate"
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

    <BottomToolbar
      :active-id="toolbarActiveId"
      @select="onToolbarSelect"
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

.bg-orbs {
  position: fixed;
  inset: 0;
  pointer-events: none;
  z-index: 0;
  overflow: hidden;
}

.bg-orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.15;

  &--1 {
    width: 400px;
    height: 400px;
    background: radial-gradient(circle, $color-primary, transparent);
    top: -5%;
    right: -5%;
    animation: orb-float-1 12s ease-in-out infinite;
  }

  &--2 {
    width: 300px;
    height: 300px;
    background: radial-gradient(circle, $color-info, transparent);
    bottom: 10%;
    left: -8%;
    animation: orb-float-2 15s ease-in-out infinite;
  }

  &--3 {
    width: 200px;
    height: 200px;
    background: radial-gradient(circle, $color-primary-light, transparent);
    bottom: 30%;
    right: 20%;
    animation: orb-float-3 10s ease-in-out infinite;
  }
}

.calendar-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 0 $space-6 $space-2;
  position: relative;
  z-index: 1;
  overflow: hidden;
}

.weekday-row {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  padding: $space-2 0;
  margin-bottom: $space-1;
}

.weekday-cell {
  text-align: center;
  font-size: $font-size-xs;
  font-weight: $font-weight-medium;
  color: $color-text-tertiary;
  letter-spacing: 0.05em;
  text-transform: uppercase;
  padding: $space-1 0;
}

.voice-center {
  position: absolute;
  bottom: 72px;
  left: 50%;
  transform: translateX(-50%);
  z-index: $z-overlay;
  display: flex;
  align-items: center;
  justify-content: center;
}

@keyframes orb-float-1 {
  0%, 100% { transform: translate(0, 0) scale(1); }
  33% { transform: translate(30px, -40px) scale(1.1); }
  66% { transform: translate(-20px, 20px) scale(0.95); }
}

@keyframes orb-float-2 {
  0%, 100% { transform: translate(0, 0) scale(1); }
  50% { transform: translate(40px, -30px) scale(1.08); }
}

@keyframes orb-float-3 {
  0%, 100% { transform: translate(0, 0) scale(1); opacity: 0.12; }
  50% { transform: translate(-20px, -50px) scale(1.15); opacity: 0.2; }
}


</style>
