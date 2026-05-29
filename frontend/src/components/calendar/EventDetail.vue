<script setup lang="ts">
import { ref, reactive, watch } from 'vue'
import GlassButton from '@/components/glass/GlassButton.vue'
import type { CalendarEvent } from '@/types/event'

const props = defineProps<{
  open: boolean
  event: CalendarEvent | null
}>()

const emit = defineEmits<{
  close: []
  edit: [event: CalendarEvent]
  delete: [id: number]
}>()

const pos = reactive({ x: 0, y: 0 })
const size = reactive({ w: 400, h: 0 })
const isDragging = ref(false)
const isResizing = ref(false)
const isPositioned = ref(false)
let dragStartX = 0
let dragStartY = 0
let dragStartPosX = 0
let dragStartPosY = 0
let resizeStartX = 0
let resizeStartY = 0
let resizeStartW = 0
let resizeStartH = 0

function centerPanel() {
  const vw = window.innerWidth
  const vh = window.innerHeight
  pos.x = Math.max(0, (vw - size.w) / 2)
  pos.y = Math.max(0, vh * 0.15)
  size.h = Math.min(500, vh * 0.6)
  isPositioned.value = true
}

function onDragStart(e: MouseEvent) {
  if ((e.target as HTMLElement).closest('.event-detail__close')) return
  isDragging.value = true
  dragStartX = e.clientX
  dragStartY = e.clientY
  dragStartPosX = pos.x
  dragStartPosY = pos.y
  e.preventDefault()
}

function onMouseMove(e: MouseEvent) {
  if (isDragging.value) {
    pos.x = dragStartPosX + (e.clientX - dragStartX)
    pos.y = dragStartPosY + (e.clientY - dragStartY)
  }
  if (isResizing.value) {
    size.w = Math.max(300, resizeStartW + (e.clientX - resizeStartX))
    size.h = Math.max(250, resizeStartH + (e.clientY - resizeStartY))
  }
}

function onMouseUp() {
  isDragging.value = false
  isResizing.value = false
}

function onResizeStart(e: MouseEvent) {
  isResizing.value = true
  resizeStartX = e.clientX
  resizeStartY = e.clientY
  resizeStartW = size.w
  resizeStartH = size.h
  e.preventDefault()
  e.stopPropagation()
}

watch(() => props.open, (v) => {
  if (v) {
    isPositioned.value = false
    size.h = 0
    centerPanel()
  }
})

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
    <div
      v-if="open && event"
      class="event-detail-backdrop"
      @mousemove="onMouseMove"
      @mouseup="onMouseUp"
    >
      <div
        class="event-detail"
        :class="{
          'event-detail--dragging': isDragging || isResizing,
          'event-detail--positioned': isPositioned
        }"
        :style="{
          left: pos.x + 'px',
          top: pos.y + 'px',
          width: size.w + 'px',
          maxHeight: size.h > 0 ? size.h + 'px' : '60vh'
        }"
        @click.stop
      >
        <div class="event-detail__bar" :style="{ background: event.color || '#6366f1' }" />

        <div class="event-detail__header" @mousedown="onDragStart">
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

        <div class="event-detail__resize-handle" @mousedown="onResizeStart">
          <svg width="12" height="12" viewBox="0 0 12 12" fill="none">
            <path d="M10 2L2 10M10 6L6 10M10 10L10 10" stroke="rgba(255,255,255,0.3)" stroke-width="1.5" stroke-linecap="round"/>
          </svg>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<style scoped lang="scss">
@use '@/styles/variables' as *;

.event-detail-backdrop {
  position: fixed;
  inset: 0;
  z-index: $z-modal;
  background: rgba(0, 0, 0, 0.2);
}

.event-detail {
  position: absolute;
  background: rgba(20, 18, 40, 0.95);
  backdrop-filter: blur(var(--glass-blur)) saturate(1.4);
  -webkit-backdrop-filter: blur(var(--glass-blur)) saturate(1.4);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: $radius-xl;
  overflow: hidden;
  box-shadow: $shadow-lg;
  transition: box-shadow $transition-fast;

  &--positioned {
    transition: none;
  }

  &--dragging {
    box-shadow: 0 16px 48px rgba(0, 0, 0, 0.4);
    cursor: move;
    user-select: none;
  }

  &__bar {
    height: 4px;
  }

  &__header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: $space-5 $space-6 $space-3;
    cursor: grab;

    &:active {
      cursor: grabbing;
    }
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
    overflow-y: auto;
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
    font-size: $font-size-base;
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

  &__resize-handle {
    position: absolute;
    right: 0;
    bottom: 0;
    width: 24px;
    height: 24px;
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: nwse-resize;
    border-radius: $radius-xl 0 $radius-xl 0;
    transition: background $transition-fast;

    &:hover {
      background: rgba(255, 255, 255, 0.08);
    }
  }
}
</style>
