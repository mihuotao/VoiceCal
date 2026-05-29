<script setup lang="ts">
import { motion } from 'motion-v'
import { springPresets } from '@/composables/useMotion'
import type { CalendarEvent } from '@/types/event'

defineProps<{
  events: CalendarEvent[]
  selectedDate: string
}>()

const emit = defineEmits<{
  select: [event: CalendarEvent]
  add: [date: string]
}>()

function formatTime(iso: string) {
  const d = new Date(iso)
  return `${d.getHours().toString().padStart(2, '0')}:${d.getMinutes().toString().padStart(2, '0')}`
}

function formatDateLabel(dateKey: string) {
  const [y, m, d] = dateKey.split('-')
  const weekdays = ['日', '一', '二', '三', '四', '五', '六']
  const date = new Date(parseInt(y), parseInt(m) - 1, parseInt(d))
  return `${parseInt(m)}月${parseInt(d)}日 周${weekdays[date.getDay()]}`
}
</script>

<template>
  <div class="event-list">
    <div class="event-list__header">
      <h3 class="event-list__title">{{ formatDateLabel(selectedDate) }}</h3>
      <motion.button
        class="event-list__add"
        :while-hover="{ scale: 1.06 }"
        :while-tap="{ scale: 0.94 }"
        :transition="springPresets.snappy"
        @click="emit('add', selectedDate)"
      >
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
          <line x1="12" y1="5" x2="12" y2="19" /><line x1="5" y1="12" x2="19" y2="12" />
        </svg>
        新建
      </motion.button>
    </div>

    <div v-if="events.length === 0" class="event-list__empty">
      <span>当天暂无事件</span>
    </div>

    <div v-else class="event-list__items">
      <motion.div
        v-for="ev in events"
        :key="ev.id"
        class="event-list__item"
        :layout="true"
        :initial="{ opacity: 0, y: 10 }"
        :animate="{ opacity: 1, y: 0 }"
        :transition="springPresets.gentle"
        @click="emit('select', ev)"
      >
        <span class="event-list__dot" :style="{ background: ev.color || '#6366f1' }" />
        <div class="event-list__info">
          <span class="event-list__item-title">{{ ev.title }}</span>
          <span v-if="!ev.allDay" class="event-list__item-time">{{ formatTime(ev.startTime) }} — {{ formatTime(ev.endTime) }}</span>
          <span v-else class="event-list__item-time">全天</span>
        </div>
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" class="event-list__chevron">
          <polyline points="9 18 15 12 9 6" />
        </svg>
      </motion.div>
    </div>
  </div>
</template>

<style scoped lang="scss">
@use '@/styles/variables' as *;

.event-list {
  width: 100%;
  padding: 0;
  flex: 1;
}

.event-list__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: $space-3;
}

.event-list__title {
  font-size: $font-size-sm;
  font-weight: $font-weight-semibold;
  color: $color-text-secondary;
}

.event-list__add {
  display: inline-flex;
  align-items: center;
  gap: $space-1;
  padding: $space-1 $space-3;
  border-radius: $radius-full;
  font-size: $font-size-xs;
  font-family: $font-family;
  font-weight: $font-weight-medium;
  cursor: pointer;
  border: 1px solid rgba(255, 255, 255, 0.08);
  background: rgba(255, 255, 255, 0.06);
  color: $color-text-secondary;
  transition: background $transition-fast;

  &:hover {
    background: rgba(255, 255, 255, 0.12);
    color: $color-text-primary;
  }
}

.event-list__empty {
  text-align: center;
  padding: $space-6 0;
  font-size: $font-size-sm;
  color: $color-text-tertiary;
}

.event-list__items {
  display: flex;
  flex-direction: column;
  gap: $space-1;
}

.event-list__item {
  display: flex;
  align-items: center;
  gap: $space-3;
  padding: $space-2 $space-3;
  border-radius: $radius-md;
  cursor: pointer;
  transition: background $transition-fast;

  &:hover {
    background: rgba(255, 255, 255, 0.04);
  }
}

.event-list__dot {
  width: 4px;
  height: 32px;
  border-radius: 2px;
  flex-shrink: 0;
}

.event-list__info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 1px;
  min-width: 0;
}

.event-list__item-title {
  font-size: $font-size-base;
  font-weight: $font-weight-medium;
  color: $color-text-primary;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.event-list__item-time {
  font-size: $font-size-xs;
  color: $color-text-tertiary;
}

.event-list__chevron {
  flex-shrink: 0;
  opacity: 0.3;
}
</style>
