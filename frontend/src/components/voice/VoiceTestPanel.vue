<script setup lang="ts">
import { ref } from 'vue'
import GlassButton from '@/components/glass/GlassButton.vue'

const emit = defineEmits<{
  test: [text: string]
}>()

const testCases = [
  { label: '查询 - 今天有什么安排', text: '查询今天有什么安排', type: 'query' },
  { label: '查询 - 明天的日程', text: '查看明天的日程', type: 'query' },
  { label: '创建 - 明天下午三点开会', text: '创建明天下午三点开会', type: 'create' },
  { label: '创建 - 新建项目评审', text: '新建明天下午两点的项目评审会议', type: 'create' },
  { label: '创建 - 下周三培训', text: '创建下周三上午十点在会议室A的产品培训', type: 'create' },
  { label: '创建 - 全天团建', text: '添加后天全天的团队建设活动', type: 'create' },
  { label: '修改 - 改会议时间', text: '修改明天的会议到下午四点', type: 'update' },
  { label: '删除 - 取消会议', text: '删除明天的会议', type: 'delete' },
]

const customText = ref('')

function handleTest(text: string) {
  emit('test', text)
}

function handleCustomTest() {
  if (customText.value.trim()) {
    emit('test', customText.value.trim())
  }
}
</script>

<template>
  <div class="test-panel">
    <div class="test-panel__header">
      <span class="test-panel__icon">🧪</span>
      <span class="test-panel__title">语音测试面板</span>
    </div>

    <div class="test-panel__cases">
      <GlassButton
        v-for="tc in testCases"
        :key="tc.text"
        size="sm"
        :class="['test-panel__btn', `test-panel__btn--${tc.type}`]"
        @click="handleTest(tc.text)"
      >
        {{ tc.label }}
      </GlassButton>
    </div>

    <div class="test-panel__custom">
      <input
        v-model="customText"
        class="test-panel__input"
        placeholder="输入自定义语音文本..."
        @keyup.enter="handleCustomTest"
      />
      <GlassButton size="sm" @click="handleCustomTest">
        测试
      </GlassButton>
    </div>
  </div>
</template>

<style scoped lang="scss">
@use '@/styles/variables' as *;

.test-panel {
  background: rgba(255, 255, 255, 0.06);
  backdrop-filter: blur(var(--glass-blur));
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: $radius-lg;
  padding: $space-4;
  margin-bottom: $space-4;

  &__header {
    display: flex;
    align-items: center;
    gap: $space-2;
    margin-bottom: $space-3;
    padding-bottom: $space-2;
    border-bottom: 1px solid rgba(255, 255, 255, 0.06);
  }

  &__icon {
    font-size: 18px;
  }

  &__title {
    font-size: $font-size-sm;
    font-weight: $font-weight-medium;
    color: $color-text-primary;
  }

  &__cases {
    display: flex;
    flex-wrap: wrap;
    gap: $space-2;
    margin-bottom: $space-3;
  }

  &__btn {
    font-size: $font-size-xs !important;
    padding: $space-1 $space-3 !important;

    &--query {
      background: rgba($color-info, 0.15) !important;
      border-color: rgba($color-info, 0.3) !important;
      color: $color-info !important;
    }

    &--create {
      background: rgba($color-primary, 0.15) !important;
      border-color: rgba($color-primary, 0.3) !important;
      color: $color-primary-light !important;
    }

    &--update {
      background: rgba($color-warning, 0.15) !important;
      border-color: rgba($color-warning, 0.3) !important;
      color: $color-warning !important;
    }

    &--delete {
      background: rgba($color-danger, 0.15) !important;
      border-color: rgba($color-danger, 0.3) !important;
      color: $color-danger !important;
    }
  }

  &__custom {
    display: flex;
    gap: $space-2;
  }

  &__input {
    flex: 1;
    padding: $space-2 $space-3;
    background: rgba(255, 255, 255, 0.06);
    border: 1px solid rgba(255, 255, 255, 0.1);
    border-radius: $radius-md;
    color: $color-text-primary;
    font-size: $font-size-sm;
    font-family: $font-family;

    &::placeholder {
      color: $color-text-tertiary;
    }

    &:focus {
      outline: none;
      border-color: rgba($color-primary, 0.5);
    }
  }
}
</style>
