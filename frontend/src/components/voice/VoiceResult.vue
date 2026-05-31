<script setup lang="ts">
import { computed } from 'vue'
import { motion } from 'motion-v'
import { springPresets } from '@/composables/useMotion'
import GlassButton from '@/components/glass/GlassButton.vue'
import type { ParsedIntent } from '@/types/voice'

const props = withDefaults(defineProps<{
  transcript: string
  intent?: ParsedIntent | null
}>(), {
  transcript: '',
  intent: null
})

const confirmButtonText = computed(() => {
  switch (props.intent?.action) {
    case 'query': return '查看结果'
    case 'create': return '确认创建'
    case 'preview': return '打开表单'
    case 'update': return '确认修改'
    case 'delete': return '确认删除'
    case 'reminder': return '确认提醒'
    case 'clarify': return '重新录音'
    case 'unknown': return '重新录音'
    default: return '确认'
  }
})

const emit = defineEmits<{
  confirm: []
  edit: []
  retry: []
  close: []
  openForm: []
}>()

const actionLabel: Record<string, string> = {
  create: '创建日程',
  created: '创建成功',
  preview: '准备创建',
  query: '查询日程',
  update: '更新日程',
  delete: '删除日程',
  reminder: '设置提醒',
  clarify: '需要补充',
  unknown: '无法识别'
}
</script>

<template>
  <div class="voice-result">
    <div class="voice-result__icon">
      <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round">
        <path d="M12 1a3 3 0 0 0-3 3v8a3 3 0 0 0 6 0V4a3 3 0 0 0-3-3z" />
        <path d="M19 10v2a7 7 0 0 1-14 0v-2" />
        <line x1="12" y1="19" x2="12" y2="23" />
        <line x1="8" y1="23" x2="16" y2="23" />
      </svg>
    </div>

    <div class="voice-result__transcript">
      <div class="voice-result__label">识别结果</div>
      <div class="voice-result__text">{{ transcript }}</div>
    </div>

    <div v-if="intent" class="voice-result__intent">
      <div class="voice-result__label">识别意图</div>
      <div class="voice-result__action">
        <span class="voice-result__badge" :class="`voice-result__badge--${intent.action}`">
          {{ actionLabel[intent.action] || '未知' }}
        </span>
        <span class="voice-result__confidence">
          置信度 {{ Math.round(intent.confidence * 100) }}%
        </span>
      </div>

      <!-- 澄清提示 -->
      <div v-if="intent.action === 'clarify' && intent.clarifyQuestion" class="voice-result__field">
        <span class="voice-result__field-label">系统提示</span>
        <span class="voice-result__field-value">{{ intent.clarifyQuestion }}</span>
      </div>

      <div v-if="intent.title && intent.action !== 'clarify'" class="voice-result__field">
        <span class="voice-result__field-label">标题</span>
        <span class="voice-result__field-value">{{ intent.title }}</span>
      </div>
      <div v-if="intent.action === 'query' && intent.queryDate" class="voice-result__field">
        <span class="voice-result__field-label">查询日期</span>
        <span class="voice-result__field-value">{{ intent.queryDate }}{{ intent.queryEndDate && intent.queryEndDate !== intent.queryDate ? ' 至 ' + intent.queryEndDate : '' }}</span>
      </div>
      <div v-if="intent.action === 'query' && intent.events" class="voice-result__field">
        <span class="voice-result__field-label">找到事件</span>
        <span class="voice-result__field-value">{{ intent.events.length }} 个安排</span>
      </div>
      <div v-if="intent.action === 'unknown' && intent.responseText" class="voice-result__field">
        <span class="voice-result__field-label">系统回复</span>
        <span class="voice-result__field-value">{{ intent.responseText }}</span>
      </div>

      <!-- 预览卡片：显示识别到的事件信息 -->
      <div v-if="intent.action === 'preview' && intent.event" class="voice-result__preview">
        <div class="voice-result__preview-header">
          <span class="voice-result__preview-icon">📅</span>
          <span class="voice-result__preview-title">识别到的事件信息</span>
        </div>
        <div class="voice-result__preview-content">
          <div v-if="intent.event.title" class="voice-result__preview-item">
            <span class="voice-result__preview-label">标题</span>
            <span class="voice-result__preview-value">{{ intent.event.title }}</span>
          </div>
          <div v-if="intent.event.date" class="voice-result__preview-item">
            <span class="voice-result__preview-label">日期</span>
            <span class="voice-result__preview-value">{{ intent.event.date }}</span>
          </div>
          <div v-if="intent.event.startTime" class="voice-result__preview-item">
            <span class="voice-result__preview-label">时间</span>
            <span class="voice-result__preview-value">{{ intent.event.startTime }} - {{ intent.event.endTime }}</span>
          </div>
          <div v-if="intent.event.location" class="voice-result__preview-item">
            <span class="voice-result__preview-label">地点</span>
            <span class="voice-result__preview-value">{{ intent.event.location }}</span>
          </div>
          <div v-if="intent.event.category" class="voice-result__preview-item">
            <span class="voice-result__preview-label">分类</span>
            <span class="voice-result__preview-value">{{ intent.event.category }}</span>
          </div>
        </div>
        <div class="voice-result__preview-hint">
          点击下方按钮打开表单，确认后创建事件
        </div>
      </div>
    </div>

    <div class="voice-result__actions">
      <motion.button
        class="voice-result__btn voice-result__btn--secondary"
        :while-hover="{ scale: 1.04 }"
        :while-tap="{ scale: 0.97 }"
        :transition="springPresets.snappy"
        @click="emit('retry')"
      >
        重新录音
      </motion.button>
      <motion.button
        v-if="intent?.action !== 'unknown' && intent?.action !== 'clarify' && intent?.action !== 'preview'"
        class="voice-result__btn voice-result__btn--secondary"
        :while-hover="{ scale: 1.04 }"
        :while-tap="{ scale: 0.97 }"
        :transition="springPresets.snappy"
        @click="emit('edit')"
      >
        编辑
      </motion.button>
      <GlassButton
        v-if="intent?.action === 'preview'"
        variant="primary"
        @click="emit('openForm')"
      >
        {{ confirmButtonText }}
      </GlassButton>
      <GlassButton
        v-else
        variant="primary"
        @click="emit('confirm')"
      >
        {{ confirmButtonText }}
      </GlassButton>
    </div>
  </div>
