<script setup lang="ts">
import { ref, reactive, watch } from 'vue'
import { motion } from 'motion-v'
import { springPresets } from '@/composables/useMotion'
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
}>(), {
  open: false,
  initialDate: ''
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

watch(() => props.open, (opened) => {
  if (!opened) return
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
    form.title = ''
    form.description = ''
    form.date = d
    form.startTime = '09:00'
    form.endTime = '10:00'
    form.allDay = false
    form.location = ''
    form.color = '#6366f1'
    form.category = 'personal'
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
      @click="emit('close')"
    >
      <motion.div
        class="event-form"
        :initial="{ opacity: 0, scale: 0.92, y: 20 }"
        :animate="{ opacity: 1, scale: 1, y: 0 }"
        :exit="{ opacity: 0, scale: 0.92, y: 20 }"
        :transition="springPresets.modal"
        @click.stop
      >
        <div class="event-form__header">
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

          <div class="event-form__field">
            <label>颜色</label>
            <div class="event-form__colors">
              <button
                v-for="c in ['#6366f1', '#22c55e', '#f59e0b', '#ef4444', '#ec4899', '#3b82f6', '#8b5cf6', '#14b8a6']"
                :key="c"
                class="event-form__color"
                :class="{ 'event-form__color--active': form.color === c }"
                :style="{ background: c }"
                @click="form.color = c"
              />
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
      </motion.div>
    </motion.div>
  </Teleport>
</template>

<style scoped lang="scss">
@use '@/styles/variables' as *;

.event-form-backdrop {
  position: fixed;
  inset: 0;
  z-index: $z-modal;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.4);
  backdrop-filter: blur(8px);
  padding: $space-4;
}

.event-form {
  width: 100%;
  max-width: 480px;
  max-height: 85vh;
  overflow-y: auto;
  background: rgba(20, 18, 40, 0.85);
  backdrop-filter: blur(32px) saturate(1.4);
  -webkit-backdrop-filter: blur(32px) saturate(1.4);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: $radius-xl;
  box-shadow: $shadow-lg;

  &__header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: $space-5 $space-6;
    border-bottom: 1px solid rgba(255, 255, 255, 0.06);

    h2 {
      font-size: $font-size-lg;
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
      font-size: $font-size-xs;
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
    font-size: $font-size-sm;
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
    padding: $space-1 $space-3;
    border-radius: $radius-full;
    font-size: $font-size-xs;
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

  &__colors {
    display: flex;
    gap: $space-2;
    flex-wrap: wrap;
  }

  &__color {
    width: 28px;
    height: 28px;
    border-radius: 50%;
    border: 2px solid transparent;
    cursor: pointer;
    transition: border-color $transition-fast, transform $transition-fast;

    &:hover {
      transform: scale(1.15);
    }

    &--active {
      border-color: white;
      box-shadow: 0 0 8px rgba(255, 255, 255, 0.3);
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
}
</style>
