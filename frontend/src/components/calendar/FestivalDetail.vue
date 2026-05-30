<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import type { FestivalInfo } from '@/composables/useFestival'

defineProps<{
  open: boolean
  festival: FestivalInfo | null
}>()

const emit = defineEmits<{
  close: []
}>()

// 拖动状态
const isDragging = ref(false)
const dragOffset = { x: 0, y: 0 }
const position = ref({ x: 0, y: 0 })
const isPositionInitialized = ref(false)

// 缩放状态
const isResizing = ref(false)
const size = ref({ width: 440, height: 0 })
const minSize = { width: 360, height: 400 }

// 初始化位置（居中）
function initPosition() {
  if (!isPositionInitialized.value) {
    const x = (window.innerWidth - size.value.width) / 2
    const y = (window.innerHeight - 600) / 2
    position.value = { x: Math.max(0, x), y: Math.max(0, y) }
    isPositionInitialized.value = true
  }
}

// 拖动开始
function onDragStart(e: MouseEvent) {
  if ((e.target as HTMLElement).closest('.festival-detail__close')) return
  isDragging.value = true
  dragOffset.x = e.clientX - position.value.x
  dragOffset.y = e.clientY - position.value.y
  e.preventDefault()
}

// 拖动中
function onDragMove(e: MouseEvent) {
  if (isDragging.value) {
    position.value = {
      x: Math.max(0, Math.min(window.innerWidth - 100, e.clientX - dragOffset.x)),
      y: Math.max(0, Math.min(window.innerHeight - 50, e.clientY - dragOffset.y))
    }
  }
  if (isResizing.value) {
    size.value = {
      width: Math.max(minSize.width, e.clientX - position.value.x),
      height: Math.max(minSize.height, e.clientY - position.value.y)
    }
  }
}

// 拖动结束
function onDragEnd() {
  isDragging.value = false
  isResizing.value = false
}

// 缩放开始
function onResizeStart(e: MouseEvent) {
  isResizing.value = true
  e.preventDefault()
  e.stopPropagation()
}

// 监听全局事件
onMounted(() => {
  document.addEventListener('mousemove', onDragMove)
  document.addEventListener('mouseup', onDragEnd)
})

onUnmounted(() => {
  document.removeEventListener('mousemove', onDragMove)
  document.removeEventListener('mouseup', onDragEnd)
})
</script>

<template>
  <Teleport to="body">
    <Transition name="festival-detail" @before-enter="initPosition">
      <div v-if="open && festival" class="festival-overlay" @click.self="emit('close')">
        <div
          class="festival-detail"
          :style="{
            left: position.x + 'px',
            top: position.y + 'px',
            width: size.width + 'px',
            height: size.height > 0 ? size.height + 'px' : 'auto'
          }"
          :class="{
            'festival-detail--dragging': isDragging,
            'festival-detail--resizing': isResizing
          }"
        >
          <!-- 头部（可拖动） -->
          <div class="festival-detail__header" @mousedown="onDragStart">
            <div class="festival-detail__title-row">
              <span class="festival-detail__emoji">{{ festival.emoji }}</span>
              <h2 class="festival-detail__name">{{ festival.name }}</h2>
            </div>
            <button class="festival-detail__close" @click="emit('close')">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
                <line x1="18" y1="6" x2="6" y2="18" />
                <line x1="6" y1="6" x2="18" y2="18" />
              </svg>
            </button>
          </div>

          <!-- 内容 -->
          <div class="festival-detail__body">
            <!-- 基本信息 -->
            <div v-if="festival.detail" class="festival-detail__section">
              <div class="festival-detail__info-row">
                <span class="festival-detail__label">📅 日期</span>
                <span class="festival-detail__value">{{ festival.detail.date }}</span>
              </div>
              <div class="festival-detail__info-row">
                <span class="festival-detail__label">🏷️ 类型</span>
                <span class="festival-detail__value">{{ festival.detail.type }}</span>
              </div>
            </div>

            <!-- 简介 -->
            <div v-if="festival.detail?.summary" class="festival-detail__section">
              <h3 class="festival-detail__section-title">📖 简介</h3>
              <p class="festival-detail__text">{{ festival.detail.summary }}</p>
            </div>

            <!-- 习俗 -->
            <div v-if="festival.detail?.customs?.length" class="festival-detail__section">
              <h3 class="festival-detail__section-title">🎯 习俗</h3>
              <div class="festival-detail__tags">
                <span v-for="custom in festival.detail.customs" :key="custom" class="festival-detail__tag">
                  {{ custom }}
                </span>
              </div>
            </div>

            <!-- 美食 -->
            <div v-if="festival.detail?.food?.length" class="festival-detail__section">
              <h3 class="festival-detail__section-title">🍽️ 美食</h3>
              <div class="festival-detail__tags">
                <span v-for="item in festival.detail.food" :key="item" class="festival-detail__tag festival-detail__tag--food">
                  {{ item }}
                </span>
              </div>
            </div>

            <!-- 诗词 -->
            <div v-if="festival.detail?.poetry" class="festival-detail__section festival-detail__section--poetry">
              <h3 class="festival-detail__section-title">📝 诗词</h3>
              <blockquote class="festival-detail__poetry">
                <p>{{ festival.detail.poetry }}</p>
                <cite v-if="festival.detail.poet">——{{ festival.detail.poet }}</cite>
              </blockquote>
            </div>

            <!-- 祝福语 -->
            <div class="festival-detail__section festival-detail__section--greeting">
              <div class="festival-detail__greeting">
                {{ festival.greeting }}
              </div>
            </div>
          </div>

          <!-- 缩放把手 -->
          <div class="festival-detail__resize-handle" @mousedown="onResizeStart">
            <svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor">
              <path d="M14 16L16 14L14 16ZM10 16L16 10L16 16ZM6 16L16 6L16 16Z" />
            </svg>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped lang="scss">
