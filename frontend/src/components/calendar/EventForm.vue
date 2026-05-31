<script setup lang="ts">
import { ref, reactive, watch } from 'vue'
import { motion } from 'motion-v'
import GlassInput from '@/components/glass/GlassInput.vue'
import GlassButton from '@/components/glass/GlassButton.vue'
import type { CalendarEvent } from '@/types/event'

interface FormState {
  title: string
  description: string
  date: string
  startTime: string
  endTime: string
  allDay: boolean
  location: string
  color: string
  category: string
}

const props = withDefaults(defineProps<{
  open: boolean
  event?: CalendarEvent | null
  initialDate?: string
  initialTitle?: string
  initialStartTime?: string
  initialEndTime?: string
  initialLocation?: string
  initialCategory?: string
  initialAllDay?: boolean
}>(), {
  open: false,
  initialDate: '',
  initialTitle: '',
  initialStartTime: '',
  initialEndTime: '',
  initialLocation: '',
  initialCategory: 'personal',
  initialAllDay: false
})

const emit = defineEmits<{
  close: []
  save: [data: FormState]
  delete: [id: number]
}>()

const form = reactive<FormState>({
  title: '',
  description: '',
  date: '',
  startTime: '',
  endTime: '',
  allDay: false,
  location: '',
  color: '#6366f1',
  category: 'personal'
})

const isEditing = ref(false)

// Drag state
const pos = reactive({ x: 0, y: 0 })
const size = reactive({ w: 480, h: 0 })
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

function centerForm() {
  const vw = window.innerWidth
  const vh = window.innerHeight
  pos.x = Math.max(0, (vw - size.w) / 2)
  pos.y = Math.max(0, (vh - vh * 0.85) / 2)
  isPositioned.value = true
}

function onDragStart(e: MouseEvent) {
  if ((e.target as HTMLElement).closest('.event-form__close')) return
  isDragging.value = true
  dragStartX = e.clientX
  dragStartY = e.clientY
  dragStartPosX = pos.x
  dragStartPosY = pos.y
  e.preventDefault()
}

function onDragMove(e: MouseEvent) {
  if (isDragging.value) {
    pos.x = dragStartPosX + (e.clientX - dragStartX)
    pos.y = dragStartPosY + (e.clientY - dragStartY)
  }
  if (isResizing.value) {
    size.w = Math.max(380, resizeStartW + (e.clientX - resizeStartX))
    size.h = Math.max(300, resizeStartH + (e.clientY - resizeStartY))
  }
}

function onDragEnd() {
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

watch(() => props.open, (opened) => {
  if (!opened) return
  isPositioned.value = false
  size.h = 0
  centerForm()

  if (props.event) {
    isEditing.value = true
    const s = new Date(props.event.startTime)
    const e = new Date(props.event.endTime)
    form.title = props.event.title
    form.description = props.event.description ?? ''
    form.date = s.toISOString().slice(0, 10)
    form.startTime = s.toTimeString().slice(0, 5)
    form.endTime = e.toTimeString().slice(0, 5)
    form.allDay = props.event.allDay ?? false
    form.location = props.event.location ?? ''
    form.color = props.event.color ?? '#6366f1'
    form.category = props.event.category ?? 'personal'
  } else {
    isEditing.value = false
    const d = props.initialDate || new Date().toISOString().slice(0, 10)
    form.title = props.initialTitle || ''
    form.description = ''
    form.date = d
    form.startTime = props.initialStartTime || '09:00'
    form.endTime = props.initialEndTime || '10:00'
    form.allDay = props.initialAllDay || false
    form.location = props.initialLocation || ''
    form.color = '#6366f1'
    form.category = props.initialCategory || 'personal'
  }
})

function handleSave() {
  emit('save', { ...form })
}

function handleDelete() {
  if (props.event) emit('delete', props.event.id)
}
</script>

<template>
  <Teleport to="body">
    <motion.div
      v-if="open"
      class="event-form-backdrop"
      :initial="{ opacity: 0 }"
      :animate="{ opacity: 1 }"
      :exit="{ opacity: 0 }"
      :transition="{ duration: 0.15 }"
      @mousemove="onDragMove"
      @mouseup="onDragEnd"
    >
      <div
        class="event-form"
        :class="{
          'event-form--dragging': isDragging || isResizing,
          'event-form--positioned': isPositioned
        }"
        :style="{
          left: pos.x + 'px',
          top: pos.y + 'px',
          width: size.w + 'px',
          maxHeight: size.h > 0 ? size.h + 'px' : '85vh'
        }"
        @click.stop
      >
        <div class="event-form__header" @mousedown="onDragStart">
          <h2>{{ isEditing ? '编辑日程' : '新建日程' }}</h2>
          <button class="event-form__close" @click="emit('close')">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
              <line x1="18" y1="6" x2="6" y2="18" /><line x1="6" y1="6" x2="18" y2="18" />
            </svg>
          </button>
        </div>

        <div class="event-form__body">
          <div class="event-form__field">
            <label>标题</label>
            <GlassInput v-model="form.title" placeholder="事件标题" />
          </div>

          <div class="event-form__row">
            <div class="event-form__field">
              <label>日期</label>
              <GlassInput v-model="form.date" type="date" />
            </div>
          </div>

          <div v-if="!form.allDay" class="event-form__row">
            <div class="event-form__field">
              <label>开始</label>
              <GlassInput v-model="form.startTime" type="time" />
            </div>
            <div class="event-form__field">
              <label>结束</label>
              <GlassInput v-model="form.endTime" type="time" />
            </div>
          </div>

          <div class="event-form__field">
            <label class="event-form__checkbox">
              <input v-model="form.allDay" type="checkbox" />
              <span>全天事件</span>
            </label>
          </div>

          <div class="event-form__field">
            <label>地点</label>
            <GlassInput v-model="form.location" placeholder="地点（可选）" />
          </div>

          <div class="event-form__field">
            <label>描述</label>
            <GlassInput v-model="form.description" placeholder="描述（可选）" multiline :rows="2" />
          </div>

          <div class="event-form__field">
            <label>分类</label>
            <div class="event-form__categories">
              <button
                v-for="cat in [{ id: 'personal', label: '个人', color: '#22c55e' }, { id: 'work', label: '工作', color: '#6366f1' }, { id: 'health', label: '健康', color: '#f59e0b' }, { id: 'social', label: '社交', color: '#ec4899' }]"
                :key="cat.id"
                class="event-form__category"
                :class="{ 'event-form__category--active': form.category === cat.id }"
                :style="{ '--cat-color': cat.color }"
                @click="form.category = cat.id"
              >
                {{ cat.label }}
              </button>
            </div>
          </div>

        </div>

        <div class="event-form__footer">
          <div class="event-form__footer-left">
            <GlassButton v-if="isEditing" variant="ghost" @click="handleDelete">
              删除
            </GlassButton>
          </div>
          <div class="event-form__footer-right">
            <GlassButton variant="ghost" @click="emit('close')">取消</GlassButton>
            <GlassButton variant="primary" :disabled="!form.title.trim()" @click="handleSave">
              {{ isEditing ? '保存' : '创建' }}
            </GlassButton>
          </div>
        </div>

        <div class="event-form__resize-handle" @mousedown="onResizeStart">
          <svg width="12" height="12" viewBox="0 0 12 12" fill="none">
            <path d="M10 2L2 10M10 6L6 10M10 10L10 10" stroke="rgba(255,255,255,0.3)" stroke-width="1.5" stroke-linecap="round"/>
          </svg>
        </div>
      </div>
    </motion.div>
  </Teleport>
