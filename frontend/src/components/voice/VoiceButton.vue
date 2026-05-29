<script setup lang="ts">
import { motion } from 'motion-v'
import { springPresets } from '@/composables/useMotion'
import VoiceOrbCanvas from '@/components/voice/VoiceOrbCanvas.vue'
import type { VoiceOrbStatus } from '@/composables/useVoiceOrb'

withDefaults(defineProps<{
  status?: VoiceOrbStatus
  amplitude?: number
  size?: number
}>(), {
  status: 'idle',
  amplitude: 0,
  size: 80
})

const emit = defineEmits<{
  click: [e: MouseEvent]
}>()
</script>

<template>
  <motion.div
    class="voice-button"
    :while-hover="{ scale: 1.06 }"
    :while-tap="{ scale: 0.94 }"
    :transition="springPresets.snappy"
    @click="emit('click', $event)"
  >
    <div class="voice-button__ring" />
    <div class="voice-button__orb">
      <VoiceOrbCanvas
        :status="status"
        :size="size"
        :amplitude="amplitude"
      />
    </div>
    <div class="voice-button__glow" />
  </motion.div>
</template>

<style scoped lang="scss">
@use '@/styles/variables' as *;

.voice-button {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 88px;
  height: 88px;
  border-radius: 50%;
  cursor: pointer;
  -webkit-tap-highlight-color: transparent;
  outline: none;
}

.voice-button__ring {
  position: absolute;
  inset: -4px;
  border-radius: 50%;
  border: 1.5px solid rgba(255, 255, 255, 0.1);
  background: rgba(255, 255, 255, 0.04);
  backdrop-filter: blur(4px);
  pointer-events: none;
}

.voice-button__orb {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.voice-button__glow {
  position: absolute;
  inset: -12px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(99, 102, 241, 0.15), transparent 70%);
  pointer-events: none;
  animation: button-glow-pulse 3s ease-in-out infinite;
}

@keyframes button-glow-pulse {
  0%, 100% { opacity: 0.5; transform: scale(1); }
  50% { opacity: 1; transform: scale(1.08); }
}
</style>
