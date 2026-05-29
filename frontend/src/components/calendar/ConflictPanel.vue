<script setup lang="ts">
import { motion, AnimatePresence } from 'motion-v'
import type { CalendarEvent } from '@/types/event'

defineProps<{
  conflicts: CalendarEvent[]
}>()

const emit = defineEmits<{
  resolve: []
  ignore: []
}>()

function formatTime(iso: string) {
  const d = new Date(iso)
  return `${d.getHours().toString().padStart(2, '0')}:${d.getMinutes().toString().padStart(2, '0')}`
}
</script>

<template>
  <AnimatePresence>
    <motion.div
      v-if="conflicts.length > 0"
      class="conflict-panel"
      :initial="{ opacity: 0, height: 0 }"
      :animate="{ opacity: 1, height: 'auto' }"
      :exit="{ opacity: 0, height: 0 }"
      :transition="{ duration: 0.25 }"
    >
      <div class="conflict-panel__inner">
        <div class="conflict-panel__icon">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
            <circle cx="12" cy="12" r="10" /><line x1="12" y1="8" x2="12" y2="12" /><line x1="12" y1="16" x2="12.01" y2="16" />
          </svg>
        </div>

        <div class="conflict-panel__content">
          <span class="conflict-panel__title">检测到时间冲突</span>
          <div class="conflict-panel__items">
            <div v-for="c in conflicts" :key="c.id" class="conflict-panel__item">
              <span class="conflict-panel__dot" :style="{ background: c.color || '#f59e0b' }" />
              <span class="conflict-panel__name">{{ c.title }}</span>
              <span class="conflict-panel__time">{{ formatTime(c.startTime) }} — {{ formatTime(c.endTime) }}</span>
            </div>
          </div>
        </div>

        <div class="conflict-panel__actions">
          <button class="conflict-panel__btn conflict-panel__btn--ignore" @click="emit('ignore')">
            忽略
          </button>
          <button class="conflict-panel__btn conflict-panel__btn--resolve" @click="emit('resolve')">
            调整
          </button>
        </div>
      </div>
    </motion.div>
  </AnimatePresence>
</template>

<style scoped lang="scss">
@use '@/styles/variables' as *;

.conflict-panel {
  overflow: hidden;
  width: 100%;
}

.conflict-panel__inner {
  display: flex;
  align-items: flex-start;
  gap: $space-3;
  padding: $space-3 $space-4;
  margin: 0 $space-6;
  border-radius: $radius-md;
  background: rgba($color-warning, 0.08);
  border: 1px solid rgba($color-warning, 0.15);
}

.conflict-panel__icon {
  flex-shrink: 0;
  color: $color-warning;
  margin-top: 2px;
}

.conflict-panel__content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: $space-2;
}

.conflict-panel__title {
  font-size: $font-size-sm;
  font-weight: $font-weight-semibold;
  color: $color-warning;
}

.conflict-panel__items {
  display: flex;
  flex-direction: column;
  gap: $space-1;
}

.conflict-panel__item {
  display: flex;
  align-items: center;
  gap: $space-2;
  font-size: $font-size-xs;
  color: $color-text-secondary;
}

.conflict-panel__dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  flex-shrink: 0;
}

.conflict-panel__name {
  font-weight: $font-weight-medium;
}

.conflict-panel__time {
  color: $color-text-tertiary;
  margin-left: auto;
}

.conflict-panel__actions {
  display: flex;
  flex-direction: column;
  gap: $space-1;
  flex-shrink: 0;
}

.conflict-panel__btn {
  padding: $space-1 $space-3;
  border-radius: $radius-full;
  font-size: $font-size-xs;
  font-family: $font-family;
  font-weight: $font-weight-medium;
  cursor: pointer;
  border: 1px solid rgba(255, 255, 255, 0.1);
  transition: background $transition-fast;

  &--ignore {
    background: transparent;
    color: $color-text-tertiary;

    &:hover {
      background: rgba(255, 255, 255, 0.06);
    }
  }

  &--resolve {
    background: rgba($color-warning, 0.15);
    color: $color-warning;
    border-color: rgba($color-warning, 0.2);

    &:hover {
      background: rgba($color-warning, 0.25);
    }
  }
}
</style>
