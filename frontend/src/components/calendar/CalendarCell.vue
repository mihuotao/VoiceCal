<script setup lang="ts">
import { computed } from 'vue'
import { motion } from 'motion-v'
import { springPresets } from '@/composables/useMotion'
import { getHolidayInfo } from '@/utils/holiday'
import type { CalendarCellData } from '@/types/calendar'

const props = withDefaults(defineProps<{
  cell: CalendarCellData
  isSelected?: boolean
  hasEvents?: boolean
}>(), {
  isSelected: false,
  hasEvents: false
})

const emit = defineEmits<{
  select: [dateKey: string]
  dblclick: [dateKey: string]
}>()

const holiday = computed(() => {
  if (!props.cell.isCurrentMonth) return null
  return getHolidayInfo(props.cell.month + 1, props.cell.day)
})

const festivalName = computed(() => {
  if (props.cell.festivals?.length > 0) return props.cell.festivals[0]
  if (props.cell.jieQi) return props.cell.jieQi
  if (holiday.value) return holiday.value.name
  return ''
})

const isHoliday = computed(() => holiday.value?.isOff ?? false)

function handleClick() {
  emit('select', props.cell.dateKey)
}

function handleDblClick() {
  emit('dblclick', props.cell.dateKey)
}
</script>

<template>
  <motion.div
    class="calendar-cell"
    :class="{
      'calendar-cell--current': cell.isCurrentMonth,
      'calendar-cell--other': !cell.isCurrentMonth,
      'calendar-cell--today': cell.isToday,
      'calendar-cell--selected': isSelected,
      'calendar-cell--weekend': cell.isWeekend
    }"
    :while-hover="{ scale: 1.04, y: -3 }"
    :while-tap="{ scale: 0.97 }"
    :transition="springPresets.gentle"
    :layout="true"
    @click="handleClick"
    @dblclick.prevent="handleDblClick"
  >
    <span v-if="isHoliday" class="calendar-cell__holiday-badge">休</span>

    <div class="calendar-cell__top">
      <span v-if="cell.isToday" class="calendar-cell__today-circle">
        <span class="calendar-cell__day">{{ cell.day }}</span>
      </span>
      <span v-else class="calendar-cell__day">{{ cell.day }}</span>
    </div>

    <div class="calendar-cell__bottom">
      <span
        v-if="cell.lunarDay && cell.isCurrentMonth"
        class="calendar-cell__lunar"
      >{{ cell.lunarDay }}</span>
      <span
        v-if="festivalName && cell.isCurrentMonth"
        class="calendar-cell__festival"
      >{{ festivalName }}</span>

      <div v-if="hasEvents" class="calendar-cell__dots">
        <span class="dot" />
      </div>
    </div>
  </motion.div>
</template>

<style>
@property --angle {
  syntax: '<angle>';
  initial-value: 0deg;
  inherits: false;
}
</style>

<style scoped lang="scss">
@use '@/styles/variables' as *;

.calendar-cell {
  position: relative;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 3px 5px;
  min-height: $calendar-cell-min-height;
  border-radius: $radius-sm;
  border: 1px solid rgba(255, 255, 255, 0.05);
  backdrop-filter: blur(var(--glass-blur));
  -webkit-backdrop-filter: blur(var(--glass-blur));
  cursor: pointer;
  user-select: none;
  -webkit-tap-highlight-color: transparent;
  transition: background $transition-fast, box-shadow $transition-fast;
  overflow: hidden;

  &:hover {
    background: rgba(255, 255, 255, 0.05);
  }

  &--current {
    color: $color-text-primary;
  }

  &--other {
    color: $color-text-tertiary;
    opacity: 0.4;
  }

  &--today {
    .calendar-cell__day {
      color: white;
      font-weight: $font-weight-bold;
    }
  }

  &--selected {
    background: transparent;
    border-color: transparent;
    box-shadow: none;
    animation: glow-pulse 3s ease-in-out infinite;

    &::before {
      content: '';
      position: absolute;
      inset: -2px;
      border-radius: $radius-sm;
      background: conic-gradient(
        from var(--angle),
        transparent 0%,
        transparent 5%,
        rgba($color-primary-light, 0.15) 10%,
        rgba($color-primary-light, 0.5) 15%,
        rgba(255, 255, 255, 0.95) 18%,
        $color-primary-light 20%,
        rgba($color-primary, 0.4) 25%,
        transparent 35%
      );
      z-index: -1;
      animation: border-flow 3s linear infinite;
    }

    &::after {
      content: '';
      position: absolute;
      inset: -1px;
      border-radius: calc(#{$radius-sm} + 1px);
      background: conic-gradient(
        from var(--angle),
        transparent 0%,
        transparent 14%,
        rgba(255, 255, 255, 0.6) 17%,
        rgba(255, 255, 255, 0.9) 18%,
        rgba(255, 255, 255, 0.6) 19%,
        transparent 22%,
        transparent 100%
      );
      z-index: -2;
      animation: border-flow 3s linear infinite;
    }

    &:hover {
      background: transparent;
    }
  }

  &__holiday-badge {
    position: absolute;
    top: 1px;
    right: 1px;
    width: 16px;
    height: 16px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 9px;
    font-weight: $font-weight-bold;
    color: white;
    background: #ef4444;
    border-radius: 50%;
    z-index: 1;
    line-height: 1;
  }

  &__top {
    display: flex;
    align-items: center;
    gap: 2px;
  }

  &__today-circle {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: $calendar-today-circle-size;
    height: $calendar-today-circle-size;
    border-radius: 50%;
    background: rgba($color-primary, 0.25);
  }

  &__day {
    font-size: $calendar-cell-day-size;
    font-weight: $font-weight-bold;
    line-height: 1;
  }

  &__bottom {
    display: flex;
    align-items: center;
    gap: 2px;
    flex-wrap: wrap;
  }

  &__festival {
    font-size: 11px;
    color: $color-warning;
    font-weight: $font-weight-semibold;
    line-height: 1;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  &__lunar {
    font-size: $calendar-cell-lunar-size;
    color: $color-text-tertiary;
    line-height: 1;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  &__dots {
    display: flex;
    align-items: center;
    gap: 2px;

    .dot {
      width: 3px;
      height: 3px;
      border-radius: 50%;
      background: $color-primary-light;
    }
  }
}

@keyframes border-flow {
  to {
    --angle: 360deg;
  }
}

@keyframes glow-pulse {
  0%, 100% {
    box-shadow: 0 0 8px rgba($color-primary, 0.2), 0 0 16px rgba($color-primary, 0.1);
  }
  50% {
    box-shadow: 0 0 12px rgba($color-primary, 0.4), 0 0 24px rgba($color-primary, 0.2);
  }
}
</style>
