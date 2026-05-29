<script setup lang="ts">
import { motion } from 'motion-v'
import { springPresets } from '@/composables/useMotion'
import VoiceOrbCanvas from '@/components/voice/VoiceOrbCanvas.vue'
import VoiceWaveform from '@/components/voice/VoiceWaveform.vue'
import type { VoiceStatus } from '@/composables/useVoice'

withDefaults(defineProps<{
  open: boolean
  status?: VoiceStatus
  amplitude?: number
}>(), {
  status: 'idle',
  amplitude: 0
})

const emit = defineEmits<{
  close: []
  confirm: []
}>()

const statusText: Record<VoiceStatus, string> = {
  idle: '',
  listening: '正在聆听...',
  processing: '处理中...',
  result: '识别结果'
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
        @click="emit('close')"
      />

      <motion.div
        class="voice-overlay__panel"
        :initial="{ y: '100%', opacity: 0 }"
        :animate="{ y: 0, opacity: 1 }"
        :exit="{ y: '100%', opacity: 0 }"
        :transition="springPresets.slide"
      >
        <div class="voice-overlay__header">
          <button class="voice-overlay__close" @click="emit('close')">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
              <line x1="18" y1="6" x2="6" y2="18" />
              <line x1="6" y1="6" x2="18" y2="18" />
            </svg>
          </button>
        </div>

        <div class="voice-overlay__content">
          <div class="voice-overlay__orb">
            <VoiceOrbCanvas
              :status="status === 'listening' ? 'recording' : status === 'processing' ? 'processing' : 'idle'"
              :amplitude="amplitude"
              :size="140"
            />
          </div>

          <div class="voice-overlay__waveform">
            <VoiceWaveform
              :amplitude="amplitude"
              :is-active="status === 'listening'"
              :height="60"
            />
          </div>

          <div class="voice-overlay__status">
            <span v-if="status === 'listening'" class="voice-overlay__dots">
              正在聆听
              <span class="dot">.</span>
              <span class="dot">.</span>
              <span class="dot">.</span>
            </span>
            <span v-else-if="status === 'processing'" class="voice-overlay__processing">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="voice-spinner">
                <circle cx="12" cy="12" r="10" stroke-dasharray="31.4" stroke-dashoffset="10" />
              </svg>
              处理中
            </span>
            <span v-else>{{ statusText[status] }}</span>
          </div>
        </div>

        <div class="voice-overlay__footer">
          <motion.button
            class="voice-overlay__action voice-overlay__action--cancel"
            :while-hover="{ scale: 1.04 }"
            :while-tap="{ scale: 0.97 }"
            :transition="springPresets.snappy"
            @click="emit('close')"
          >
            取消
          </motion.button>
          <motion.button
            v-if="status === 'listening'"
            class="voice-overlay__action voice-overlay__action--done"
            :while-hover="{ scale: 1.04 }"
            :while-tap="{ scale: 0.97 }"
            :transition="springPresets.snappy"
            @click="emit('confirm')"
          >
            完成
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
  backdrop-filter: blur(32px) saturate(1.3);
  -webkit-backdrop-filter: blur(32px) saturate(1.3);
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

.voice-spinner {
  animation: rotate-slow 0.8s linear infinite;
}

.voice-overlay__footer {
  display: flex;
  justify-content: center;
  gap: $space-4;
  margin-top: $space-6;
}

.voice-overlay__action {
  padding: $space-2 $space-8;
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

  &--done {
    background: rgba($color-primary, 0.25);
    border-color: rgba($color-primary, 0.3);
    color: white;

    &:hover {
      background: rgba($color-primary, 0.35);
      box-shadow: 0 0 16px rgba($color-primary, 0.25);
    }
  }
}

@keyframes dot-blink {
  0%, 80%, 100% { opacity: 0; }
  40% { opacity: 1; }
}
</style>
