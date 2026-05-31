<script setup lang="ts">
import { ref, computed } from 'vue'
import { motion, AnimatePresence } from 'motion-v'
import { springPresets } from '@/composables/useMotion'

export interface FunctionMode {
  id: string
  label: string
  icon: string
  gradient: string
}

const modes: FunctionMode[] = [
  { id: 'CREATE', label: '创建', icon: '＋', gradient: 'linear-gradient(135deg, #22c55e 0%, #16a34a 100%)' },
  { id: 'QUERY', label: '查询', icon: '⌕', gradient: 'linear-gradient(135deg, #3b82f6 0%, #2563eb 100%)' },
]

defineProps<{
  transcript?: string
  extractedEntities?: Record<string, any> | null
}>()

const emit = defineEmits<{
  selectMode: [mode: string]
  execute: []
}>()

const selectedMode = ref<string>('')
const isExpanded = ref(false)

function selectMode(modeId: string) {
  if (selectedMode.value === modeId) {
    selectedMode.value = ''
    isExpanded.value = false
    emit('selectMode', '')
  } else {
    selectedMode.value = modeId
    isExpanded.value = true
    emit('selectMode', modeId)
  }
}

function handleExecute() {
  emit('execute')
}

const selectedModeInfo = computed(() => {
  return modes.find(m => m.id === selectedMode.value)
})
</script>

<template>
  <div class="voice-panel">
    <!-- 模式选择器 - 水平紧凑布局 -->
    <div class="voice-panel__modes">
      <motion.button
        v-for="mode in modes"
        :key="mode.id"
        class="voice-panel__mode"
        :class="{ 'voice-panel__mode--active': selectedMode === mode.id }"
        :style="{ '--mode-gradient': mode.gradient }"
        :while-hover="{ scale: 1.05, y: -1 }"
        :while-tap="{ scale: 0.95 }"
        :transition="springPresets.snappy"
        @click="selectMode(mode.id)"
      >
        <span class="voice-panel__mode-icon">{{ mode.icon }}</span>
        <span class="voice-panel__mode-label">{{ mode.label }}</span>
      </motion.button>
    </div>

    <!-- 展开区域 -->
    <AnimatePresence>
      <motion.div
        v-if="isExpanded && selectedModeInfo"
        class="voice-panel__expanded"
        :initial="{ height: 0, opacity: 0 }"
        :animate="{ height: 'auto', opacity: 1 }"
        :exit="{ height: 0, opacity: 0 }"
        :transition="{ duration: 0.25, ease: [0.4, 0, 0.2, 1] }"
      >
        <!-- 状态指示 -->
        <div class="voice-panel__status">
          <div class="voice-panel__status-dot" :style="{ background: selectedModeInfo.gradient }" />
          <span class="voice-panel__status-text">
            {{ selectedModeInfo.label }}模式
          </span>
        </div>

        <!-- 识别内容 -->
        <AnimatePresence>
          <motion.div
            v-if="transcript"
            class="voice-panel__transcript"
            :initial="{ opacity: 0, y: -8 }"
            :animate="{ opacity: 1, y: 0 }"
            :exit="{ opacity: 0 }"
          >
            <span class="voice-panel__transcript-label">识别</span>
            <span class="voice-panel__transcript-text">{{ transcript }}</span>
          </motion.div>
        </AnimatePresence>

        <!-- 提取结果 -->
        <AnimatePresence>
          <motion.div
            v-if="extractedEntities"
            class="voice-panel__entities"
            :initial="{ opacity: 0, y: -8 }"
            :animate="{ opacity: 1, y: 0 }"
            :exit="{ opacity: 0 }"
          >
            <div v-if="extractedEntities.title" class="voice-panel__entity">
              <span class="voice-panel__entity-key">标题</span>
              <span class="voice-panel__entity-val">{{ extractedEntities.title }}</span>
            </div>
            <div v-if="extractedEntities.date" class="voice-panel__entity">
              <span class="voice-panel__entity-key">日期</span>
              <span class="voice-panel__entity-val">{{ extractedEntities.date }}</span>
            </div>
            <div v-if="extractedEntities.startTime" class="voice-panel__entity">
              <span class="voice-panel__entity-key">时间</span>
              <span class="voice-panel__entity-val">{{ extractedEntities.startTime }}–{{ extractedEntities.endTime }}</span>
            </div>
            <div v-if="extractedEntities.location" class="voice-panel__entity">
              <span class="voice-panel__entity-key">地点</span>
              <span class="voice-panel__entity-val">{{ extractedEntities.location }}</span>
            </div>

            <!-- 执行按钮 -->
            <motion.button
              class="voice-panel__execute"
              :style="{ background: selectedModeInfo.gradient }"
              :while-hover="{ scale: 1.02, y: -1 }"
              :while-tap="{ scale: 0.98 }"
              :transition="springPresets.snappy"
              @click="handleExecute"
            >
              确认{{ selectedModeInfo.label }}
            </motion.button>
          </motion.div>
        </AnimatePresence>
      </motion.div>
    </AnimatePresence>
  </div>
