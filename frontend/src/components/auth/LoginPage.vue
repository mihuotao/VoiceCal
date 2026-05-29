<script setup lang="ts">
import { ref, watch } from 'vue'
import { motion } from 'motion-v'
import { springPresets } from '@/composables/useMotion'
import { useAuth } from '@/composables/useAuth'
import GlassButton from '@/components/glass/GlassButton.vue'

const { login, register, loading, error, clearError } = useAuth()

const mode = ref<'login' | 'register'>('login')

const username = ref('')
const password = ref('')
const confirmPassword = ref('')
const nickname = ref('')
const email = ref('')

const formError = ref('')

watch(mode, () => {
  formError.value = ''
  clearError()
})

function switchMode() {
  mode.value = mode.value === 'login' ? 'register' : 'login'
}

async function handleSubmit() {
  formError.value = ''

  if (!username.value.trim() || !password.value.trim()) {
    formError.value = '请填写用户名和密码'
    return
  }

  if (mode.value === 'register') {
    if (password.value.length < 8) {
      formError.value = '密码长度至少8位'
      return
    }
    if (password.value !== confirmPassword.value) {
      formError.value = '两次密码输入不一致'
      return
    }
  }

  clearError()

  if (mode.value === 'login') {
    await login({ username: username.value.trim(), password: password.value })
  } else {
    await register({
      username: username.value.trim(),
      password: password.value,
      nickname: nickname.value.trim() || undefined,
      email: email.value.trim() || undefined
    })
  }
}
</script>

<template>
  <div class="login-overlay">
    <div class="login-bg-orbs">
      <div class="login-orb login-orb--1" />
      <div class="login-orb login-orb--2" />
    </div>

    <motion.div
      class="login-card"
      :initial="{ opacity: 0, y: 40, scale: 0.96 }"
      :animate="{ opacity: 1, y: 0, scale: 1 }"
      :transition="springPresets.modal"
    >
      <div class="login-header">
        <h1 class="login-title">VoiceCal</h1>
        <p class="login-subtitle">{{ mode === 'login' ? '欢迎回来' : '创建账号' }}</p>
      </div>

      <form class="login-form" @submit.prevent="handleSubmit">
        <div class="login-field">
          <input
            v-model="username"
            class="login-input"
            type="text"
            placeholder="用户名"
            autocomplete="username"
            @input="formError = ''"
          />
        </div>

        <div class="login-field">
          <input
            v-model="password"
            class="login-input"
            type="password"
            placeholder="密码"
            autocomplete="current-password"
            @input="formError = ''"
          />
        </div>

        <template v-if="mode === 'register'">
          <div class="login-field">
            <input
              v-model="confirmPassword"
              class="login-input"
              type="password"
              placeholder="确认密码"
              autocomplete="new-password"
              @input="formError = ''"
            />
          </div>

          <div class="login-field">
            <input
              v-model="nickname"
              class="login-input"
              type="text"
              placeholder="昵称（可选）"
              autocomplete="nickname"
            />
          </div>

          <div class="login-field">
            <input
              v-model="email"
              class="login-input"
              type="email"
              placeholder="邮箱（可选）"
              autocomplete="email"
            />
          </div>
        </template>

        <div v-if="formError || error" class="login-error">
          {{ formError || error }}
        </div>

        <GlassButton
          variant="primary"
          size="lg"
          :loading="loading"
          :disabled="loading"
          rounded
          class="login-submit"
          @click="handleSubmit"
        >
          {{ mode === 'login' ? '登 录' : '注 册' }}
        </GlassButton>
      </form>

      <div class="login-footer">
        <span class="login-footer-text">
          {{ mode === 'login' ? '还没有账号？' : '已有账号？' }}
        </span>
        <button class="login-switch" @click="switchMode">
          {{ mode === 'login' ? '立即注册' : '去登录' }}
        </button>
      </div>
    </motion.div>
  </div>
</template>

<style scoped lang="scss">
@use '@/styles/variables' as *;

.login-overlay {
  position: fixed;
  inset: 0;
  z-index: $z-modal;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg-color, #0a0a1a);
  overflow: hidden;
}

.login-bg-orbs {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.login-orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(100px);
  opacity: 0.12;

  &--1 {
    width: 500px;
    height: 500px;
    background: radial-gradient(circle, $color-primary, transparent);
    top: -15%;
    right: -10%;
    animation: orb-drift-1 14s ease-in-out infinite;
  }

  &--2 {
    width: 400px;
    height: 400px;
    background: radial-gradient(circle, $color-info, transparent);
    bottom: -10%;
    left: -10%;
    animation: orb-drift-2 18s ease-in-out infinite;
  }
}

.login-card {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 400px;
  padding: $space-10 $space-8;
  background: rgba(255, 255, 255, 0.05);
  backdrop-filter: blur(24px);
  -webkit-backdrop-filter: blur(24px);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: $radius-xl;
  box-shadow: $shadow-lg;
}

.login-header {
  text-align: center;
  margin-bottom: $space-8;
}

.login-title {
  font-size: 32px;
  font-weight: $font-weight-bold;
  color: white;
  margin: 0 0 $space-1;
  letter-spacing: -0.02em;
}

.login-subtitle {
  font-size: $font-size-sm;
  color: $color-text-tertiary;
  margin: 0;
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: $space-4;
}

.login-field {
  position: relative;
}

.login-input {
  width: 100%;
  padding: $space-3 $space-4;
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: $radius-lg;
  color: white;
  font-size: $font-size-base;
  font-family: $font-family;
  outline: none;
  transition: border-color $transition-base, box-shadow $transition-base;
  box-sizing: border-box;

  &::placeholder {
    color: $color-text-tertiary;
  }

  &:focus {
    border-color: rgba($color-primary, 0.4);
    box-shadow: 0 0 0 3px rgba($color-primary, 0.1);
  }
}

.login-error {
  font-size: $font-size-sm;
  color: $color-danger;
  text-align: center;
  padding: $space-2;
  background: rgba($color-danger, 0.08);
  border-radius: $radius-md;
}

.login-submit {
  width: 100%;
  margin-top: $space-2;
}

.login-footer {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: $space-2;
  margin-top: $space-6;
}

.login-footer-text {
  font-size: $font-size-sm;
  color: $color-text-tertiary;
}

.login-switch {
  background: none;
  border: none;
  color: $color-primary;
  font-size: $font-size-sm;
  cursor: pointer;
  font-family: $font-family;
  padding: 0;

  &:hover {
    text-decoration: underline;
  }
}

@keyframes orb-drift-1 {
  0%, 100% { transform: translate(0, 0) scale(1); }
  50% { transform: translate(40px, -50px) scale(1.1); }
}

@keyframes orb-drift-2 {
  0%, 100% { transform: translate(0, 0) scale(1); }
  50% { transform: translate(-50px, 40px) scale(1.08); }
}
</style>
