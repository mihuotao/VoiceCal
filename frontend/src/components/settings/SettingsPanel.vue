<script setup lang="ts">
import { ref, watch } from 'vue'
import { motion, AnimatePresence } from 'motion-v'
import { springPresets } from '@/composables/useMotion'
import { usePreferences } from '@/composables/usePreferences'
import { useThemeStore } from '@/stores/theme'

const props = withDefaults(defineProps<{
  open: boolean
}>(), {
  open: false
})

const emit = defineEmits<{
  close: []
}>()

const theme = useThemeStore()
const { prefs, updatePreferences } = usePreferences()

const localBlur = ref(theme.state.glassBlur)
const localOpacity = ref(theme.state.glassOpacity * 100)

watch(() => props.open, (v) => {
  if (v) {
    localBlur.value = theme.state.glassBlur
    localOpacity.value = theme.state.glassOpacity * 100
  }
})

async function handleBlurChange(e: Event) {
  const val = parseInt((e.target as HTMLInputElement).value)
  localBlur.value = val
  await updatePreferences({ glassBlur: val })
}

async function handleOpacityChange(e: Event) {
  const val = parseInt((e.target as HTMLInputElement).value)
  localOpacity.value = val
  await updatePreferences({ glassOpacity: val / 100 })
}

async function handleTtsToggle() {
  await updatePreferences({ ttsEnabled: !prefs.value.ttsEnabled })
}

async function handleNotifToggle() {
  await updatePreferences({ notificationEnabled: !prefs.value.notificationEnabled })
}

async function handleWeekStartChange(e: Event) {
  await updatePreferences({ weekStartDay: parseInt((e.target as HTMLSelectElement).value) })
}

const colorPresets = [
  '#6366f1', '#8b5cf6', '#ec4899', '#f43f5e',
  '#f97316', '#22c55e', '#14b8a6', '#06b6d4'
]

async function handleColorPick(color: string) {
  await updatePreferences({ primaryColor: color })
}
</script>

<template>
  <AnimatePresence>
    <div v-if="open" class="settings-backdrop" @click.self="emit('close')">
      <motion.div
        class="settings-panel"
        :initial="{ x: '100%' }"
        :animate="{ x: 0 }"
        :exit="{ x: '100%' }"
        :transition="springPresets.slide"
        @click.stop
      >
        <div class="settings-header">
          <h2 class="settings-title">设置</h2>
          <button class="settings-close" @click="emit('close')">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M18 6L6 18M6 6l12 12" />
            </svg>
          </button>
        </div>

        <div class="settings-body">
          <section class="settings-section">
            <h3 class="settings-section-title">外观</h3>

            <div class="settings-row">
              <label class="settings-label">玻璃模糊</label>
              <div class="settings-slider-row">
                <input
                  type="range"
                  min="8"
                  max="64"
                  step="2"
                  class="settings-slider"
                  :value="localBlur"
                  @input="handleBlurChange"
                />
                <span class="settings-value">{{ localBlur }}px</span>
              </div>
            </div>

            <div class="settings-row">
              <label class="settings-label">玻璃透明度</label>
              <div class="settings-slider-row">
                <input
                  type="range"
                  min="10"
                  max="100"
                  step="5"
                  class="settings-slider"
                  :value="localOpacity"
                  @input="handleOpacityChange"
                />
                <span class="settings-value">{{ localOpacity }}%</span>
              </div>
            </div>

            <div class="settings-row">
              <label class="settings-label">主题色</label>
              <div class="settings-colors">
                <button
                  v-for="c in colorPresets"
                  :key="c"
                  class="settings-color-swatch"
                  :class="{ 'settings-color-swatch--active': theme.state.primaryColor === c }"
                  :style="{ background: c }"
                  :title="c"
                  @click="handleColorPick(c)"
                />
              </div>
            </div>
          </section>

          <section class="settings-section">
            <h3 class="settings-section-title">日历</h3>

            <div class="settings-row">
              <label class="settings-label">每周起始日</label>
              <select
                class="settings-select"
                :value="prefs.weekStartDay"
                @change="handleWeekStartChange"
              >
                <option :value="0">周日</option>
                <option :value="1">周一</option>
              </select>
            </div>
          </section>

          <section class="settings-section">
            <h3 class="settings-section-title">语音</h3>

            <div class="settings-row">
              <label class="settings-label">语音播报</label>
              <button
                class="settings-toggle"
                :class="{ 'settings-toggle--on': prefs.ttsEnabled }"
                @click="handleTtsToggle"
              >
                <span class="settings-toggle-knob" />
              </button>
            </div>
          </section>

          <section class="settings-section">
            <h3 class="settings-section-title">通知</h3>

            <div class="settings-row">
              <label class="settings-label">事件通知</label>
              <button
                class="settings-toggle"
                :class="{ 'settings-toggle--on': prefs.notificationEnabled }"
                @click="handleNotifToggle"
              >
                <span class="settings-toggle-knob" />
              </button>
            </div>
          </section>
        </div>
      </motion.div>
    </div>
  </AnimatePresence>
