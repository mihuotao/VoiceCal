<script setup lang="ts">
import { computed } from 'vue'
import { motion } from 'motion-v'
import { springPresets } from '@/composables/useMotion'
import type { FestivalInfo } from '@/composables/useFestival'

const props = defineProps<{
  festival: FestivalInfo | null
}>()

const show = computed(() => props.festival !== null)
</script>

<template>
  <motion.div
    v-if="show"
    class="festival-card"
    :initial="{ opacity: 0, y: 12, scale: 0.97 }"
    :animate="{ opacity: 1, y: 0, scale: 1 }"
    :exit="{ opacity: 0, y: -8, scale: 0.97 }"
    :transition="springPresets.gentle"
  >
    <div class="festival-emoji">{{ festival!.emoji }}</div>
    <div class="festival-info">
      <div class="festival-name">{{ festival!.name }}</div>
      <div class="festival-greeting">{{ festival!.greeting }}</div>
    </div>
    <div v-if="festival!.type === 'solarTerm'" class="festival-badge">节气</div>
    <div v-else-if="festival!.type === 'festival'" class="festival-badge festival-badge--festival">节日</div>
  </motion.div>
</template>

<style scoped lang="scss">
@use '@/styles/variables' as *;

.festival-card {
  display: flex;
  align-items: center;
  gap: $space-3;
  padding: $space-3 $space-4;
  background: rgba(255, 255, 255, 0.06);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: $radius-lg;
  margin: $space-2 0;
}

.festival-emoji {
  font-size: 28px;
  line-height: 1;
  flex-shrink: 0;
}

.festival-info {
  flex: 1;
  min-width: 0;
}

.festival-name {
  font-size: $font-size-sm;
  font-weight: $font-weight-semibold;
  color: white;
}

.festival-greeting {
  font-size: $font-size-xs;
  color: $color-text-secondary;
  margin-top: $space-1;
  line-height: 1.4;
}

.festival-badge {
  flex-shrink: 0;
  padding: $space-1 $space-2;
  border-radius: $radius-sm;
  font-size: 10px;
  font-weight: $font-weight-semibold;
  background: rgba(34, 197, 94, 0.15);
  color: #4ade80;
  border: 1px solid rgba(34, 197, 94, 0.2);

  &--festival {
    background: rgba($color-primary, 0.15);
    color: rgba($color-primary, 0.9);
    border-color: rgba($color-primary, 0.2);
  }
}
</style>
