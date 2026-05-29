<script setup lang="ts">
import { motion } from 'motion-v'
import { springPresets } from '@/composables/useMotion'

export interface ToolbarItem {
  id: string
  label: string
  icon: string
}

withDefaults(defineProps<{
  items?: ToolbarItem[]
  activeId?: string
}>(), {
  items: () => [
    { id: 'month', label: '月', icon: 'grid' },
    { id: 'week', label: '周', icon: 'list' },
    { id: 'day', label: '日', icon: 'calendar' },
    { id: 'search', label: '搜索', icon: 'search' },
    { id: 'settings', label: '设置', icon: 'settings' },
    { id: 'profile', label: '我的', icon: 'user' }
  ],
  activeId: 'month'
})

const emit = defineEmits<{
  select: [id: string]
}>()
</script>

<template>
  <div class="bottom-toolbar">
    <div class="bottom-toolbar__inner">
      <motion.button
        v-for="item in items"
        :key="item.id"
        class="toolbar-item"
        :class="{ 'toolbar-item--active': item.id === activeId }"
        :layout="true"
        :while-hover="{ y: -4, scale: 1.08 }"
        :while-tap="{ scale: 0.92 }"
        :transition="springPresets.gentle"
        @click="emit('select', item.id)"
      >
        <div class="toolbar-item__icon">
          <svg v-if="item.icon === 'grid'" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round">
            <rect x="3" y="3" width="7" height="7" /><rect x="14" y="3" width="7" height="7" /><rect x="14" y="14" width="7" height="7" /><rect x="3" y="14" width="7" height="7" />
          </svg>
          <svg v-else-if="item.icon === 'list'" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round">
            <line x1="8" y1="6" x2="21" y2="6" /><line x1="8" y1="12" x2="21" y2="12" /><line x1="8" y1="18" x2="21" y2="18" /><line x1="3" y1="6" x2="3.01" y2="6" /><line x1="3" y1="12" x2="3.01" y2="12" /><line x1="3" y1="18" x2="3.01" y2="18" />
          </svg>
          <svg v-else-if="item.icon === 'calendar'" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round">
            <rect x="3" y="4" width="18" height="18" rx="2" /><line x1="16" y1="2" x2="16" y2="6" /><line x1="8" y1="2" x2="8" y2="6" /><line x1="3" y1="10" x2="21" y2="10" />
          </svg>
          <svg v-else-if="item.icon === 'search'" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round">
            <circle cx="11" cy="11" r="8" /><line x1="21" y1="21" x2="16.65" y2="16.65" />
          </svg>
          <svg v-else-if="item.icon === 'settings'" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round">
            <circle cx="12" cy="12" r="3" /><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06A1.65 1.65 0 0 0 4.68 15a1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06A1.65 1.65 0 0 0 9 4.68a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06A1.65 1.65 0 0 0 19.4 9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z" />
          </svg>
          <svg v-else-if="item.icon === 'user'" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round">
            <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" /><circle cx="12" cy="7" r="4" />
          </svg>
        </div>
        <span class="toolbar-item__label">{{ item.label }}</span>
      </motion.button>
    </div>
  </div>
</template>

<style scoped lang="scss">
@use '@/styles/variables' as *;

.bottom-toolbar {
  display: flex;
  align-items: center;
  justify-content: center;
  height: $calendar-bottom-toolbar-height;
  position: relative;
  z-index: 2;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
  background: rgba(0, 0, 0, 0.15);
  backdrop-filter: blur(var(--glass-blur));
  -webkit-backdrop-filter: blur(var(--glass-blur));
}

.bottom-toolbar__inner {
  display: flex;
  align-items: center;
  gap: $space-1;
  padding: $space-1 $space-2;
  border-radius: $radius-xl;
  background: rgba(255, 255, 255, 0.04);
}

.toolbar-item {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  padding: $space-1 $space-3;
  border-radius: $radius-lg;
  border: none;
  background: transparent;
  color: $color-text-tertiary;
  cursor: pointer;
  font-family: $font-family;
  -webkit-tap-highlight-color: transparent;
  transition: color $transition-fast, background $transition-fast;

  &:hover {
    color: $color-text-secondary;
  }

  &--active {
    color: $color-primary-light;
    background: rgba($color-primary, 0.08);

    &:hover {
      color: $color-primary-light;
      background: rgba($color-primary, 0.12);
    }

    .toolbar-item__label {
      color: $color-primary-light;
    }
  }

  &__icon {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 24px;
    height: 24px;
  }

  &__label {
    font-size: 10px;
    font-weight: $font-weight-medium;
    line-height: 1;
    color: inherit;
    transition: color $transition-fast;
  }
}
</style>
