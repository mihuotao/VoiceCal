<script setup lang="ts">
import { ref, nextTick, watch } from 'vue'
import { motion } from 'motion-v'
import { springPresets } from '@/composables/useMotion'

const props = withDefaults(defineProps<{
  dateKey: string
  dateLabel: string
  modelValue?: string
  open?: boolean
}>(), {
  modelValue: '',
  open: false
})

const emit = defineEmits<{
  'update:modelValue': [value: string]
  save: [dateKey: string, text: string]
  close: []
}>()

const inputRef = ref<HTMLTextAreaElement | null>(null)
const localText = ref(props.modelValue)

watch(() => props.modelValue, (v) => {
  localText.value = v ?? ''
})

watch(() => props.open, (opened) => {
  if (opened) {
    nextTick(() => {
      inputRef.value?.focus()
      inputRef.value?.select()
    })
  }
})

function handleSave() {
  emit('update:modelValue', localText.value)
  emit('save', props.dateKey, localText.value)
  emit('close')
}

function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    handleSave()
  }
  if (e.key === 'Escape') {
    emit('close')
  }
}
</script>

<template>
  <Teleport to="body">
    <motion.div
      v-if="open"
      class="inline-note-backdrop"
      :initial="{ opacity: 0 }"
      :animate="{ opacity: 1 }"
      :exit="{ opacity: 0 }"
      :transition="{ duration: 0.15 }"
      @click="emit('close')"
    >
      <motion.div
        class="inline-note"
        :initial="{ opacity: 0, y: 16, scale: 0.95 }"
        :animate="{ opacity: 1, y: 0, scale: 1 }"
        :exit="{ opacity: 0, y: 16, scale: 0.95 }"
        :transition="springPresets.snappy"
        @click.stop
      >
        <div class="inline-note__header">
          <span class="inline-note__date">{{ dateLabel }}</span>
          <button class="inline-note__close" @click="emit('close')">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
              <line x1="18" y1="6" x2="6" y2="18" /><line x1="6" y1="6" x2="18" y2="18" />
            </svg>
          </button>
        </div>
        <textarea
          ref="inputRef"
          v-model="localText"
          class="inline-note__input"
          placeholder="输入笔记..."
          rows="3"
          @keydown="handleKeydown"
          @blur="handleSave"
        />
        <div class="inline-note__hint">
          Enter 保存 · Shift+Enter 换行 · Esc 取消
        </div>
      </motion.div>
    </motion.div>
  </Teleport>
</template>

<style scoped lang="scss">
@use '@/styles/variables' as *;

.inline-note-backdrop {
  position: fixed;
  inset: 0;
  z-index: $z-overlay;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding-top: 20vh;
  background: rgba(0, 0, 0, 0.2);
}

.inline-note {
  width: 320px;
  max-width: 90vw;
  background: rgba(255, 255, 255, 0.08);
  backdrop-filter: blur(24px) saturate(1.4);
  -webkit-backdrop-filter: blur(24px) saturate(1.4);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: $radius-lg;
  padding: $space-4;
  box-shadow: $shadow-lg;

  &__header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: $space-3;
  }

  &__date {
    font-size: $font-size-sm;
    font-weight: $font-weight-semibold;
    color: $color-text-primary;
  }

  &__close {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 24px;
    height: 24px;
    border-radius: 50%;
    background: rgba(255, 255, 255, 0.06);
    border: 1px solid rgba(255, 255, 255, 0.06);
    color: $color-text-tertiary;
    cursor: pointer;
    transition: background $transition-fast, color $transition-fast;

    &:hover {
      background: rgba(255, 255, 255, 0.12);
      color: $color-text-primary;
    }
  }

  &__input {
    width: 100%;
    background: rgba(0, 0, 0, 0.2);
    border: 1px solid rgba(255, 255, 255, 0.08);
    border-radius: $radius-md;
    padding: $space-2 $space-3;
    color: $color-text-primary;
    font-family: $font-family;
    font-size: $font-size-base;
    line-height: $line-height-base;
    resize: none;
    outline: none;
    transition: border-color $transition-fast;
    box-sizing: border-box;

    &::placeholder {
      color: $color-text-tertiary;
    }

    &:focus {
      border-color: rgba($color-primary, 0.4);
    }
  }

  &__hint {
    margin-top: $space-2;
    font-size: $font-size-xs;
    color: $color-text-tertiary;
    opacity: 0.6;
  }
}
</style>
