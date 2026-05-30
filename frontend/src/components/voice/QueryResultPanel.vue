<script setup lang="ts">
import { computed } from 'vue'
import type { CalendarEvent } from '@/types/event'

const props = defineProps<{
  open: boolean
  date: string
  events: CalendarEvent[]
  responseText: string
}>()

const emit = defineEmits<{
  close: []
}>()

const dateLabel = computed(() => {
  if (!props.date) return ''
  const [y, m, d] = props.date.split('-').map(Number)
  const queryDate = new Date(y, m - 1, d)
  const today = new Date()
  const todayStr = `${today.getFullYear()}-${today.getMonth() + 1}-${today.getDate()}`
  const queryStr = `${y}-${m}-${d}`

  if (queryStr === todayStr) return '今天'
  if (queryDate.getTime() - today.getTime() === 86400000) return '明天'
  if (today.getTime() - queryDate.getTime() === 86400000) return '昨天'
  return `${m}月${d}日`
})

const weekdayLabel = computed(() => {
  if (!props.date) return ''
  const [y, m, d] = props.date.split('-').map(Number)
  const labels = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
  return labels[new Date(y, m - 1, d).getDay()]
})

function formatTime(ev: CalendarEvent): string {
  if (ev.allDay) return '全天'
  const start = new Date(ev.startTime)
  const end = new Date(ev.endTime)
  return `${start.getHours().toString().padStart(2, '0')}:${start.getMinutes().toString().padStart(2, '0')} - ${end.getHours().toString().padStart(2, '0')}:${end.getMinutes().toString().padStart(2, '0')}`
}

function getCategoryIcon(category?: string): string {
  switch (category) {
    case 'work': return '💼'
    case 'personal': return '👤'
    case 'meeting': return '🤝'
    case 'reminder': return '🔔'
    default: return '📌'
  }
}
</script>

<template>
  <Teleport to="body">
    <Transition name="query-panel">
      <div v-if="open" class="query-overlay" @click.self="emit('close')">
        <div class="query-panel">
          <div class="query-header">
            <div class="query-header-icon">📅</div>
            <div class="query-header-text">
              <span class="query-date">{{ dateLabel }}</span>
              <span class="query-weekday">{{ weekdayLabel }}</span>
            </div>
            <button class="query-close" @click="emit('close')">✕</button>
          </div>

          <div class="query-body">
            <div v-if="events.length === 0" class="query-empty">
              <div class="query-empty-icon">🎉</div>
              <div class="query-empty-text">没有日程安排</div>
              <div class="query-empty-sub">享受自由的一天吧</div>
            </div>

            <div v-else class="query-events">
              <div class="query-events-count">共 {{ events.length }} 个安排</div>
              <div
                v-for="ev in events"
                :key="ev.id"
                class="query-event-item"
              >
                <div class="query-event-dot" :style="{ background: ev.color || '#6366f1' }" />
                <div class="query-event-icon">{{ getCategoryIcon(ev.category) }}</div>
                <div class="query-event-info">
                  <div class="query-event-title">{{ ev.title }}</div>
                  <div class="query-event-meta">
                    <span class="query-event-time">{{ formatTime(ev) }}</span>
                    <span v-if="ev.location" class="query-event-location">📍 {{ ev.location }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div class="query-footer">
            <button class="query-btn-ok" @click="emit('close')">知道了</button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped lang="scss">
@use '@/styles/variables' as *;

.query-overlay {
  position: fixed;
  inset: 0;
  z-index: $z-modal;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(4px);
}

.query-panel {
  width: 380px;
  max-height: 500px;
  background: linear-gradient(135deg, #1e1e3f, #2a2a5a);
  border-radius: $radius-lg;
  border: 1px solid rgba(255, 255, 255, 0.1);
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.5);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.query-header {
  display: flex;
  align-items: center;
  gap: $space-3;
  padding: $space-4 $space-5;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.query-header-icon {
  font-size: 28px;
}

.query-header-text {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.query-date {
  font-size: $font-size-lg;
  font-weight: $font-weight-bold;
  color: white;
}

.query-weekday {
  font-size: $font-size-sm;
  color: $color-text-tertiary;
}

.query-close {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: none;
  background: rgba(255, 255, 255, 0.08);
  color: $color-text-secondary;
  font-size: 14px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all $transition-fast;

  &:hover {
    background: rgba(255, 255, 255, 0.15);
    color: white;
  }
}

.query-body {
  flex: 1;
  overflow-y: auto;
  padding: $space-4 $space-5;
}

.query-empty {
  text-align: center;
  padding: $space-6 0;
}

.query-empty-icon {
  font-size: 48px;
  margin-bottom: $space-3;
}

.query-empty-text {
  font-size: $font-size-lg;
  font-weight: $font-weight-semibold;
  color: $color-text-primary;
  margin-bottom: $space-1;
}

.query-empty-sub {
  font-size: $font-size-sm;
  color: $color-text-tertiary;
}

.query-events-count {
  font-size: $font-size-sm;
  color: $color-text-tertiary;
  margin-bottom: $space-3;
}

.query-event-item {
  display: flex;
  align-items: center;
  gap: $space-3;
  padding: $space-3;
  border-radius: $radius-md;
  transition: background $transition-fast;

  &:hover {
    background: rgba(255, 255, 255, 0.05);
  }
}

.query-event-dot {
  width: 4px;
  height: 32px;
  border-radius: 2px;
  flex-shrink: 0;
}

.query-event-icon {
  font-size: 20px;
  flex-shrink: 0;
}

.query-event-info {
  flex: 1;
  min-width: 0;
}

.query-event-title {
  font-size: $font-size-base;
  font-weight: $font-weight-medium;
  color: $color-text-primary;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.query-event-meta {
  display: flex;
  align-items: center;
  gap: $space-2;
  margin-top: 2px;
}

.query-event-time {
  font-size: $font-size-sm;
  color: $color-text-tertiary;
}

.query-event-location {
  font-size: $font-size-sm;
  color: $color-text-tertiary;
}

.query-footer {
  padding: $space-4 $space-5;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
  display: flex;
  justify-content: center;
}

.query-btn-ok {
  width: 100%;
  padding: $space-3;
  border-radius: $radius-md;
  border: none;
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  color: white;
  font-size: $font-size-base;
  font-weight: $font-weight-semibold;
  cursor: pointer;
  transition: all $transition-fast;

  &:hover {
    transform: translateY(-1px);
    box-shadow: 0 4px 12px rgba(99, 102, 241, 0.4);
  }

  &:active {
    transform: translateY(0);
  }
}

// 动画
.query-panel-enter-active {
  transition: all 0.3s ease;
}

.query-panel-leave-active {
  transition: all 0.2s ease;
}

.query-panel-enter-from {
  opacity: 0;

  .query-panel {
    transform: translateY(20px) scale(0.95);
  }
}

.query-panel-leave-to {
  opacity: 0;

  .query-panel {
    transform: translateY(10px) scale(0.98);
  }
}
</style>