@use '@/styles/variables' as *;

.festival-overlay {
  position: fixed;
  inset: 0;
  z-index: $z-modal;
  background: rgba(0, 0, 0, 0.3);
}

.festival-detail {
  position: fixed;
  display: flex;
  flex-direction: column;
  background: linear-gradient(135deg, #1e1e3f, #2a2a5a);
  border-radius: $radius-lg;
  border: 1px solid rgba(255, 255, 255, 0.1);
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.5);
  overflow: hidden;

  &--dragging {
    user-select: none;
    cursor: grabbing;
  }

  &--resizing {
    user-select: none;
  }
}

// 头部
.festival-detail__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: $space-4 $space-5;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  cursor: grab;
  flex-shrink: 0;

  &:active {
    cursor: grabbing;
  }
}

.festival-detail__title-row {
  display: flex;
  align-items: center;
  gap: $space-3;
}

.festival-detail__emoji {
  font-size: 32px;
  line-height: 1;
}

.festival-detail__name {
  font-size: $font-size-xl;
  font-weight: $font-weight-bold;
  color: white;
  margin: 0;
}

.festival-detail__close {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: none;
  background: rgba(255, 255, 255, 0.08);
  color: $color-text-secondary;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all $transition-fast;

  &:hover {
    background: rgba(255, 255, 255, 0.15);
    color: white;
  }
}

// 内容
.festival-detail__body {
  flex: 1;
  overflow-y: auto;
  padding: $space-4 $space-5;
}

.festival-detail__section {
  margin-bottom: $space-5;

  &:last-child {
    margin-bottom: 0;
  }
}

.festival-detail__section-title {
  font-size: $font-size-sm;
  font-weight: $font-weight-semibold;
  color: $color-text-secondary;
  margin: 0 0 $space-3 0;
}

// 信息行
.festival-detail__info-row {
  display: flex;
  align-items: center;
  gap: $space-3;
  padding: $space-2 0;
}

.festival-detail__label {
  font-size: $font-size-sm;
  color: $color-text-tertiary;
  min-width: 60px;
}

.festival-detail__value {
  font-size: $font-size-sm;
  color: $color-text-primary;
}

// 文本
.festival-detail__text {
  font-size: $font-size-sm;
  color: $color-text-secondary;
  line-height: $line-height-relaxed;
  margin: 0;
}

// 标签
.festival-detail__tags {
  display: flex;
  flex-wrap: wrap;
  gap: $space-2;
}

.festival-detail__tag {
  padding: $space-1 $space-3;
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: $radius-full;
  font-size: $font-size-xs;
  color: $color-text-secondary;

  &--food {
    background: rgba($color-warning, 0.1);
    border-color: rgba($color-warning, 0.2);
    color: $color-warning;
  }
}

// 诗词
.festival-detail__section--poetry {
  background: rgba(255, 255, 255, 0.03);
  border-radius: $radius-md;
  padding: $space-4;
  margin-left: -$space-4;
  margin-right: -$space-4;
}

.festival-detail__poetry {
  margin: 0;

  p {
    font-size: $font-size-base;
    color: $color-text-primary;
    line-height: $line-height-relaxed;
    font-style: italic;
    margin: 0 0 $space-2 0;
  }

  cite {
    font-size: $font-size-xs;
    color: $color-text-tertiary;
    font-style: normal;
  }
}

// 祝福语
.festival-detail__section--greeting {
  background: linear-gradient(135deg, rgba($color-primary, 0.1), rgba($color-primary, 0.05));
  border-radius: $radius-md;
  padding: $space-4;
  margin-left: -$space-4;
  margin-right: -$space-4;
  border: 1px solid rgba($color-primary, 0.15);
}

.festival-detail__greeting {
  font-size: $font-size-base;
  color: $color-primary-light;
  text-align: center;
  font-weight: $font-weight-medium;
}

// 缩放把手
.festival-detail__resize-handle {
  position: absolute;
  bottom: 0;
  right: 0;
  width: 24px;
  height: 24px;
  cursor: nwse-resize;
  color: rgba(255, 255, 255, 0.3);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: color $transition-fast;

  &:hover {
    color: rgba(255, 255, 255, 0.6);
  }

  svg {
    transform: rotate(180deg);
  }
}

// 动画
.festival-detail-enter-active {
  transition: all 0.3s ease;
}

.festival-detail-leave-active {
  transition: all 0.2s ease;
}

.festival-detail-enter-from {
  opacity: 0;
  transform: scale(0.95);
}

.festival-detail-leave-to {
  opacity: 0;
  transform: scale(0.98);
}
</style>
