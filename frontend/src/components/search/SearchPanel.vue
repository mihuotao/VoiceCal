<script setup lang="ts">
import { ref } from 'vue'
import { motion } from 'motion-v'
import { springPresets } from '@/composables/useMotion'
import GlassInput from '@/components/glass/GlassInput.vue'
import SearchResultCard from '@/components/search/SearchResultCard.vue'
import type { SearchResultItem } from '@/types/search'

const props = withDefaults(defineProps<{
  open: boolean
  query?: string
  results?: SearchResultItem[]
  summary?: string
  isSearching?: boolean
}>(), {
  open: false,
  query: '',
  results: () => [],
  summary: '',
  isSearching: false
})

const emit = defineEmits<{
  close: []
  search: [query: string]
  createEvent: [item: SearchResultItem]
}>()

const input = ref(props.query)
</script>

<template>
  <Teleport to="body">
    <motion.div
      v-if="open"
      class="search-panel"
      :initial="{ opacity: 0 }"
      :animate="{ opacity: 1 }"
      :exit="{ opacity: 0 }"
      :transition="{ duration: 0.2 }"
    >
      <motion.div
        class="search-panel__backdrop"
        :initial="{ opacity: 0 }"
        :animate="{ opacity: 1 }"
        :exit="{ opacity: 0 }"
        :transition="{ duration: 0.25 }"
        @click="emit('close')"
      />

      <motion.div
        class="search-panel__sheet"
        :initial="{ y: '100%', opacity: 0 }"
        :animate="{ y: 0, opacity: 1 }"
        :exit="{ y: '100%', opacity: 0 }"
        :transition="springPresets.slide"
      >
        <div class="search-panel__handle" />

        <div class="search-panel__header">
          <div class="search-panel__input">
            <GlassInput
              v-model="input"
              placeholder="搜索互联网..."
              @keydown.enter="emit('search', input)"
            />
          </div>
          <button class="search-panel__close" @click="emit('close')">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
              <line x1="18" y1="6" x2="6" y2="18" /><line x1="6" y1="6" x2="18" y2="18" />
            </svg>
          </button>
        </div>

        <div class="search-panel__body">
          <div v-if="isSearching" class="search-panel__loading">
            <div class="search-panel__spinner" />
            <span>搜索中...</span>
          </div>

          <div v-else-if="results.length === 0 && !query" class="search-panel__empty">
            <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1" stroke-linecap="round" opacity="0.2">
              <circle cx="11" cy="11" r="8" /><line x1="21" y1="21" x2="16.65" y2="16.65" />
            </svg>
            <p>输入关键词，搜索互联网信息</p>
          </div>

          <div v-else-if="results.length > 0" class="search-panel__results">
            <div v-if="summary" class="search-panel__summary">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
                <circle cx="12" cy="12" r="10" /><line x1="12" y1="16" x2="12" y2="12" /><line x1="12" y1="8" x2="12.01" y2="8" />
              </svg>
              <span>{{ summary }}</span>
            </div>

            <div class="search-panel__cards">
              <SearchResultCard
                v-for="(item, i) in results"
                :key="i"
                :item="item"
                :index="i"
                @create-event="emit('createEvent', $event)"
              />
            </div>
          </div>

          <div v-else class="search-panel__no-results">
            <p>未找到"{{ query }}"的相关结果</p>
          </div>
        </div>
      </motion.div>
    </motion.div>
  </Teleport>
</template>

<style scoped lang="scss">
@use '@/styles/variables' as *;

.search-panel {
  position: fixed;
  inset: 0;
  z-index: $z-voice-overlay;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
}

.search-panel__backdrop {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  backdrop-filter: blur(8px);
}

.search-panel__sheet {
  position: relative;
  display: flex;
  flex-direction: column;
  background: rgba(15, 12, 41, 0.92);
  backdrop-filter: blur(32px) saturate(1.3);
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: $radius-xl $radius-xl 0 0;
  max-height: 85vh;
  overflow: hidden;
}

.search-panel__handle {
  width: 36px;
  height: 4px;
  border-radius: 2px;
  background: rgba(255, 255, 255, 0.15);
  margin: $space-2 auto;
}

.search-panel__header {
  display: flex;
  align-items: center;
  gap: $space-3;
  padding: $space-2 $space-5 $space-4;
}

.search-panel__input {
  flex: 1;
}

.search-panel__close {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.08);
  color: $color-text-secondary;
  cursor: pointer;
  flex-shrink: 0;

  &:hover {
    background: rgba(255, 255, 255, 0.14);
    color: $color-text-primary;
  }
}

.search-panel__body {
  flex: 1;
  overflow-y: auto;
  padding: 0 $space-5 $space-8;
}

.search-panel__loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: $space-3;
  padding: $space-16 0;
  color: $color-text-tertiary;
}

.search-panel__spinner {
  width: 24px;
  height: 24px;
  border: 2px solid rgba(255, 255, 255, 0.1);
  border-top-color: $color-primary-light;
  border-radius: 50%;
  animation: rotate-slow 0.6s linear infinite;
}

.search-panel__empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: $space-3;
  padding: $space-16 0;
  color: $color-text-tertiary;
  font-size: $font-size-sm;
}

.search-panel__no-results {
  text-align: center;
  padding: $space-16 0;
  color: $color-text-tertiary;
  font-size: $font-size-sm;
}

.search-panel__summary {
  display: flex;
  align-items: flex-start;
  gap: $space-2;
  padding: $space-3 $space-4;
  margin-bottom: $space-4;
  border-radius: $radius-md;
  background: rgba($color-primary, 0.06);
  border: 1px solid rgba($color-primary, 0.1);
  font-size: $font-size-sm;
  color: $color-text-secondary;
  line-height: $line-height-base;

  svg {
    flex-shrink: 0;
    margin-top: 2px;
    color: $color-primary-light;
  }
}

.search-panel__cards {
  display: flex;
  flex-direction: column;
  gap: $space-2;
}
</style>
