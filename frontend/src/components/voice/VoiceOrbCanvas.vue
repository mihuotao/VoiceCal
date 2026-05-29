<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useVoiceOrb, type VoiceOrbStatus } from '@/composables/useVoiceOrb'

const props = withDefaults(defineProps<{
  status?: VoiceOrbStatus
  size?: number
  amplitude?: number
  primaryColor?: string
  glowColor?: string
}>(), {
  status: 'idle',
  size: 120,
  amplitude: 0
})

const emit = defineEmits<{
  click: [e: MouseEvent]
}>()

const canvasRef = ref<HTMLCanvasElement | null>(null)
const orb = useVoiceOrb(canvasRef, {
  size: props.size,
  primaryColor: props.primaryColor,
  glowColor: props.glowColor
})

defineExpose({ orb })

onMounted(() => {
  orb.start()
})
</script>

<template>
  <div
    class="voice-orb-wrapper"
    :style="{ width: `${size * 2 + 80}px`, height: `${size * 2 + 80}px` }"
    @click="emit('click', $event)"
  >
    <canvas
      ref="canvasRef"
      class="voice-orb-canvas"
    />
  </div>
</template>

<style scoped lang="scss">
.voice-orb-wrapper {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  -webkit-tap-highlight-color: transparent;
}

.voice-orb-canvas {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  pointer-events: none;
}
</style>
