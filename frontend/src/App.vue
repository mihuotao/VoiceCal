<script setup lang="ts">
import GlassButton from '@/components/glass/GlassButton.vue'
import GlassPanel from '@/components/glass/GlassPanel.vue'
import GlassDialog from '@/components/glass/GlassDialog.vue'
import GlassInput from '@/components/glass/GlassInput.vue'
import { shallowRef } from 'vue'

const dialogOpen = shallowRef(false)
const inputValue = shallowRef('')
</script>

<template>
  <div class="app-shell">
    <header class="app-header">
      <h1 class="app-title">VoiceCal</h1>
      <span class="app-subtitle">智能语音日历</span>
    </header>

    <main class="app-main">
      <GlassPanel class="demo-panel">
        <h2 class="demo-heading">毛玻璃组件库</h2>
        <p class="demo-text">GlassPanel · GlassButton · GlassDialog · GlassInput</p>

        <div class="demo-actions">
          <GlassButton variant="primary" @click="dialogOpen = true">
            打开弹窗
          </GlassButton>
          <GlassButton variant="default">
            默认按钮
          </GlassButton>
          <GlassButton variant="ghost">
            幽灵按钮
          </GlassButton>
        </div>

        <div class="demo-input">
          <GlassInput v-model="inputValue" placeholder="输入文字..." />
        </div>

        <div class="demo-grid">
          <GlassPanel v-for="i in 3" :key="i" hoverable class="demo-card">
            <strong>卡片 {{ i }}</strong>
            <span>hover 查看升起效果</span>
          </GlassPanel>
        </div>
      </GlassPanel>
    </main>

    <GlassDialog
      :open="dialogOpen"
      title="玻璃弹窗"
      @close="dialogOpen = false"
    >
      <p>这是一个毛玻璃风格的弹窗。</p>
      <p>带有弹簧入场动画和毛玻璃背景。</p>
      <template #footer>
        <GlassButton variant="ghost" @click="dialogOpen = false">
          取消
        </GlassButton>
        <GlassButton variant="primary" @click="dialogOpen = false">
          确认
        </GlassButton>
      </template>
    </GlassDialog>
  </div>
</template>

<style scoped lang="scss">
@use '@/styles/variables' as *;

.app-shell {
  width: 100%;
  height: 100vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.app-header {
  display: flex;
  align-items: baseline;
  gap: $space-3;
  padding: $space-4 $space-6;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
}

.app-title {
  font-size: $font-size-2xl;
  font-weight: $font-weight-bold;
  background: linear-gradient(135deg, $color-primary-light, $color-primary);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.app-subtitle {
  font-size: $font-size-sm;
  color: $color-text-secondary;
}

.app-main {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: $space-6;
  overflow-y: auto;
}

.demo-panel {
  width: 100%;
  max-width: 560px;
}

.demo-heading {
  font-size: $font-size-xl;
  font-weight: $font-weight-semibold;
  margin-bottom: $space-2;
}

.demo-text {
  color: $color-text-secondary;
  font-size: $font-size-sm;
  margin-bottom: $space-6;
}

.demo-actions {
  display: flex;
  gap: $space-3;
  margin-bottom: $space-6;
  flex-wrap: wrap;
}

.demo-input {
  margin-bottom: $space-6;
}

.demo-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: $space-3;
}

.demo-card {
  display: flex;
  flex-direction: column;
  gap: $space-1;
  font-size: $font-size-sm;
  text-align: center;

  strong {
    font-weight: $font-weight-semibold;
  }

  span {
    color: $color-text-secondary;
    font-size: $font-size-xs;
  }
}
</style>
