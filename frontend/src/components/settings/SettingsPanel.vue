<script setup lang="ts">
import { ref, watch } from 'vue'
import { motion, AnimatePresence } from 'motion-v'
import { usePreferences } from '@/composables/usePreferences'
import { useThemeStore } from '@/stores/theme'
import { useCustomBg } from '@/composables/useCustomBg'

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
const { customBg, setCustomBg, clearCustomBg } = useCustomBg()

const localBlur = ref(theme.state.glassBlur)
const localOpacity = ref(theme.state.glassOpacity * 100)

const INIT_W = 380
const MIN_W = 300
const rightAnchor = ref(20)
const panelW = ref(INIT_W)
const panelH = ref(0)
const panelY = ref(60)
const isResizing = ref(false)
const isReady = ref(false)
let resizeStartX = 0
let resizeStartW = 0

function initPanel() {
  const vh = window.innerHeight
  rightAnchor.value = 20
  panelW.value = INIT_W
  panelY.value = 60
  panelH.value = vh - 80
  isReady.value = true
}

function getMaxW() {
  const vw = window.innerWidth
  const calendarRightEdge = vw * 0.6
  return Math.max(MIN_W, vw - calendarRightEdge - rightAnchor.value)
}

function onResizeStart(e: MouseEvent) {
  isResizing.value = true
  resizeStartX = e.clientX
  resizeStartW = panelW.value
  e.preventDefault()
}

function onMouseMove(e: MouseEvent) {
  if (!isResizing.value) return
  const delta = resizeStartX - e.clientX
  const newW = resizeStartW + delta
  const maxW = getMaxW()
  panelW.value = Math.max(MIN_W, Math.min(maxW, newW))
}

function onMouseUp() {
  isResizing.value = false
}

watch(() => props.open, (v) => {
  if (v) {
    isReady.value = false
    panelW.value = INIT_W
    initPanel()
    localBlur.value = theme.state.glassBlur
    localOpacity.value = theme.state.glassOpacity * 100
  }
})

function panelStyle() {
  return {
    right: rightAnchor.value + 'px',
    top: panelY.value + 'px',
    width: panelW.value + 'px',
    height: panelH.value + 'px'
  }
}

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

function handleBgUpload(e: Event) {
  const file = (e.target as HTMLInputElement).files?.[0]
  if (!file) return
  const reader = new FileReader()
  reader.onload = () => {
    if (typeof reader.result === 'string') {
      setCustomBg(reader.result)
    }
  }
  reader.readAsDataURL(file)
}

function handleResetBg() {
  clearCustomBg()
}
</script>

<template>
  <AnimatePresence>
    <motion.div
      v-if="open"
      class="settings-overlay"
      :initial="{ opacity: 0 }"
      :animate="{ opacity: 1 }"
      :exit="{ opacity: 0 }"
      :transition="{ duration: 0.15 }"
      @mousemove="onMouseMove"
      @mouseup="onMouseUp"
    >
      <div
        class="settings-panel"
        :class="{
          'settings-panel--resizing': isResizing,
          'settings-panel--ready': isReady
        }"
        :style="panelStyle()"
        @click.stop
      >
        <div
          class="settings-resize-border"
          :class="{ 'settings-resize-border--active': isResizing }"
          @mousedown="onResizeStart"
        >
          <div class="settings-resize-dots">
            <span /><span /><span />
          </div>
        </div>

        <div class="settings-content">
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
                  min="0"
                  max="128"
                  step="4"
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
                  min="5"
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
                <label class="settings-label">自定义背景</label>
                <div class="settings-bg-actions">
                  <label class="settings-upload-btn">
                    <input type="file" accept="image/*" class="settings-upload-input" @change="handleBgUpload" />
                    上传图片
                  </label>
                  <button
                    v-if="customBg"
                    class="settings-reset-btn"
                    @click="handleResetBg"
                  >
                    恢复默认
                  </button>
                </div>
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
        </div>
      </div>
    </motion.div>
  </AnimatePresence>
</template>

<style scoped lang="scss">
@use '@/styles/variables' as *;

.settings-overlay {
  position: fixed;
  inset: 0;
  z-index: $z-modal;
  background: rgba(0, 0, 0, 0.15);
}

.settings-panel {
  position: absolute;
  display: flex;
  flex-direction: row;
  border-radius: $radius-xl;
  overflow: hidden;
  box-shadow: $shadow-lg;
  transition: width 0.05s linear;

  &--ready {
    transition: width 0.05s linear;
  }

  &--resizing {
    box-shadow: 0 20px 60px rgba(0, 0, 0, 0.45);
    user-select: none;
    transition: none;
  }
}

