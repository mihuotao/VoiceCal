<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useCustomBg } from '@/composables/useCustomBg'

const { customBg } = useCustomBg()

const hasCustomBg = computed(() => !!customBg.value)

interface Star {
  x: number
  y: number
  size: number
  delay: number
  duration: number
  color: string
}

interface Meteor {
  x: number
  y: number
  angle: number
  delay: number
  duration: number
  length: number
}

const stars = ref<Star[]>([])
const meteors = ref<Meteor[]>([])

const starColors = [
  'rgba(255, 255, 255, 0.8)',
  'rgba(200, 220, 255, 0.9)',
  'rgba(255, 230, 200, 0.7)',
  'rgba(200, 255, 230, 0.6)',
  'rgba(230, 200, 255, 0.7)',
  'rgba(255, 200, 200, 0.6)',
]

function generateStars(count: number) {
  const result: Star[] = []
  for (let i = 0; i < count; i++) {
    result.push({
      x: Math.random() * 100,
      y: Math.random() * 100,
      size: Math.random() * 2.5 + 0.5,
      delay: Math.random() * 5,
      duration: Math.random() * 3 + 2,
      color: starColors[Math.floor(Math.random() * starColors.length)]
    })
  }
  return result
}

function generateMeteors(count: number) {
  const result: Meteor[] = []
  for (let i = 0; i < count; i++) {
    result.push({
      x: Math.random() * 60 + 10,
      y: Math.random() * 30,
      angle: 30 + Math.random() * 20,
      delay: Math.random() * 15 + i * 8,
      duration: Math.random() * 1.5 + 0.8,
      length: Math.random() * 80 + 60
    })
  }
  return result
}

onMounted(() => {
  stars.value = generateStars(120)
  meteors.value = generateMeteors(4)
})
</script>

<template>
  <div class="starry-bg">
    <div v-if="hasCustomBg" class="custom-bg" :style="{ backgroundImage: `url(${customBg})` }" />
    <template v-else>
      <div
        v-for="(star, i) in stars"
        :key="'s' + i"
        class="star"
        :style="{
          left: star.x + '%',
          top: star.y + '%',
          width: star.size + 'px',
          height: star.size + 'px',
          background: star.color,
          animationDelay: star.delay + 's',
          animationDuration: star.duration + 's'
        }"
      />
      <div
        v-for="(meteor, i) in meteors"
        :key="'m' + i"
        class="meteor"
        :style="{
          left: meteor.x + '%',
          top: meteor.y + '%',
          '--angle': meteor.angle + 'deg',
          '--length': meteor.length + 'px',
          animationDelay: meteor.delay + 's',
          animationDuration: meteor.duration + 's'
        }"
      />
      <div class="nebula nebula--1" />
      <div class="nebula nebula--2" />
      <div class="nebula nebula--3" />
    </template>
  </div>
</template>

<style scoped lang="scss">
.starry-bg {
  position: fixed;
  inset: 0;
  z-index: 0;
  overflow: hidden;
  background: linear-gradient(180deg, #0d1117 0%, #161b22 40%, #1c2333 70%, #0d1117 100%);
}

.custom-bg {
  position: absolute;
  inset: 0;
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
}

.star {
  position: absolute;
  border-radius: 50%;
  animation: star-twinkle ease-in-out infinite;
}

@keyframes star-twinkle {
  0%, 100% { opacity: 0.3; transform: scale(1); }
  50% { opacity: 1; transform: scale(1.3); }
}

.meteor {
  position: absolute;
  width: 2px;
  height: 2px;
  background: white;
  border-radius: 50%;
  animation: meteor-fly linear infinite;
  opacity: 0;
}

.meteor::after {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: var(--length, 80px);
  height: 1.5px;
  background: linear-gradient(90deg, rgba(255, 255, 255, 0.8), rgba(150, 200, 255, 0.4), transparent);
  transform-origin: 0 50%;
  transform: rotate(var(--angle, 35deg));
  border-radius: 1px;
}

@keyframes meteor-fly {
  0% {
    opacity: 0;
    transform: translate(0, 0);
  }
  5% {
    opacity: 1;
  }
  70% {
    opacity: 1;
  }
  100% {
    opacity: 0;
    transform: translate(300px, 200px);
  }
}

.nebula {
  position: absolute;
  border-radius: 50%;
  filter: blur(100px);
  opacity: 0.04;

  &--1 {
    width: 600px;
    height: 600px;
    background: radial-gradient(circle, #6366f1, transparent);
    top: 10%;
    right: 10%;
    animation: nebula-drift 25s ease-in-out infinite;
  }

  &--2 {
    width: 500px;
    height: 500px;
    background: radial-gradient(circle, #818cf8, transparent);
    bottom: 20%;
    left: 5%;
    animation: nebula-drift 30s ease-in-out infinite reverse;
  }

  &--3 {
    width: 450px;
    height: 450px;
    background: radial-gradient(circle, #a5b4fc, transparent);
    top: 50%;
    left: 40%;
    animation: nebula-drift 22s ease-in-out infinite;
  }
}

@keyframes nebula-drift {
  0%, 100% { transform: translate(0, 0) scale(1); }
  33% { transform: translate(30px, -20px) scale(1.05); }
  66% { transform: translate(-20px, 15px) scale(0.95); }
}
</style>
