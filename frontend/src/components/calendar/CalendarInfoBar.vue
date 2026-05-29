<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { Solar } from 'lunar-javascript'

const time = ref('')
const dateStr = ref('')
const weekday = ref('')
const lunarStr = ref('')
const weatherDesc = ref('')
const temperature = ref('')

const enToZh: Record<string, string> = {
  'Sunny': '晴天',
  'Clear': '晴朗',
  'Partly cloudy': '多云',
  'Partly Cloudy': '多云',
  'Cloudy': '阴天',
  'Overcast': '阴天',
  'Mist': '薄雾',
  'Fog': '雾',
  'Light rain': '小雨',
  'Light Rain': '小雨',
  'Moderate rain': '中雨',
  'Heavy rain': '大雨',
  'Patchy rain possible': '可能有阵雨',
  'Patchy light rain': '零星小雨',
  'Thundery outbreaks possible': '可能有雷阵雨',
  'Light drizzle': '毛毛雨',
  'Heavy rain at times': '间歇大雨',
  'Moderate or heavy rain shower': '大到暴雨',
  'Light rain shower': '阵雨',
  'Torrential rain shower': '暴雨',
  'Light snow': '小雪',
  'Moderate snow': '中雪',
  'Heavy snow': '大雪',
  'Blowing snow': '风吹雪',
  'Blizzard': '暴风雪',
  'Patchy light snow': '零星小雪',
  'Light freezing rain': '冻雨',
  'Ice pellets': '冰粒',
  'Haze': '霾',
  'Wind': '大风',
  'Breezy': '微风',
  'Windy': '大风',
}

const weekdayNames = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六']

const weatherIcon = computed(() => {
  const d = weatherDesc.value
  if (!d) return '🌤️'
  if (d.includes('晴') && !d.includes('云')) return '☀️'
  if (d.includes('晴') && d.includes('云')) return '⛅'
  if (d.includes('多云') || d.includes('阴')) return '☁️'
  if (d.includes('小雨') || d.includes('阵雨')) return '🌦️'
  if (d.includes('中雨') || d.includes('大雨') || d.includes('暴雨')) return '🌧️'
  if (d.includes('雷')) return '⛈️'
  if (d.includes('雪') || d.includes('冰')) return '❄️'
  if (d.includes('雾') || d.includes('霾')) return '🌫️'
  if (d.includes('风')) return '💨'
  return '🌤️'
})

function updateTime() {
  const now = new Date()
  time.value = now.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit', second: '2-digit' })
}

function updateDate() {
  const now = new Date()
  const y = now.getFullYear()
  const m = now.getMonth() + 1
  const d = now.getDate()
  dateStr.value = `${y}年${m}月${d}日`
  weekday.value = weekdayNames[now.getDay()]

  try {
    const solar = Solar.fromYmd(y, m, d)
    const lunar = solar.getLunar()
    lunarStr.value = `${lunar.getMonthInChinese()}月${lunar.getDayInChinese()}`
  } catch {
    lunarStr.value = ''
  }
}

async function fetchWeather() {
  try {
    const res = await fetch('https://wttr.in/?format=j1')
    if (!res.ok) return
    const data = await res.json()
    const current = data.current_condition?.[0]
    if (current) {
      const desc = current.weatherDesc?.[0]?.value || ''
      const temp = current.temp_C || ''
      weatherDesc.value = enToZh[desc] || desc
      temperature.value = temp ? `${temp}°C` : ''
    }
  } catch {
    weatherDesc.value = ''
    temperature.value = ''
  }
}

let timer: ReturnType<typeof setInterval>

onMounted(() => {
  updateTime()
  updateDate()
  fetchWeather()
  timer = setInterval(updateTime, 1000)
})

onUnmounted(() => {
  clearInterval(timer)
})
</script>

<template>
  <div class="info-bar">
    <div class="info-bar__primary">
      <span class="info-bar__time">{{ time }}</span>
      <span v-if="weatherDesc || temperature" class="info-bar__weather">
        <span class="info-bar__weather-icon">{{ weatherIcon }}</span>
        <span v-if="weatherDesc" class="info-bar__weather-desc">{{ weatherDesc }}</span>
        <span v-if="temperature" class="info-bar__temp">{{ temperature }}</span>
      </span>
    </div>
    <div class="info-bar__secondary">
      <span class="info-bar__date">{{ dateStr }}</span>
      <span class="info-bar__sep">·</span>
      <span class="info-bar__weekday">{{ weekday }}</span>
      <span v-if="lunarStr" class="info-bar__sep">·</span>
      <span v-if="lunarStr" class="info-bar__lunar">{{ lunarStr }}</span>
    </div>
  </div>
</template>

<style scoped lang="scss">
@use '@/styles/variables' as *;

.info-bar {
  display: flex;
  flex-direction: column;
  gap: $space-2;
  padding: $space-4 $space-5;
  background: rgba(255, 255, 255, 0.04);
  backdrop-filter: blur(var(--glass-blur));
  -webkit-backdrop-filter: blur(var(--glass-blur));
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: $radius-lg;
}

.info-bar__primary {
  display: flex;
  align-items: baseline;
  gap: $space-5;
}

.info-bar__time {
  font-size: 36px;
  font-weight: $font-weight-bold;
  color: white;
  letter-spacing: 0.02em;
  font-variant-numeric: tabular-nums;
}

.info-bar__weather {
  display: flex;
  align-items: center;
  gap: $space-2;
}

.info-bar__weather-icon {
  font-size: 28px;
  line-height: 1;
}

.info-bar__weather-desc {
  font-size: $font-size-xl;
  font-weight: $font-weight-semibold;
  color: $color-text-secondary;
}

.info-bar__temp {
  font-size: $font-size-xl;
  font-weight: $font-weight-bold;
  color: $color-warning;
}

.info-bar__secondary {
  display: flex;
  align-items: center;
  gap: $space-3;
}

.info-bar__date {
  font-size: $font-size-base;
  color: $color-text-secondary;
}

.info-bar__weekday {
  font-size: $font-size-base;
  color: $color-text-secondary;
}

.info-bar__lunar {
  font-size: $font-size-base;
  color: $color-text-tertiary;
}

.info-bar__sep {
  font-size: $font-size-base;
  color: $color-text-tertiary;
}
</style>