</template>

<style scoped lang="scss">
@use '@/styles/variables' as *;

.voice-result {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: $space-5;
  width: 100%;
  max-width: 400px;
  padding: $space-2 0;

  &__icon {
    color: $color-primary-light;
    opacity: 0.8;
  }

  &__transcript {
    text-align: center;
    width: 100%;
  }

  &__label {
    font-size: $font-size-xs;
    color: $color-text-tertiary;
    margin-bottom: $space-1;
    text-transform: uppercase;
    letter-spacing: 0.05em;
  }

  &__text {
    font-size: $font-size-xl;
    font-weight: $font-weight-semibold;
    color: $color-text-primary;
    line-height: $line-height-tight;
    padding: $space-3;
    background: rgba(255, 255, 255, 0.04);
    border-radius: $radius-md;
    border: 1px solid rgba(255, 255, 255, 0.06);
  }

  &__intent {
    width: 100%;
    display: flex;
    flex-direction: column;
    gap: $space-2;
  }

  &__action {
    display: flex;
    align-items: center;
    gap: $space-3;
  }

  &__badge {
    display: inline-flex;
    align-items: center;
    padding: $space-1 $space-3;
    border-radius: $radius-full;
    font-size: $font-size-xs;
    font-weight: $font-weight-medium;

    &--create {
      background: rgba($color-primary, 0.15);
      color: $color-primary-light;
    }
    &--query {
      background: rgba($color-info, 0.15);
      color: $color-info;
    }
    &--update {
      background: rgba($color-warning, 0.15);
      color: $color-warning;
    }
    &--delete {
      background: rgba($color-danger, 0.15);
      color: $color-danger;
    }
    &--reminder {
      background: rgba($color-success, 0.15);
      color: $color-success;
    }
    &--clarify {
      background: rgba($color-warning, 0.15);
      color: $color-warning;
    }
    &--unknown {
      background: rgba(255, 255, 255, 0.1);
      color: $color-text-tertiary;
    }
  }

  &__confidence {
    font-size: $font-size-xs;
    color: $color-text-tertiary;
  }

  &__field {
    display: flex;
    flex-direction: column;
    gap: 2px;
    padding: $space-2 $space-3;
    background: rgba(255, 255, 255, 0.03);
    border-radius: $radius-sm;
  }

  &__field-label {
    font-size: 10px;
    color: $color-text-tertiary;
    text-transform: uppercase;
    letter-spacing: 0.05em;
  }

  &__field-value {
    font-size: $font-size-base;
    color: $color-text-primary;
  }

  &__actions {
    display: flex;
    align-items: center;
    gap: $space-3;
    margin-top: $space-2;
  }

  &__btn {
    padding: $space-2 $space-4;
    border-radius: $radius-full;
    font-size: $font-size-sm;
    font-family: $font-family;
    font-weight: $font-weight-medium;
    cursor: pointer;
    border: 1px solid rgba(255, 255, 255, 0.1);
    background: rgba(255, 255, 255, 0.06);
    color: $color-text-secondary;
    transition: background $transition-base, color $transition-base;

    &:hover {
      background: rgba(255, 255, 255, 0.12);
      color: $color-text-primary;
    }
  }

  &__preview {
    width: 100%;
    background: rgba(255, 255, 255, 0.04);
    border: 1px solid rgba(255, 255, 255, 0.08);
    border-radius: $radius-md;
    padding: $space-4;
    margin-top: $space-2;
  }

  &__preview-header {
    display: flex;
    align-items: center;
    gap: $space-2;
    margin-bottom: $space-3;
    padding-bottom: $space-2;
    border-bottom: 1px solid rgba(255, 255, 255, 0.06);
  }

  &__preview-icon {
    font-size: 18px;
  }

  &__preview-title {
    font-size: $font-size-sm;
    font-weight: $font-weight-medium;
    color: $color-text-primary;
  }

  &__preview-content {
    display: flex;
    flex-direction: column;
    gap: $space-2;
  }

  &__preview-item {
    display: flex;
    align-items: center;
    gap: $space-3;
  }

  &__preview-label {
    font-size: $font-size-xs;
    color: $color-text-tertiary;
    min-width: 40px;
  }

  &__preview-value {
    font-size: $font-size-base;
    color: $color-text-primary;
    font-weight: $font-weight-medium;
  }

  &__preview-hint {
    font-size: $font-size-xs;
    color: $color-text-tertiary;
    margin-top: $space-3;
    padding-top: $space-2;
    border-top: 1px solid rgba(255, 255, 255, 0.06);
    text-align: center;
  }
}
</style>
