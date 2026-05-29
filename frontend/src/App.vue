<script setup lang="ts">
import CalendarHeader from '@/components/calendar/CalendarHeader.vue'
import CalendarGrid from '@/components/calendar/CalendarGrid.vue'
import VoiceButton from '@/components/voice/VoiceButton.vue'
import VoiceOverlay from '@/components/voice/VoiceOverlay.vue'
import { useCalendar } from '@/composables/useCalendar'
import { useVoice } from '@/composables/useVoice'

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
  openOverlay,
  closeOverlay
} = useVoice()

const weekdays = ['日', '一', '二', '三', '四', '五', '六']
</script>

<template>
  <div class="app-shell">
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
        @select-date="selectDate"
      />
    </main>

    <div class="voice-center">
      <VoiceButton
        :status="status === 'listening' ? 'recording' : status === 'processing' ? 'processing' : 'idle'"
        :amplitude="amplitude"
        :size="80"
        @click="openOverlay"
      />
    </div>

    <VoiceOverlay
      :open="isOverlayOpen"
      :status="status"
      :amplitude="amplitude"
      @close="closeOverlay"
      @confirm="closeOverlay"
    />

    <footer class="bottom-toolbar">
      <div class="toolbar-placeholder">
        <span>底部工具栏将在 Step 6 构建</span>
      </div>
    </footer>
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

.bottom-toolbar {
  height: $calendar-bottom-toolbar-height;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  z-index: 1;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
}

.toolbar-placeholder {
  font-size: $font-size-xs;
  color: $color-text-tertiary;
  opacity: 0.4;
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
