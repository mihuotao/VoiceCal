<script setup lang="ts">
import { motion } from 'motion-v'
import { springPresets } from '@/composables/useMotion'

defineProps<{
  activeId?: string
}>()

const emit = defineEmits<{
  select: [id: string]
}>()

const buttons = [
  { id: 'new', label: '新建', icon: '➕' },
  { id: 'settings', label: '设置', icon: '⚙️' },
  { id: 'profile', label: '我的', icon: '👤' }
]
</script>

<template>
  <div class="inline-toolbar">
    <motion.button
      v-for="btn in buttons"
      :key="btn.id"
      class="inline-toolbar__btn"
      :class="{ 'inline-toolbar__btn--active': activeId === btn.id }"
      :while-hover="{ scale: 1.06 }"
      :while-tap="{ scale: 0.94 }"
      :transition="springPresets.snappy"
      @click="emit('select', btn.id)"
    >
      <span class="inline-toolbar__icon">{{ btn.icon }}</span>
      <span class="inline-toolbar__label">{{ btn.label }}</span>
    </motion.button>
  </div>
</template>

<style scoped lang="scss">
@use '@/styles/variables' as *;

.inline-toolbar {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: $space-1;
  padding: $space-2;
  background: rgba(255, 255, 255, 0.04);
  backdrop-filter: blur(var(--glass-blur));
  -webkit-backdrop-filter: blur(var(--glass-blur));
  border-radius: $radius-lg;
  border: 1px solid rgba(255, 255, 255, 0.06);
}

.inline-toolbar__btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: $space-1;
  padding: $space-2 $space-1;
  border: none;
  border-radius: $radius-md;
  background: transparent;
  color: $color-text-secondary;
  cursor: pointer;
  font-family: $font-family;
  transition: background $transition-fast, color $transition-fast;

  &:hover {
    background: rgba(255, 255, 255, 0.08);
    color: $color-text-primary;
  }

  &--active {
    background: rgba($color-primary, 0.15);
    color: $color-primary-light;
  }
}

.inline-toolbar__icon {
  font-size: 20px;
  line-height: 1;
}

.inline-toolbar__label {
  font-size: $font-size-sm;
  font-weight: $font-weight-medium;
  line-height: 1;
}
</style>
