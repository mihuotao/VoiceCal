<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'

const props = withDefaults(defineProps<{
  amplitude?: number
  color?: string
  height?: number
  barCount?: number
  isActive?: boolean
}>(), {
  amplitude: 0,
  color: '129, 140, 248',
  height: 80,
  barCount: 48,
  isActive: false
})

const canvasRef = ref<HTMLCanvasElement | null>(null)
let ctx: CanvasRenderingContext2D | null = null
let animId = 0
let time = 0
let dpr = 1

function init() {
  const canvas = canvasRef.value
  if (!canvas) return
  dpr = window.devicePixelRatio || 1
  ctx = canvas.getContext('2d')!
  resize()
}

function resize() {
  const canvas = canvasRef.value
  if (!canvas || !ctx) return
  const w = canvas.parentElement?.clientWidth ?? window.innerWidth
  canvas.width = w * dpr
  canvas.height = props.height * dpr
  canvas.style.width = `${w}px`
  canvas.style.height = `${props.height}px`
  ctx.scale(dpr, dpr)
}

function render(timestamp: number) {
  const dt = Math.min(0.05, (timestamp - (animId || timestamp)) / 1000)
  time += dt

  if (!ctx || !canvasRef.value) {
    animId = requestAnimationFrame(render)
    return
  }

  const w = canvasRef.value.width / dpr
  const h = canvasRef.value.height / dpr
  ctx.clearRect(0, 0, w, h)

  const midY = h / 2
  const barW = w / props.barCount
  const amp = props.isActive ? props.amplitude : 0.15

  for (let i = 0; i < props.barCount; i++) {
    const phase = (i / props.barCount) * Math.PI * 2
    const n1 = Math.sin(phase * 3 + time * 2) * 0.5 + 0.5
    const n2 = Math.sin(phase * 5 + time * 1.5) * 0.3
    const n3 = Math.sin(phase * 7 + time * 3) * 0.2
    const envelope = Math.sin((i / props.barCount) * Math.PI)
    const barH = (n1 + n2 + n3) * amp * midY * 0.8 * envelope

    const x = i * barW + barW * 0.1
    const bw = Math.max(1, barW * 0.6)

    const alpha = 0.2 + envelope * 0.6 * amp
    ctx.fillStyle = `rgba(${props.color}, ${alpha})`
    ctx.beginPath()
    ctx.roundRect(x, midY - barH, bw, barH * 2, bw / 2)
    ctx.fill()
  }

  animId = requestAnimationFrame(render)
}

let resizeObserver: ResizeObserver | null = null

onMounted(() => {
  init()
  animId = requestAnimationFrame(render)
  resizeObserver = new ResizeObserver(() => resize())
  if (canvasRef.value?.parentElement) {
    resizeObserver.observe(canvasRef.value.parentElement)
  }
})

onUnmounted(() => {
  cancelAnimationFrame(animId)
  resizeObserver?.disconnect()
})
</script>

<template>
  <canvas ref="canvasRef" class="voice-waveform" />
</template>

<style scoped lang="scss">
.voice-waveform {
  display: block;
  width: 100%;
}
</style>