</template>

<style scoped lang="scss">
@use '@/styles/variables' as *;

.event-form-backdrop {
  position: fixed;
  inset: 0;
  z-index: $z-modal;
  background: rgba(0, 0, 0, 0.3);
  padding: 0;
}

.event-form {
  position: absolute;
  overflow-y: auto;
  background: rgba(20, 18, 40, 0.92);
  backdrop-filter: blur(var(--glass-blur)) saturate(1.4);
  -webkit-backdrop-filter: blur(var(--glass-blur)) saturate(1.4);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: $radius-xl;
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

  &__header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: $space-5 $space-6;
    border-bottom: 1px solid rgba(255, 255, 255, 0.06);
    cursor: grab;

    &:active {
      cursor: grabbing;
    }

    h2 {
      font-size: $font-size-xl;
      font-weight: $font-weight-semibold;
    }
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
    transition: background $transition-fast;

    &:hover {
      background: rgba(255, 255, 255, 0.12);
      color: $color-text-primary;
    }
  }

  &__body {
    padding: $space-5 $space-6;
    display: flex;
    flex-direction: column;
    gap: $space-4;
  }

  &__row {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: $space-3;
  }

  &__field {
    display: flex;
    flex-direction: column;
    gap: $space-1;

    label {
      font-size: $font-size-sm;
      color: $color-text-tertiary;
      text-transform: uppercase;
      letter-spacing: 0.05em;
    }
  }

  &__checkbox {
    display: flex;
    align-items: center;
    gap: $space-2;
    cursor: pointer;
    font-size: $font-size-base;
    color: $color-text-secondary;
    text-transform: none;
    letter-spacing: normal;

    input {
      width: 16px;
      height: 16px;
      accent-color: $color-primary;
    }
  }

  &__categories {
    display: flex;
    gap: $space-2;
    flex-wrap: wrap;
  }

  &__category {
    padding: $space-2 $space-4;
    border-radius: $radius-full;
    font-size: $font-size-sm;
    font-family: $font-family;
    font-weight: $font-weight-medium;
    cursor: pointer;
    border: 1px solid rgba(255, 255, 255, 0.08);
    background: rgba(255, 255, 255, 0.04);
    color: $color-text-secondary;
    transition: all $transition-fast;

    &:hover {
      background: rgba(255, 255, 255, 0.1);
    }

    &--active {
      background: rgba(var(--cat-color), 0.2);
      border-color: var(--cat-color);
      color: var(--cat-color);
    }
  }

  &__footer {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: $space-4 $space-6;
    border-top: 1px solid rgba(255, 255, 255, 0.06);
  }

  &__footer-left,
  &__footer-right {
    display: flex;
    gap: $space-2;
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