</template>

<style scoped lang="scss">
@use '@/styles/variables' as *;

.voice-panel {
  margin-bottom: $space-3;

  &__modes {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 6px;
  }

  &__mode {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 2px;
    padding: $space-2 $space-1;
    border-radius: $radius-md;
    border: 1px solid rgba(255, 255, 255, 0.06);
    background: rgba(255, 255, 255, 0.03);
    color: $color-text-tertiary;
    cursor: pointer;
    transition: all 0.2s ease;
    font-family: $font-family;

    &:hover {
      background: rgba(255, 255, 255, 0.06);
      color: $color-text-secondary;
      border-color: rgba(255, 255, 255, 0.1);
    }

    &--active {
      background: rgba(255, 255, 255, 0.08);
      border-color: rgba(255, 255, 255, 0.15);
      color: $color-text-primary;
      box-shadow: 0 0 12px rgba(255, 255, 255, 0.05);
    }
  }

  &__mode-icon {
    font-size: 16px;
    line-height: 1;
    font-weight: 300;
  }

  &__mode-label {
    font-size: 10px;
    font-weight: $font-weight-medium;
    letter-spacing: 0.02em;
  }

  &__expanded {
    overflow: hidden;
    margin-top: $space-2;
  }

  &__status {
    display: flex;
    align-items: center;
    gap: $space-2;
    padding: $space-2 0;
  }

  &__status-dot {
    width: 6px;
    height: 6px;
    border-radius: 50%;
    animation: pulse-dot 2s infinite;
  }

  &__status-text {
    font-size: 11px;
    color: $color-text-tertiary;
    font-weight: $font-weight-medium;
    letter-spacing: 0.02em;
  }

  &__transcript {
    display: flex;
    align-items: baseline;
    gap: $space-2;
    padding: $space-2;
    background: rgba(255, 255, 255, 0.03);
    border-radius: $radius-sm;
    margin-bottom: $space-2;
  }

  &__transcript-label {
    font-size: 9px;
    color: $color-text-tertiary;
    text-transform: uppercase;
    letter-spacing: 0.1em;
    flex-shrink: 0;
  }

  &__transcript-text {
    font-size: $font-size-sm;
    color: $color-text-primary;
    font-weight: $font-weight-medium;
    line-height: 1.4;
  }

  &__entities {
    display: flex;
    flex-direction: column;
    gap: $space-1;
  }

  &__entity {
    display: flex;
    align-items: center;
    gap: $space-2;
    padding: 3px 0;
  }

  &__entity-key {
    font-size: 9px;
    color: $color-text-tertiary;
    text-transform: uppercase;
    letter-spacing: 0.08em;
    min-width: 28px;
    flex-shrink: 0;
  }

  &__entity-val {
    font-size: 12px;
    color: $color-text-secondary;
    font-weight: $font-weight-medium;
  }

  &__execute {
    width: 100%;
    padding: $space-2;
    border: none;
    border-radius: $radius-md;
    color: white;
    font-size: $font-size-xs;
    font-weight: $font-weight-semibold;
    font-family: $font-family;
    cursor: pointer;
    margin-top: $space-2;
    letter-spacing: 0.02em;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);

    &:hover {
      box-shadow: 0 4px 16px rgba(0, 0, 0, 0.3);
    }
  }
}

@keyframes pulse-dot {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.5; transform: scale(0.8); }
}
</style>
