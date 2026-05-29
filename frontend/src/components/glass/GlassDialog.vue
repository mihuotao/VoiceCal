<script setup lang="ts">
import { motion } from 'motion-v'
import { springPresets } from '@/composables/useMotion'

withDefaults(defineProps<{
  open?: boolean
  title?: string
  width?: string
  closable?: boolean
}>(), {
  open: false,
  title: '',
  width: '420px',
  closable: true
})

const emit = defineEmits<{
  close: []
}>()
</script>

<template>
  <Teleport to="body">
    <motion.div
      v-if="open"
      class="glass-dialog-overlay"
      :initial="{ opacity: 0 }"
      :animate="{ opacity: 1 }"
      :exit="{ opacity: 0 }"
      :transition="{ duration: 0.2 }"
      @click.self="closable && emit('close')"
    >
      <motion.div
        class="glass-dialog"
        :style="{ maxWidth: width }"
        :initial="{ opacity: 0, scale: 0.9, y: 20 }"
        :animate="{ opacity: 1, scale: 1, y: 0 }"
        :exit="{ opacity: 0, scale: 0.9, y: 20 }"
        :transition="springPresets.modal"
      >
        <div v-if="title || closable" class="glass-dialog__header">
          <h2 v-if="title" class="glass-dialog__title">{{ title }}</h2>
          <button
            v-if="closable"
            class="glass-dialog__close"
            @click="emit('close')"
          >
            ✕
          </button>
        </div>
        <div class="glass-dialog__body">
          <slot />
        </div>
        <div v-if="$slots.footer" class="glass-dialog__footer">
          <slot name="footer" />
        </div>
      </motion.div>
    </motion.div>
  </Teleport>
</template>

<style scoped lang="scss">
@use '@/styles/variables' as *;

.glass-dialog-overlay {
  position: fixed;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: $surface-overlay;
  backdrop-filter: blur(4px);
  z-index: $z-modal;
}

.glass-dialog {
  width: 90%;
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(32px) saturate(1.4);
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: $radius-lg;
  box-shadow: $shadow-lg;
  max-height: 80vh;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.glass-dialog__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: $space-4 $space-6;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.glass-dialog__title {
  font-size: $font-size-lg;
  font-weight: $font-weight-semibold;
  color: $color-text-primary;
}

.glass-dialog__close {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: $radius-full;
  color: $color-text-secondary;
  font-size: $font-size-sm;
  transition: all $transition-fast;

  &:hover {
    background: rgba(255, 255, 255, 0.1);
    color: $color-text-primary;
  }
}

.glass-dialog__body {
  padding: $space-6;
  overflow-y: auto;
  flex: 1;
}

.glass-dialog__footer {
  padding: $space-4 $space-6;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
  display: flex;
  justify-content: flex-end;
  gap: $space-3;
}
</style>
