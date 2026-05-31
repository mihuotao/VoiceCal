<script setup lang="ts">
import { watch, nextTick } from 'vue'
import { motion } from 'motion-v'
import { springPresets } from '@/composables/useMotion'
import VoiceOrbCanvas from '@/components/voice/VoiceOrbCanvas.vue'
import VoiceWaveform from '@/components/voice/VoiceWaveform.vue'
import VoiceResult from '@/components/voice/VoiceResult.vue'
import type { VoiceStatus, ParsedIntent } from '@/types/voice'

const props = withDefaults(defineProps<{
  open: boolean
  status?: VoiceStatus
  amplitude?: number
  transcript?: string
  partialText?: string
  errorMessage?: string
  intent?: ParsedIntent | null
}>(), {
  status: 'idle',
  amplitude: 0,
  transcript: '',
  partialText: '',
  errorMessage: '',
  intent: null
})

const emit = defineEmits<{
  close: []
  confirmCreate: []
  retry: []
  editResult: []
  startRecord: []
  stopRecord: []
  openForm: []
}>()

watch(() => props.open, async (v) => {
  if (v) {
    await nextTick()
    if (props.status === 'listening') {
      emit('startRecord')
    }
  }
})

function handleConfirm() {
  if (props.status === 'recording') {
    emit('stopRecord')
  }
}

function handleClose() {
  if (props.status === 'recording') {
    emit('stopRecord')
  }
  emit('close')
}
</script>

<template>
  <Teleport to="body">
    <motion.div
      v-if="open"
      class="voice-overlay"
      :initial="{ opacity: 0 }"
      :animate="{ opacity: 1 }"
      :exit="{ opacity: 0 }"
      :transition="{ duration: 0.2 }"
    >
      <motion.div
        class="voice-overlay__backdrop"
        :initial="{ opacity: 0 }"
        :animate="{ opacity: 1 }"
        :exit="{ opacity: 0 }"
        :transition="{ duration: 0.25 }"
        @click="handleClose"
      />

      <motion.div
        class="voice-overlay__panel"
        :initial="{ y: '100%', opacity: 0 }"
        :animate="{ y: 0, opacity: 1 }"
        :exit="{ y: '100%', opacity: 0 }"
        :transition="springPresets.slide"
      >
        <div class="voice-overlay__header">
          <button class="voice-overlay__close" @click="handleClose">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
              <line x1="18" y1="6" x2="6" y2="18" />
              <line x1="6" y1="6" x2="18" y2="18" />
            </svg>
          </button>
        </div>

        <div v-if="status === 'result'" class="voice-overlay__content voice-overlay__content--result">
          <VoiceResult
            :transcript="transcript"
            :intent="intent"
            @confirm="emit('confirmCreate')"
            @edit="emit('editResult')"
            @retry="emit('retry')"
            @close="handleClose"
            @open-form="emit('openForm')"
          />
        </div>

        <div v-else class="voice-overlay__content">
          <div class="voice-overlay__orb">
            <VoiceOrbCanvas
              :status="status === 'recording' || status === 'processing' ? 'recording' : 'idle'"
              :amplitude="amplitude"
              :size="140"
            />
          </div>

          <div class="voice-overlay__waveform">
            <VoiceWaveform
              :amplitude="amplitude"
              :is-active="status === 'listening' || status === 'recording' || status === 'processing'"
              :height="60"
            />
          </div>

          <div class="voice-overlay__status">
            <span v-if="status === 'listening'" class="voice-overlay__hint">点击下方按钮开始录音</span>
            <span v-else-if="status === 'recording'" class="voice-overlay__realtime">
              <span class="voice-overlay__dots">
                正在聆听
                <span class="dot">.</span>
                <span class="dot">.</span>
                <span class="dot">.</span>
              </span>
              <span v-if="partialText" class="voice-overlay__partial">{{ partialText }}</span>
              <span v-else class="voice-overlay__partial-hint">请说话...</span>
            </span>
            <span v-else-if="status === 'processing'" class="voice-overlay__realtime">
              <span v-if="partialText" class="voice-overlay__partial">{{ partialText }}</span>
              <span v-else class="voice-overlay__partial-hint">识别中...</span>
            </span>
            <span v-else-if="status === 'error'" class="voice-overlay__error-container">
              <span class="voice-overlay__error-icon">⚠️</span>
              <span class="voice-overlay__error-text">{{ errorMessage || '识别失败，请重试' }}</span>
            </span>
            <span v-if="errorMessage && (status === 'processing' || status === 'recording')" class="voice-overlay__error">
              {{ errorMessage }}
            </span>
          </div>
        </div>

        <div v-if="status !== 'result'" class="voice-overlay__footer">
          <motion.button
            class="voice-overlay__action voice-overlay__action--cancel"
            :while-hover="{ scale: 1.04 }"
            :while-tap="{ scale: 0.97 }"
            :transition="springPresets.snappy"
            @click="handleClose"
          >
            取消
          </motion.button>
          <motion.button
            v-if="status === 'listening'"
            class="voice-overlay__action voice-overlay__action--record"
            :while-hover="{ scale: 1.06 }"
            :while-tap="{ scale: 0.94 }"
            :transition="springPresets.snappy"
            @click="emit('startRecord')"
          >
            <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
              <circle cx="12" cy="12" r="6" />
            </svg>
            开始录音
          </motion.button>
          <motion.button
            v-if="status === 'recording'"
            class="voice-overlay__action voice-overlay__action--done"
            :while-hover="{ scale: 1.04 }"
            :while-tap="{ scale: 0.97 }"
            :transition="springPresets.snappy"
            @click="handleConfirm"
          >
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
              <polyline points="20 6 9 17 4 12" />
            </svg>
            完成
          </motion.button>
          <motion.button
            v-if="status === 'error'"
            class="voice-overlay__action voice-overlay__action--retry"
            :while-hover="{ scale: 1.06 }"
            :while-tap="{ scale: 0.94 }"
            :transition="springPresets.snappy"
            @click="emit('retry')"
          >
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
              <polyline points="23 4 23 10 17 10" />
              <path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10" />
            </svg>
            重试
          </motion.button>
        </div>
      </motion.div>
    </motion.div>
  </Teleport>
