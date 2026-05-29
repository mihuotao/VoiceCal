<script setup lang="ts">
import { motion } from 'motion-v'
import { springPresets } from '@/composables/useMotion'
import GlassButton from '@/components/glass/GlassButton.vue'
import type { SearchResultItem } from '@/types/search'

defineProps<{
  item: SearchResultItem
  index: number
}>()

const emit = defineEmits<{
  createEvent: [item: SearchResultItem]
  openUrl: [url: string]
}>()
</script>

<template>
  <motion.div
    class="search-card"
    :initial="{ opacity: 0, y: 20 }"
    :animate="{ opacity: 1, y: 0 }"
    :transition="{ ...springPresets.gentle, delay: index * 0.06 }"
  >
    <div class="search-card__icon">{{ item.icon || '🔍' }}</div>

    <div class="search-card__content">
      <a
        class="search-card__title"
        :href="item.url"
        target="_blank"
        rel="noopener noreferrer"
      >{{ item.title }}</a>
      <p class="search-card__snippet">{{ item.snippet }}</p>
    </div>

    <div class="search-card__actions">
      <GlassButton
        variant="ghost"
        size="sm"
        @click="emit('createEvent', item)"
      >
        转日程
      </GlassButton>
    </div>
  </motion.div>
</template>

<style scoped lang="scss">
@use '@/styles/variables' as *;

.search-card {
  display: flex;
  gap: $space-3;
  padding: $space-3 $space-4;
  border-radius: $radius-md;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.05);
  transition: background $transition-fast, border-color $transition-fast;

  &:hover {
    background: rgba(255, 255, 255, 0.06);
    border-color: rgba(255, 255, 255, 0.1);
  }

  &__icon {
    font-size: 24px;
    line-height: 1;
    flex-shrink: 0;
    margin-top: 2px;
  }

  &__content {
    flex: 1;
    min-width: 0;
    display: flex;
    flex-direction: column;
    gap: $space-1;
  }

  &__title {
    font-size: $font-size-base;
    font-weight: $font-weight-semibold;
    color: $color-primary-light;
    text-decoration: none;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;

    &:hover {
      text-decoration: underline;
    }
  }

  &__snippet {
    font-size: $font-size-sm;
    color: $color-text-secondary;
    line-height: $line-height-base;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }

  &__actions {
    flex-shrink: 0;
    display: flex;
    align-items: flex-start;
  }
}
</style>
