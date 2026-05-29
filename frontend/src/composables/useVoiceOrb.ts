import { ref, onUnmounted, type Ref } from 'vue'
import { noise3D } from '@/utils/noise'

export type VoiceOrbStatus = 'idle' | 'recording' | 'processing'

interface Particle {
  x: number
  y: number
  vx: number
  vy: number
  life: number
  maxLife: number
  size: number
  alpha: number
  angle: number
  radius: number
  speed: number
}

interface OrbOptions {
  size?: number
  primaryColor?: string
  glowColor?: string
  particleCount?: number
}

const DEFAULT_COLORS = {
  idle: { primary: '99, 102, 241', glow: '129, 140, 248' },
  recording: { primary: '56, 189, 248', glow: '125, 211, 252' },
  processing: { primary: '232, 121, 249', glow: '216, 180, 254' }
}

export function useVoiceOrb(
  canvasRef: Ref<HTMLCanvasElement | null>,
  options: OrbOptions = {}
) {
  const status = ref<VoiceOrbStatus>('idle')
  const amplitude = ref(0)
  const orbSize = ref(options.size ?? 120)

  let ctx: CanvasRenderingContext2D | null = null
  let animId = 0
  let time = 0
  let particles: Particle[] = []
  let ringAngle = 0
  let dpr = 1

  const colorSet = DEFAULT_COLORS

  function initCanvas() {
    const canvas = canvasRef.value
    if (!canvas) return
    dpr = window.devicePixelRatio || 1
    ctx = canvas.getContext('2d')!
    resizeCanvas()
  }

  function resizeCanvas() {
    const canvas = canvasRef.value
    if (!canvas || !ctx) return
    const size = orbSize.value * 2 + 80
    canvas.width = size * dpr
    canvas.height = size * dpr
    canvas.style.width = `${size}px`
    canvas.style.height = `${size}px`
    ctx.scale(dpr, dpr)
  }

  function initParticles() {
    const count = options.particleCount ?? (status.value === 'recording' ? 60 : status.value === 'processing' ? 40 : 25)
    particles = Array.from({ length: count }, () => createParticle())
  }

  function createParticle(): Particle {
    const r = orbSize.value * (0.5 + Math.random() * 0.8)
    const a = Math.random() * Math.PI * 2
    return {
      x: Math.cos(a) * r * 0.3,
      y: Math.sin(a) * r * 0.3,
      vx: 0,
      vy: 0,
      life: Math.random(),
      maxLife: 1 + Math.random() * 2,
      size: 1 + Math.random() * 2.5,
      alpha: 0.2 + Math.random() * 0.5,
      angle: a,
      radius: r,
      speed: 0.2 + Math.random() * 0.4
    }
  }

  function updateParticles(dt: number) {
    const s = status.value

    for (const p of particles) {
      p.life += dt * 0.5

      if (p.life > p.maxLife) {
        Object.assign(p, createParticle())
        continue
      }

      const lifeRatio = p.life / p.maxLife

      if (s === 'idle') {
        p.angle += dt * p.speed * 0.3
        p.radius += Math.sin(time + p.angle) * 0.1
        p.alpha = Math.sin(lifeRatio * Math.PI) * 0.4
      } else if (s === 'recording') {
        p.angle += dt * p.speed * (1 + amplitude.value * 2)
        p.radius = orbSize.value * (0.3 + lifeRatio * 0.7)
        p.alpha = Math.sin(lifeRatio * Math.PI) * 0.7
        p.size = 1 + (1 - lifeRatio) * 3
      } else {
        p.angle += dt * p.speed * 0.8
        p.radius = orbSize.value * (0.4 + Math.sin(lifeRatio * Math.PI) * 0.4)
        p.alpha = Math.sin(lifeRatio * Math.PI) * 0.6
        p.size = 1.5 + Math.sin(p.angle * 3 + time) * 1
      }

      p.x = Math.cos(p.angle) * p.radius
      p.y = Math.sin(p.angle) * p.radius
    }
  }

  function drawOrb() {
    if (!ctx) return
    const center = orbSize.value + 40
    const radius = orbSize.value * 0.5
    const s = status.value
    const colors = colorSet[s]

    const noiseAmp = s === 'idle' ? 0.04 : s === 'recording' ? 0.08 + amplitude.value * 0.06 : 0.05
    const pulseScale = s === 'idle'
      ? 1 + Math.sin(time * 1.5) * 0.015
      : 1 + Math.sin(time * 3) * 0.025 + amplitude.value * 0.03
    const baseRadius = radius * pulseScale

    ctx.save()

    const glowRadius = radius * (2.2 + Math.sin(time * 0.5) * 0.2)
    const glow = ctx.createRadialGradient(center, center, 0, center, center, glowRadius)
    glow.addColorStop(0, `rgba(${colors.glow}, ${0.15 + (s === 'recording' ? 0.15 : 0)})`)
    glow.addColorStop(0.5, `rgba(${colors.primary}, ${0.08})`)
    glow.addColorStop(1, `rgba(${colors.primary}, 0)`)
    ctx.fillStyle = glow
    ctx.beginPath()
    ctx.arc(center, center, glowRadius, 0, Math.PI * 2)
    ctx.fill()

    ctx.beginPath()
    const steps = 64
    for (let i = 0; i <= steps; i++) {
      const a = (i / steps) * Math.PI * 2
      const nx = Math.cos(a) * 0.5 + 0.5
      const ny = Math.sin(a) * 0.5 + 0.5
      const n = noise3D(nx * 3 + time * 0.3, ny * 3 + time * 0.2, time * 0.15)
      const r = baseRadius + n * baseRadius * noiseAmp
      const x = center + Math.cos(a) * r
      const y = center + Math.sin(a) * r
      if (i === 0) ctx.moveTo(x, y)
      else ctx.lineTo(x, y)
    }
    ctx.closePath()

    const orbGrad = ctx.createRadialGradient(
      center - radius * 0.3, center - radius * 0.3, 0,
      center, center, baseRadius
    )
    orbGrad.addColorStop(0, `rgba(${colors.glow}, 0.9)`)
    orbGrad.addColorStop(0.4, `rgba(${colors.primary}, 0.7)`)
    orbGrad.addColorStop(0.8, `rgba(${colors.primary}, 0.3)`)
    orbGrad.addColorStop(1, `rgba(${colors.primary}, 0.1)`)
    ctx.fillStyle = orbGrad
    ctx.fill()

    const innerGlow = ctx.createRadialGradient(
      center - radius * 0.2, center - radius * 0.2, 0,
      center, center, baseRadius * 0.6
    )
    innerGlow.addColorStop(0, `rgba(255, 255, 255, ${0.4 + amplitude.value * 0.2})`)
    innerGlow.addColorStop(1, `rgba(255, 255, 255, 0)`)
    ctx.fillStyle = innerGlow
    ctx.beginPath()
    ctx.arc(center, center, baseRadius * 0.6, 0, Math.PI * 2)
    ctx.fill()

    ctx.restore()
  }

  function drawParticles() {
    if (!ctx) return
    const center = orbSize.value + 40
    const colors = colorSet[status.value]

    for (const p of particles) {
      ctx!.save()
      ctx!.globalAlpha = p.alpha
      ctx!.fillStyle = `rgba(${colors.glow}, ${p.alpha})`
      ctx!.beginPath()
      ctx!.arc(center + p.x, center + p.y, p.size, 0, Math.PI * 2)
      ctx!.fill()
      ctx!.restore()
    }
  }

  function drawRing() {
    if (!ctx || status.value !== 'processing') return
    const center = orbSize.value + 40
    const radius = orbSize.value * 0.65
    const colors = colorSet.processing

    ringAngle += 0.02

    ctx.save()
    ctx.globalAlpha = 0.6 + Math.sin(time * 2) * 0.2

    for (let i = 0; i < 2; i++) {
      const offset = i * Math.PI
      ctx.beginPath()
      ctx.arc(center, center, radius, ringAngle + offset, ringAngle + offset + Math.PI * 0.6)
      ctx.strokeStyle = `rgba(${colors.glow}, ${0.5 + Math.sin(time + i) * 0.2})`
      ctx.lineWidth = 2.5
      ctx.shadowColor = `rgba(${colors.glow}, 0.5)`
      ctx.shadowBlur = 10
      ctx.stroke()
    }

    ctx.restore()
  }

  function drawRecordingIndicators() {
    if (!ctx || status.value !== 'recording') return
    const center = orbSize.value + 40
    const radius = orbSize.value * 0.55
    const colors = colorSet.recording

    for (let i = 0; i < 4; i++) {
      const a = (i / 4) * Math.PI * 2 + time * 0.5
      const r = radius * (1 + Math.sin(time * 2 + i) * 0.1 * amplitude.value)
      const x = center + Math.cos(a) * r
      const y = center + Math.sin(a) * r

      ctx!.save()
      const s = 3 + Math.sin(time * 3 + i * 2) * 1.5
      ctx!.fillStyle = `rgba(${colors.glow}, ${0.3 + amplitude.value * 0.3})`
      ctx!.beginPath()
      ctx!.arc(x, y, s, 0, Math.PI * 2)
      ctx!.fill()
      ctx!.restore()
    }
  }

  function render(timestamp: number) {
    const dt = Math.min(0.05, (timestamp - (animId || timestamp)) / 1000)
    time += dt

    if (!ctx || !canvasRef.value) {
      animId = requestAnimationFrame(render)
      return
    }

    ctx.clearRect(0, 0, canvasRef.value.width, canvasRef.value.height)

    updateParticles(dt)
    drawOrb()
    drawParticles()
    drawRing()
    drawRecordingIndicators()

    animId = requestAnimationFrame(render)
  }

  function start() {
    if (!canvasRef.value) return
    initCanvas()
    initParticles()
    animId = requestAnimationFrame(render)
  }

  function stop() {
    cancelAnimationFrame(animId)
    animId = 0
  }

  function setStatus(s: VoiceOrbStatus) {
    status.value = s
    initParticles()
  }

  function setAmplitude(val: number) {
    amplitude.value = Math.max(0, Math.min(1, val))
  }

  function setSize(size: number) {
    orbSize.value = size
    resizeCanvas()
  }

  onUnmounted(stop)

  return {
    status,
    amplitude,
    orbSize,
    start,
    stop,
    setStatus,
    setAmplitude,
    setSize
  }
}
