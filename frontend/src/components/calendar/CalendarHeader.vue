<script setup lang="ts">
import { motion } from 'motion-v'
import { springPresets } from '@/composables/useMotion'
import GlassButton from '@/components/glass/GlassButton.vue'

defineProps<{
  monthYearLabel: string
  yearLabel: string
  todayLabel: string
  isCurrentMonth: boolean
  isTransitioning: boolean
}>()

const emit = defineEmits<{
  prevMonth: []
  nextMonth: []
  goToToday: []
}>()
</script>

<template>
  <header class="calendar-header">
    <div class="calendar-header__left">
      <GlassButton
        variant="ghost"
        size="sm"
        rounded
        :disabled="isTransitioning"
        @click="emit('prevMonth')"
      >
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
          <polyline points="15 18 9 12 15 6" />
        </svg>
      </GlassButton>

      <div class="calendar-header__month-wrapper">
        <span class="calendar-header__month" :class="{ 'is-transitioning': isTransitioning }">
          {{ monthYearLabel }}
        </span>
        <span class="calendar-header__year">{{ yearLabel }}</span>
      </div>

      <GlassButton
        variant="ghost"
        size="sm"
        rounded
        :disabled="isTransitioning"
        @click="emit('nextMonth')"
      >
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
          <polyline points="9 18 15 12 9 6" />
        </svg>
      </GlassButton>
    </div>

    <div class="calendar-header__right">
      <motion.button
        v-if="!isCurrentMonth"
        class="calendar-header__today"
        :while-hover="{ scale: 1.05 }"
        :while-tap="{ scale: 0.95 }"
        :transition="springPresets.snappy"
        @click="emit('goToToday')"
      >
        {{ todayLabel }}
      </motion.button>
    </div>
  </header>
</template>

<style scoped lang="scss">
@use '@/styles/variables' as *;

.calendar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: $space-4 $space-6;
  height: $calendar-toolbar-height;

  &__left {
    display: flex;
    align-items: center;
    gap: $space-3;
  }

  &__month-wrapper {
    display: flex;
    align-items: baseline;
    gap: $space-2;
  }

  &__month {
    font-size: $font-size-2xl;
    font-weight: $font-weight-semibold;
    color: $color-text-primary;
    letter-spacing: 0.02em;
    view-transition-name: month-label;

    &.is-transitioning {
      animation: month-fade-pulse 0.35s ease;
    }
  }

  &__year {
    font-size: $font-size-base;
    color: $color-text-tertiary;
    font-weight: $font-weight-normal;
  }

  &__right {
    display: flex;
    align-items: center;
  }

  &__today {
    display: inline-flex;
    align-items: center;
    gap: $space-1;
    padding: $space-1 $space-3;
    font-size: $font-size-sm;
    color: $color-text-secondary;
    background: rgba(255, 255, 255, 0.06);
    border: 1px solid rgba(255, 255, 255, 0.08);
    border-radius: $radius-full;
    cursor: pointer;
    font-family: $font-family;
    transition: background $transition-base, color $transition-base;

    &:hover {
      background: rgba(255, 255, 255, 0.12);
      color: $color-text-primary;
    }
  }
}

@keyframes month-fade-pulse {
  0% { opacity: 1; transform: scale(1); }
  40% { opacity: 0.6; transform: scale(0.97); }
  100% { opacity: 1; transform: scale(1); }
}
</style>