</template>

<style scoped lang="scss">
@use '@/styles/variables' as *;

.voice-overlay {
  position: fixed;
  inset: 0;
  z-index: $z-voice-overlay;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
}

.voice-overlay__backdrop {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
}

.voice-overlay__panel {
  position: relative;
  display: flex;
  flex-direction: column;
  background: rgba(255, 255, 255, 0.06);
  backdrop-filter: blur(var(--glass-blur)) saturate(1.3);
  -webkit-backdrop-filter: blur(var(--glass-blur)) saturate(1.3);
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: $radius-xl $radius-xl 0 0;
  padding: $space-6 $space-6 $space-8;
  max-height: 85vh;
  overflow-y: auto;
}

.voice-overlay__header {
  display: flex;
  justify-content: flex-end;
  margin-bottom: $space-4;
}

.voice-overlay__close {
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
  transition: background $transition-base;

  &:hover {
    background: rgba(255, 255, 255, 0.14);
    color: $color-text-primary;
  }
}

.voice-overlay__content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: $space-6;
  flex: 1;

  &--result {
    gap: $space-4;
  }
}

.voice-overlay__orb {
  display: flex;
  align-items: center;
  justify-content: center;
}

.voice-overlay__waveform {
  width: 100%;
  max-width: 320px;
}

.voice-overlay__status {
  font-size: $font-size-lg;
  color: $color-text-secondary;
  text-align: center;
  min-height: 1.5em;
}

.voice-overlay__hint {
  font-size: $font-size-base;
  color: $color-text-tertiary;
}

.voice-overlay__dots {
  .dot {
    animation: dot-blink 1.4s infinite;
    &:nth-child(2) { animation-delay: 0.2s; }
    &:nth-child(3) { animation-delay: 0.4s; }
  }
}

.voice-overlay__processing {
  display: flex;
  align-items: center;
  gap: $space-2;
}

.voice-overlay__realtime {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: $space-2;
}

.voice-overlay__partial {
  font-size: $font-size-xl;
  color: white;
  font-weight: $font-weight-medium;
  max-width: 80%;
  text-align: center;
  word-break: break-all;
}

.voice-overlay__partial-hint {
  font-size: $font-size-base;
  color: $color-text-tertiary;
  opacity: 0.6;
}

.voice-overlay__error {
  font-size: $font-size-sm;
  color: $color-danger;
  text-align: center;
  margin-top: $space-2;
  opacity: 0.9;
}

.voice-overlay__error-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: $space-2;
  padding: $space-4;
}

.voice-overlay__error-icon {
  font-size: 32px;
}

.voice-overlay__error-text {
  font-size: $font-size-base;
  color: $color-danger;
  text-align: center;
}

.voice-overlay__footer {
  display: flex;
  justify-content: center;
  gap: $space-4;
  margin-top: $space-6;
}

.voice-overlay__action {
  display: inline-flex;
  align-items: center;
  gap: $space-2;
  padding: $space-2 $space-6;
  border-radius: $radius-full;
  font-size: $font-size-base;
  font-family: $font-family;
  cursor: pointer;
  font-weight: $font-weight-medium;
  transition: background $transition-base, box-shadow $transition-base;
  border: 1px solid rgba(255, 255, 255, 0.1);
  background: rgba(255, 255, 255, 0.1);
  color: $color-text-primary;

  &:hover {
    background: rgba(255, 255, 255, 0.18);
  }

  &--cancel {
    background: rgba(255, 255, 255, 0.06);
    color: $color-text-secondary;

    &:hover {
      background: rgba(255, 255, 255, 0.12);
    }
  }

  &--record {
    background: rgba($color-primary, 0.2);
    border-color: rgba($color-primary, 0.3);
    color: $color-primary-light;

    &:hover {
      background: rgba($color-primary, 0.3);
      box-shadow: 0 0 16px rgba($color-primary, 0.2);
    }
  }

  &--done {
    background: rgba($color-success, 0.2);
    border-color: rgba($color-success, 0.3);
    color: $color-success;

    &:hover {
      background: rgba($color-success, 0.3);
      box-shadow: 0 0 16px rgba($color-success, 0.2);
    }
  }

  &--retry {
    background: rgba($color-warning, 0.2);
    border-color: rgba($color-warning, 0.3);
    color: $color-warning;

    &:hover {
      background: rgba($color-warning, 0.3);
      box-shadow: 0 0 16px rgba($color-warning, 0.2);
    }
  }
}

@keyframes dot-blink {
  0%, 80%, 100% { opacity: 0; }
  40% { opacity: 1; }
}
</style>