.settings-resize-border {
  width: 16px;
  flex-shrink: 0;
  background: rgba(255, 255, 255, 0.02);
  border-right: 1px solid rgba(255, 255, 255, 0.06);
  cursor: ew-resize;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background $transition-fast, border-color $transition-fast;

  &:hover {
    background: rgba($color-primary, 0.1);
    border-color: rgba($color-primary, 0.3);
  }

  &--active {
    background: rgba($color-primary, 0.18);
    border-color: rgba($color-primary, 0.5);
  }
}

.settings-resize-dots {
  display: flex;
  flex-direction: column;
  gap: 5px;

  span {
    width: 4px;
    height: 4px;
    border-radius: 50%;
    background: rgba(255, 255, 255, 0.2);
    transition: background $transition-fast;

    .settings-resize-border:hover &,
    .settings-resize-border--active & {
      background: rgba($color-primary-light, 0.7);
    }
  }
}

.settings-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: rgba(20, 20, 30, 0.95);
  backdrop-filter: blur(var(--glass-blur));
  -webkit-backdrop-filter: blur(var(--glass-blur));
  min-width: 0;
  overflow: hidden;
}

.settings-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: $space-5 $space-6;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
  flex-shrink: 0;
}

.settings-title {
  font-size: $font-size-xl;
  font-weight: $font-weight-semibold;
  color: white;
  margin: 0;
  white-space: nowrap;
}

.settings-close {
  background: none;
  border: none;
  color: $color-text-tertiary;
  cursor: pointer;
  padding: $space-1;
  border-radius: $radius-md;
  display: flex;
  flex-shrink: 0;

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
  font-size: $font-size-sm;
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
  gap: $space-4;
}

.settings-label {
  font-size: $font-size-base;
  color: $color-text-primary;
  flex-shrink: 0;
}

.settings-slider-row {
  display: flex;
  align-items: center;
  gap: $space-3;
  flex: 1;
  justify-content: flex-end;
}

.settings-slider {
  width: 100%;
  max-width: 160px;
  min-width: 60px;
  height: 4px;
  -webkit-appearance: none;
  appearance: none;
  background: rgba(255, 255, 255, 0.12);
  border-radius: 2px;
  outline: none;
  cursor: pointer;

  &::-webkit-slider-thumb {
    -webkit-appearance: none;
    width: 18px;
    height: 18px;
    border-radius: 50%;
    background: $color-primary;
    border: 2px solid rgba(255, 255, 255, 0.3);
    cursor: pointer;
  }
}

.settings-value {
  font-size: $font-size-sm;
  color: $color-text-secondary;
  min-width: 44px;
  text-align: right;
  flex-shrink: 0;
}

.settings-select {
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: $radius-md;
  color: white;
  font-size: $font-size-base;
  font-family: $font-family;
  padding: $space-2 $space-3;
  outline: none;
  cursor: pointer;

  &:focus {
    border-color: rgba($color-primary, 0.4);
  }
}

.settings-bg-actions {
  display: flex;
  gap: $space-2;
  align-items: center;
}

.settings-upload-btn {
  display: inline-flex;
  align-items: center;
  padding: $space-1 $space-3;
  border-radius: $radius-md;
  background: rgba($color-primary, 0.2);
  border: 1px solid rgba($color-primary, 0.3);
  color: $color-primary-light;
  font-size: $font-size-sm;
  font-family: $font-family;
  cursor: pointer;
  transition: background $transition-fast;

  &:hover {
    background: rgba($color-primary, 0.3);
  }
}

.settings-upload-input {
  display: none;
}

.settings-reset-btn {
  padding: $space-1 $space-3;
  border-radius: $radius-md;
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.08);
  color: $color-text-secondary;
  font-size: $font-size-sm;
  font-family: $font-family;
  cursor: pointer;
  transition: background $transition-fast;

  &:hover {
    background: rgba(255, 255, 255, 0.12);
    color: $color-text-primary;
  }
}

.settings-toggle {
  width: 48px;
  height: 26px;
  border-radius: 13px;
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
    top: 3px;
    left: 3px;
    width: 20px;
    height: 20px;
    border-radius: 50%;
    background: white;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.2);
    transition: transform $transition-base;

    .settings-toggle--on & {
      transform: translateX(22px);
    }
  }
}
</style>