</template>

<style scoped lang="scss">
@use '@/styles/variables' as *;

.settings-backdrop {
  position: fixed;
  inset: 0;
  z-index: $z-modal;
  background: rgba(0, 0, 0, 0.3);
  display: flex;
  justify-content: flex-end;
}

.settings-panel {
  width: 340px;
  height: 100%;
  background: rgba(20, 20, 30, 0.92);
  backdrop-filter: blur(24px);
  -webkit-backdrop-filter: blur(24px);
  border-left: 1px solid rgba(255, 255, 255, 0.06);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.settings-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: $space-6;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
}

.settings-title {
  font-size: $font-size-lg;
  font-weight: $font-weight-semibold;
  color: white;
  margin: 0;
}

.settings-close {
  background: none;
  border: none;
  color: $color-text-tertiary;
  cursor: pointer;
  padding: $space-1;
  border-radius: $radius-md;
  display: flex;

  &:hover {
    color: white;
    background: rgba(255, 255, 255, 0.08);
  }
}

.settings-body {
  flex: 1;
  overflow-y: auto;
  padding: $space-4 0;
}

.settings-section {
  padding: 0 $space-6;
  margin-bottom: $space-6;
}

.settings-section-title {
  font-size: $font-size-xs;
  font-weight: $font-weight-semibold;
  color: $color-text-tertiary;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  margin: 0 0 $space-3;
}

.settings-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: $space-3 0;
}

.settings-label {
  font-size: $font-size-sm;
  color: $color-text-primary;
}

.settings-slider-row {
  display: flex;
  align-items: center;
  gap: $space-3;
}

.settings-slider {
  width: 100px;
  height: 4px;
  -webkit-appearance: none;
  appearance: none;
  background: rgba(255, 255, 255, 0.12);
  border-radius: 2px;
  outline: none;
  cursor: pointer;

  &::-webkit-slider-thumb {
    -webkit-appearance: none;
    width: 16px;
    height: 16px;
    border-radius: 50%;
    background: $color-primary;
    border: 2px solid rgba(255, 255, 255, 0.3);
    cursor: pointer;
  }
}

.settings-value {
  font-size: $font-size-sm;
  color: $color-text-secondary;
  min-width: 36px;
  text-align: right;
}

.settings-colors {
  display: flex;
  gap: $space-2;
}

.settings-color-swatch {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  border: 2px solid transparent;
  cursor: pointer;
  padding: 0;
  outline: none;
  transition: transform $transition-base, border-color $transition-base;

  &:hover {
    transform: scale(1.15);
  }

  &--active {
    border-color: white;
    box-shadow: 0 0 0 2px rgba(255, 255, 255, 0.3);
  }
}

.settings-select {
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: $radius-md;
  color: white;
  font-size: $font-size-sm;
  font-family: $font-family;
  padding: $space-1 $space-3;
  outline: none;
  cursor: pointer;

  &:focus {
    border-color: rgba($color-primary, 0.4);
  }
}

.settings-toggle {
  width: 44px;
  height: 24px;
  border-radius: 12px;
  border: none;
  background: rgba(255, 255, 255, 0.15);
  cursor: pointer;
  position: relative;
  transition: background $transition-base;
  padding: 0;
  flex-shrink: 0;

  &--on {
    background: $color-primary;
  }

  &-knob {
    position: absolute;
    top: 2px;
    left: 2px;
    width: 20px;
    height: 20px;
    border-radius: 50%;
    background: white;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.2);
    transition: transform $transition-base;

    .settings-toggle--on & {
      transform: translateX(20px);
    }
  }
}
</style>
