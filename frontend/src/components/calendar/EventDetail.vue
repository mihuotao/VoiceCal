<script setup lang="ts">
import { motion } from 'motion-v'
import { springPresets } from '@/composables/useMotion'
import GlassButton from '@/components/glass/GlassButton.vue'
import type { CalendarEvent } from '@/types/event'

defineProps<{
  open: boolean
  event: CalendarEvent | null
}>()

const emit = defineEmits<{
  close: []
  edit: [event: CalendarEvent]
  delete: [id: number]
}>()

function formatTime(iso: string) {
  const d = new Date(iso)
  return `${d.getMonth() + 1}月${d.getDate()}日 ${d.getHours().toString().padStart(2, '0')}:${d.getMinutes().toString().padStart(2, '0')}`
}

const categoryLabel: Record<string, string> = {
  personal: '个人',
  work: '工作',
  health: '健康',
  social: '社交'
}
</script>

<template>
  <Teleport to="body">
    <motion.div
      v-if="open && event"
      class="event-detail-backdrop"
      :initial="{ opacity: 0 }"
      :animate="{ opacity: 1 }"
      :exit="{ opacity: 0 }"
      :transition="{ duration: 0.15 }"
      @click="emit('close')"
    >
      <motion.div
        class="event-detail"
        :initial="{ opacity: 0, x: 60 }"
        :animate="{ opacity: 1, x: 0 }"
        :exit="{ opacity: 0, x: 60 }"
        :transition="springPresets.slide"
        @click.stop
      >
        <div class="event-detail__bar" :style="{ background: event.color || '#6366f1' }" />

        <div class="event-detail__header">
          <h2 class="event-detail__title">{{ event.title }}</h2>
          <button class="event-detail__close" @click="emit('close')">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
              <line x1="18" y1="6" x2="6" y2="18" /><line x1="6" y1="6" x2="18" y2="18" />
            </svg>
          </button>
        </div>

        <div class="event-detail__body">
          <div class="event-detail__meta">
            <div class="event-detail__meta-item">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round">
                <rect x="3" y="4" width="18" height="18" rx="2" /><line x1="16" y1="2" x2="16" y2="6" /><line x1="8" y1="2" x2="8" y2="6" /><line x1="3" y1="10" x2="21" y2="10" />
              </svg>
              <span v-if="event.allDay">全天</span>
              <span v-else>{{ formatTime(event.startTime) }} — {{ formatTime(event.endTime) }}</span>
            </div>

            <div v-if="event.location" class="event-detail__meta-item">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round">
                <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z" /><circle cx="12" cy="10" r="3" />
              </svg>
              <span>{{ event.location }}</span>
            </div>

            <div v-if="event.category" class="event-detail__meta-item">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round">
                <path d="M4 4h7l9 9-7 7-9-9V4z" />
              </svg>
              <span>{{ categoryLabel[event.category] || event.category }}</span>
            </div>
          </div>

          <p v-if="event.description" class="event-detail__desc">{{ event.description }}</p>
        </div>

        <div class="event-detail__footer">
          <GlassButton variant="ghost" @click="emit('delete', event.id)">删除</GlassButton>
          <GlassButton variant="primary" @click="emit('edit', event)">编辑</GlassButton>
        </div>
      </motion.div>
    </motion.div>
  </Teleport>
</template>

<style scoped lang="scss">
@use '@/styles/variables' as *;

.event-detail-backdrop {
  position: fixed;
  inset: 0;
  z-index: $z-modal;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.3);
  backdrop-filter: blur(4px);
  padding: $space-4;
}

.event-detail {
  width: 100%;
  max-width: 400px;
  background: rgba(20, 18, 40, 0.9);
  backdrop-filter: blur(32px) saturate(1.4);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: $radius-xl;
  overflow: hidden;
  box-shadow: $shadow-lg;

  &__bar {
    height: 4px;
  }

  &__header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: $space-5 $space-6 $space-3;
  }

  &__title {
    font-size: $font-size-xl;
    font-weight: $font-weight-semibold;
  }

  &__close {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 32px;
    height: 32px;
    border-radius: 50%;
    background: rgba(255, 255, 255, 0.06);
    border: 1px solid rgba(255, 255, 255, 0.06);
    color: $color-text-tertiary;
    cursor: pointer;

    &:hover {
      background: rgba(255, 255, 255, 0.12);
      color: $color-text-primary;
    }
  }

  &__body {
    padding: $space-3 $space-6 $space-5;
    display: flex;
    flex-direction: column;
    gap: $space-4;
  }

  &__meta {
    display: flex;
    flex-direction: column;
    gap: $space-2;
  }

  &__meta-item {
    display: flex;
    align-items: center;
    gap: $space-2;
    font-size: $font-size-sm;
    color: $color-text-secondary;

    svg {
      flex-shrink: 0;
      opacity: 0.5;
    }
  }

  &__desc {
    font-size: $font-size-base;
    color: $color-text-secondary;
    line-height: $line-height-relaxed;
    padding: $space-3;
    background: rgba(255, 255, 255, 0.03);
    border-radius: $radius-md;
  }

  &__footer {
    display: flex;
    justify-content: flex-end;
    gap: $space-2;
    padding: $space-4 $space-6;
    border-top: 1px solid rgba(255, 255, 255, 0.06);
  }
}
</style>
