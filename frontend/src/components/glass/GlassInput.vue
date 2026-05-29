<script setup lang="ts">
withDefaults(defineProps<{
  modelValue?: string
  placeholder?: string
  type?: string
  disabled?: boolean
  readonly?: boolean
  rows?: number
}>(), {
  modelValue: '',
  placeholder: '',
  type: 'text',
  disabled: false,
  readonly: false,
  rows: 2
})

const emit = defineEmits<{
  'update:modelValue': [value: string]
  focus: [e: FocusEvent]
  blur: [e: FocusEvent]
}>()

function onInput(e: Event) {
  const target = e.target as HTMLInputElement | HTMLTextAreaElement
  emit('update:modelValue', target.value)
}
</script>

<template>
  <div class="glass-input-wrapper">
    <input
      v-if="type !== 'textarea'"
      class="glass-input"
      :type="type"
      :value="modelValue"
      :placeholder="placeholder"
      :disabled="disabled"
      :readonly="readonly"
      @input="onInput"
      @focus="emit('focus', $event)"
      @blur="emit('blur', $event)"
    />
    <textarea
      v-else
      class="glass-input glass-input--textarea"
      :value="modelValue"
      :placeholder="placeholder"
      :disabled="disabled"
      :readonly="readonly"
      :rows="rows"
      @input="onInput"
      @focus="emit('focus', $event)"
      @blur="emit('blur', $event)"
    />
  </div>
</template>

<style scoped lang="scss">
@use '@/styles/variables' as *;

.glass-input-wrapper {
  width: 100%;
}

.glass-input {
  width: 100%;
  background: rgba(255, 255, 255, 0.08);
  backdrop-filter: blur(8px);
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: $radius-md;
  padding: $space-3 $space-4;
  color: $color-text-primary;
  font-family: $font-family;
  font-size: $font-size-base;
  transition: all $transition-base;
  outline: none;

  &::placeholder {
    color: $color-text-tertiary;
  }

  &:focus {
    background: rgba(255, 255, 255, 0.14);
    border-color: rgba($color-primary, 0.5);
    box-shadow: 0 0 0 3px rgba($color-primary, 0.15);
  }

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }

  &--textarea {
    resize: vertical;
    min-height: 60px;
    line-height: $line-height-relaxed;
  }
}
</style>
