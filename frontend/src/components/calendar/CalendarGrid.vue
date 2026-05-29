<script setup lang="ts">
import { computed } from 'vue'
import CalendarCell from '@/components/calendar/CalendarCell.vue'
import type { CalendarCellData } from '@/types/calendar'

const props = defineProps<{
  cells: CalendarCellData[]
  selectedDate: string
}>()

const emit = defineEmits<{
  selectDate: [dateKey: string]
  cellDblclick: [dateKey: string]
}>()

const rows = computed(() => {
  const result: CalendarCellData[][] = []
  for (let i = 0; i < props.cells.length; i += 7) {
    result.push(props.cells.slice(i, i + 7))
  }
  return result
})
</script>

<template>
  <div class="calendar-grid">
    <div
      v-for="(row, ri) in rows"
      :key="ri"
      class="calendar-grid__row"
    >
      <CalendarCell
        v-for="cell in row"
        :key="cell.dateKey"
        :cell="cell"
        :is-selected="cell.dateKey === selectedDate"
        @select="emit('selectDate', $event)"
        @dblclick="emit('cellDblclick', $event)"
      />
    </div>
  </div>
</template>

<style scoped lang="scss">
@use '@/styles/variables' as *;

.calendar-grid {
  display: flex;
  flex-direction: column;
  flex: 1;
  gap: 1px;
  margin: 0;
}

.calendar-grid__row {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 1px;
  flex: 1;
  min-height: 0;
}
</style>
