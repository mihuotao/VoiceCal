<script setup lang="ts">
import { motion } from 'motion-v'
import { springPresets } from '@/composables/useMotion'

withDefaults(defineProps<{
  variant?: 'default' | 'primary' | 'ghost'
  size?: 'sm' | 'md' | 'lg'
  disabled?: boolean
  loading?: boolean
  rounded?: boolean
}>(), {
  variant: 'default',
  size: 'md',
  disabled: false,
  loading: false,
  rounded: false
})

const emit = defineEmits<{
  click: [e: MouseEvent]
}>()
</script>

<template>
  <motion.button
    class="glass-button"
    :class="[
      `glass-button--${variant}`,
      `glass-button--${size}`,
      { 'glass-button--rounded': rounded }
    ]"
    :disabled="disabled || loading"
    :while-hover="disabled ? {} : { scale: 1.04 }"
    :while-tap="disabled ? {} : { scale: 0.97 }"
    :transition="springPresets.snappy"
    @click="emit('click', $event)"
  >
    <span v-if="loading" class="glass-button__spinner" />
    <slot />
  </motion.button>
</template>

<style scoped lang="scss">
@use '@/styles/variables' as *;

.glass-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: $space-2;
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: $radius-md;
  cursor: pointer;
  user-select: none;
  -webkit-tap-highlight-color: transparent;
  font-family: $font-family;
  font-weight: $font-weight-medium;
  transition: background $transition-base, box-shadow $transition-base;
  white-space: nowrap;

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }

  &--default {
    background: rgba(255, 255, 255, 0.1);
    backdrop-filter: blur(8px);
    color: $color-text-primary;

    &:hover:not(:disabled) {
      background: rgba(255, 255, 255, 0.16);
      box-shadow: $shadow-md;
    }
  }

  &--primary {
    background: rgba($color-primary, 0.25);
    backdrop-filter: blur(8px);
    color: white;
    border-color: rgba($color-primary, 0.3);

    &:hover:not(:disabled) {
      background: rgba($color-primary, 0.35);
      box-shadow: $shadow-glow-primary;
    }
  }

  &--ghost {
    background: transparent;
    border-color: transparent;
    color: $color-text-secondary;

    &:hover:not(:disabled) {
      background: rgba(255, 255, 255, 0.08);
      color: $color-text-primary;
    }
  }

  &--sm {
    padding: $space-1 $space-3;
    font-size: $font-size-sm;
  }

  &--md {
    padding: $space-2 $space-4;
    font-size: $font-size-base;
  }

  &--lg {
    padding: $space-3 $space-6;
    font-size: $font-size-lg;
  }

  &--rounded {
    border-radius: $radius-full;
  }

  &__spinner {
    width: 14px;
    height: 14px;
    border: 2px solid rgba(255, 255, 255, 0.2);
    border-top-color: white;
    border-radius: 50%;
    animation: rotate-slow 0.6s linear infinite;
  }
}
</style>
