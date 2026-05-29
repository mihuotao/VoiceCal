<script setup lang="ts">
import { motion } from 'motion-v'
import { springPresets } from '@/composables/useMotion'
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
    <div class="calendar-cell__top">
      <span v-if="cell.isToday" class="calendar-cell__today-circle">
        <span class="calendar-cell__day">{{ cell.day }}</span>
      </span>
      <span v-else class="calendar-cell__day">{{ cell.day }}</span>
      <span v-if="cell.jieQi" class="calendar-cell__jieqi">{{ cell.jieQi }}</span>
    </div>

    <div class="calendar-cell__bottom">
      <span
        v-if="cell.lunarDay && cell.isCurrentMonth"
        class="calendar-cell__lunar"
      >{{ cell.lunarDay }}</span>

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
  padding: $space-1 $space-2;
  min-height: $calendar-cell-min-height;
  border-radius: $radius-md;
  border: 1px solid rgba(255, 255, 255, 0.06);
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
    opacity: 0.45;
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

    &::before {
      content: '';
      position: absolute;
      inset: -2px;
      border-radius: $radius-md;
      background: conic-gradient(
        from var(--angle),
        transparent 0%,
        rgba($color-primary, 0.1) 10%,
        $color-primary-light 20%,
        rgba($color-primary, 0.6) 30%,
        transparent 40%
      );
      z-index: -1;
      animation: border-flow 3s linear infinite;
    }

    &::after {
      content: '';
      position: absolute;
      inset: 0;
      border-radius: calc(#{$radius-md} - 1px);
      background: rgba(15, 12, 41, 0.92);
      z-index: -1;
    }

    &:hover {
      &::after {
        background: rgba(15, 12, 41, 0.85);
      }
    }
  }

  &__top {
    display: flex;
    align-items: center;
    gap: $space-1;
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

  &__jieqi {
    font-size: $calendar-cell-jieqi-size;
    color: $color-warning;
    font-weight: $font-weight-medium;
    line-height: 1;
  }

  &__bottom {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: $space-1;
  }

  &__lunar {
    font-size: $calendar-cell-lunar-size;
    color: $color-text-tertiary;
    line-height: 1;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    max-width: 100%;
  }

  &__dots {
    display: flex;
    align-items: center;
    gap: 2px;

    .dot {
      width: 4px;
      height: 4px;
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
</style>
