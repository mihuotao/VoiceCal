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
}>()

function handleClick() {
  emit('select', props.cell.dateKey)
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
  >
    <div class="calendar-cell__top">
      <span class="calendar-cell__day">{{ cell.day }}</span>
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

    <div v-if="cell.isToday" class="calendar-cell__indicator" />
  </motion.div>
</template>

<style scoped lang="scss">
@use '@/styles/variables' as *;

.calendar-cell {
  position: relative;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: $space-2 $space-2;
  min-height: $calendar-cell-min-height;
  border-radius: $radius-md;
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
      color: $color-primary-light;
      font-weight: $font-weight-bold;
    }
  }

  &--selected {
    background: rgba($color-primary, 0.12);
    box-shadow: inset 0 0 0 1px rgba($color-primary, 0.25);

    &:hover {
      background: rgba($color-primary, 0.16);
    }
  }

  &__top {
    display: flex;
    align-items: baseline;
    gap: $space-1;
  }

  &__day {
    font-size: $font-size-base;
    font-weight: $font-weight-medium;
    line-height: 1.2;
  }

  &__jieqi {
    font-size: $font-size-xs;
    color: $color-warning;
    font-weight: $font-weight-medium;
    line-height: 1.2;
  }

  &__bottom {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: $space-1;
  }

  &__lunar {
    font-size: 10px;
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

  &__indicator {
    position: absolute;
    top: $space-2;
    right: $space-2;
    width: 5px;
    height: 5px;
    border-radius: 50%;
    background: $color-primary-light;
    box-shadow: 0 0 6px rgba($color-primary-light, 0.5);
  }
}
</style>
