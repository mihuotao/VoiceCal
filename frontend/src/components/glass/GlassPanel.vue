<script setup lang="ts">
import { computed } from 'vue'
import { useThemeStore } from '@/stores/theme'

const props = withDefaults(defineProps<{
  blur?: number
  opacity?: number
  color?: string
  borderRadius?: string
  hoverable?: boolean
  padded?: boolean
}>(), {
  blur: 32,
  opacity: 0.75,
  color: '',
  borderRadius: '16px',
  hoverable: false,
  padded: true
})

const themeStore = useThemeStore()

const glassStyle = computed(() => ({
  background: `rgba(255, 255, 255, ${(props.opacity ?? themeStore.state.glassOpacity) * 0.12})`,
  backdropFilter: `blur(${props.blur ?? themeStore.state.glassBlur}px) saturate(1.4)`,
  WebkitBackdropFilter: `blur(${props.blur ?? themeStore.state.glassBlur}px) saturate(1.4)`,
  border: '1px solid rgba(255, 255, 255, 0.12)',
  borderRadius: props.borderRadius,
  ...(props.color ? { '--panel-color': props.color } : {})
}))
</script>

<template>
  <div
    class="glass-panel"
    :class="{ 'glass-panel--hoverable': hoverable }"
    :style="glassStyle"
  >
    <slot />
  </div>
</template>

<style scoped lang="scss">
@use '@/styles/variables' as *;

.glass-panel {
  padding: v-bind('props.padded ? "16px" : "0"');
  transition: all $transition-base;

  &--hoverable {
    cursor: pointer;
    &:hover {
      background: rgba(255, 255, 255, 0.18);
      box-shadow: $shadow-md;
      transform: translateY(-2px);
    }
    &:active {
      transform: translateY(0) scale(0.98);
    }
  }
}
</style>
