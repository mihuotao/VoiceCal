<script setup lang="ts">
import { ref, watch } from 'vue'
import { useAuth } from '@/composables/useAuth'

const props = defineProps<{
  open: boolean
}>()

const emit = defineEmits<{
  close: []
}>()

const { user, logout, loadAvatar, saveAvatar, changePassword } = useAuth()

// 头像
const avatar = ref('')

// 密码修改
const oldPassword = ref('')
const newPassword = ref('')
const confirmPassword = ref('')

// 状态
const avatarMessage = ref('')
const avatarMessageType = ref<'success' | 'error'>('success')
const passwordMessage = ref('')
const passwordMessageType = ref<'success' | 'error'>('success')
const saving = ref(false)
const changingPassword = ref(false)

// 初始化用户数据
watch(() => props.open, (val) => {
  if (val) {
    avatar.value = loadAvatar()
    avatarMessage.value = ''
    passwordMessage.value = ''
    oldPassword.value = ''
    newPassword.value = ''
    confirmPassword.value = ''
  }
})

// 压缩图片
function compressImage(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = (e) => {
      const img = new Image()
      img.onload = () => {
        const canvas = document.createElement('canvas')
        const size = 200
        canvas.width = size
        canvas.height = size
        const ctx = canvas.getContext('2d')!

        // 居中裁剪
        const min = Math.min(img.width, img.height)
        const sx = (img.width - min) / 2
        const sy = (img.height - min) / 2
        ctx.drawImage(img, sx, sy, min, min, 0, 0, size, size)

        resolve(canvas.toDataURL('image/jpeg', 0.8))
      }
      img.onerror = reject
      img.src = e.target?.result as string
    }
    reader.onerror = reject
    reader.readAsDataURL(file)
  })
}

// 头像上传
async function handleAvatarUpload(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return

  try {
    const compressed = await compressImage(file)
    avatar.value = compressed
    // 自动保存
    saveAvatar(compressed)
    avatarMessage.value = '头像已保存'
    avatarMessageType.value = 'success'
  } catch {
    avatarMessage.value = '图片处理失败'
    avatarMessageType.value = 'error'
  }
}

// 手动保存头像
function handleSaveAvatar() {
  saving.value = true
  avatarMessage.value = ''

  saveAvatar(avatar.value)
  avatarMessage.value = '头像已保存'
  avatarMessageType.value = 'success'
  saving.value = false
}

// 修改密码
async function handleChangePassword() {
  if (newPassword.value !== confirmPassword.value) {
    passwordMessage.value = '两次输入的密码不一致'
    passwordMessageType.value = 'error'
    return
  }

  if (newPassword.value.length < 8) {
    passwordMessage.value = '新密码长度至少 8 位'
    passwordMessageType.value = 'error'
    return
  }

  changingPassword.value = true
  passwordMessage.value = ''

  const result = await changePassword(oldPassword.value, newPassword.value)

  if (result.success) {
    passwordMessage.value = '密码修改成功'
    passwordMessageType.value = 'success'
    oldPassword.value = ''
    newPassword.value = ''
    confirmPassword.value = ''
  } else {
    passwordMessage.value = result.message || '修改失败'
    passwordMessageType.value = 'error'
  }

  changingPassword.value = false
}

// 退出登录
async function handleLogout() {
  await logout()
  emit('close')
}
</script>

<template>
  <Teleport to="body">
    <Transition name="profile-panel">
      <div v-if="open" class="profile-overlay" @click.self="emit('close')">
        <div class="profile-panel">
          <!-- 头部 -->
          <div class="profile-header">
            <div class="profile-header-title">我的</div>
            <button class="profile-close" @click="emit('close')">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
                <line x1="18" y1="6" x2="6" y2="18" />
                <line x1="6" y1="6" x2="18" y2="18" />
              </svg>
            </button>
          </div>

          <!-- 内容 -->
          <div class="profile-body">
            <!-- 头像区域 -->
            <div class="profile-section">
              <div class="profile-section-title">头像</div>

              <div class="profile-avatar-section">
                <div class="profile-avatar-wrapper">
                  <div class="profile-avatar" :class="{ 'profile-avatar--has-image': avatar }">
                    <img v-if="avatar" :src="avatar" alt="头像" />
                    <span v-else class="profile-avatar-placeholder">
                      {{ (user?.username || '?')[0].toUpperCase() }}
                    </span>
                  </div>
                  <label class="profile-avatar-upload">
                    <input type="file" accept="image/*" @change="handleAvatarUpload" />
                    <span class="profile-avatar-upload-icon">📷</span>
                  </label>
                </div>
                <div class="profile-avatar-hint">点击更换头像（最大 500KB）</div>
              </div>

              <div v-if="avatarMessage" class="profile-message" :class="`profile-message--${avatarMessageType}`">
                {{ avatarMessage }}
              </div>

              <button
                class="profile-btn profile-btn--primary"
                :disabled="saving"
                @click="handleSaveAvatar"
              >
                {{ saving ? '保存中...' : '保存头像' }}
              </button>
            </div>

            <!-- 修改密码 -->
            <div class="profile-section">
              <div class="profile-section-title">修改密码</div>

              <div class="profile-field">
                <label class="profile-label">原密码</label>
                <input
                  v-model="oldPassword"
                  class="profile-input"
                  type="password"
                  placeholder="请输入原密码"
                />
              </div>

              <div class="profile-field">
                <label class="profile-label">新密码</label>
                <input
                  v-model="newPassword"
                  class="profile-input"
                  type="password"
                  placeholder="请输入新密码（至少8位）"
                />
              </div>

              <div class="profile-field">
                <label class="profile-label">确认密码</label>
                <input
                  v-model="confirmPassword"
                  class="profile-input"
                  type="password"
                  placeholder="请再次输入新密码"
                />
              </div>

              <div v-if="passwordMessage" class="profile-message" :class="`profile-message--${passwordMessageType}`">
                {{ passwordMessage }}
              </div>

              <button
                class="profile-btn profile-btn--primary"
                :disabled="changingPassword"
                @click="handleChangePassword"
              >
                {{ changingPassword ? '修改中...' : '修改密码' }}
              </button>
            </div>

            <!-- 退出登录 -->
            <div class="profile-section profile-section--logout">
              <button class="profile-btn profile-btn--danger" @click="handleLogout">
                退出登录
              </button>
            </div>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped lang="scss">
@use '@/styles/variables' as *;

.profile-overlay {
  position: fixed;
  inset: 0;
  z-index: $z-modal;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(4px);
}

.profile-panel {
  width: 400px;
  max-height: 85vh;
  background: linear-gradient(135deg, #1e1e3f, #2a2a5a);
  border-radius: $radius-lg;
  border: 1px solid rgba(255, 255, 255, 0.1);
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.5);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.profile-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: $space-4 $space-5;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.profile-header-title {
  font-size: $font-size-lg;
  font-weight: $font-weight-bold;
  color: white;
}

.profile-close {
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

.profile-body {
  flex: 1;
  overflow-y: auto;
  padding: $space-4 $space-5;
}

// 头像区域
.profile-avatar-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: $space-5;
}

.profile-avatar-wrapper {
  position: relative;
  margin-bottom: $space-2;
}

.profile-avatar {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: rgba(99, 102, 241, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  border: 2px solid rgba(255, 255, 255, 0.1);

  &--has-image {
    border-color: rgba(99, 102, 241, 0.5);
  }

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}

.profile-avatar-placeholder {
  font-size: 32px;
  font-weight: $font-weight-bold;
  color: $color-primary-light;
}

.profile-avatar-upload {
  position: absolute;
  bottom: 0;
  right: 0;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: $color-primary;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  border: 2px solid #1e1e3f;
  transition: transform $transition-fast;

  &:hover {
    transform: scale(1.1);
  }

  input {
    display: none;
  }
}

.profile-avatar-upload-icon {
  font-size: 14px;
}

.profile-avatar-hint {
  font-size: $font-size-xs;
  color: $color-text-tertiary;
}

// 区块
.profile-section {
  margin-bottom: $space-5;

  &--logout {
    margin-top: $space-4;
    padding-top: $space-4;
    border-top: 1px solid rgba(255, 255, 255, 0.08);
  }
}

.profile-section-title {
  font-size: $font-size-sm;
  font-weight: $font-weight-semibold;
  color: $color-text-secondary;
  margin-bottom: $space-3;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

// 表单字段
.profile-field {
  margin-bottom: $space-3;
}

.profile-label {
  display: block;
  font-size: $font-size-xs;
  color: $color-text-tertiary;
  margin-bottom: $space-1;
}

.profile-input {
  width: 100%;
  padding: $space-2 $space-3;
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: $radius-md;
  color: $color-text-primary;
  font-size: $font-size-base;
  font-family: $font-family;
  outline: none;
  transition: border-color $transition-fast;

  &:focus {
    border-color: $color-primary;
  }

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }

  &::placeholder {
    color: $color-text-tertiary;
  }
}

// 消息
.profile-message {
  padding: $space-2 $space-3;
  border-radius: $radius-md;
  font-size: $font-size-sm;
  margin-bottom: $space-3;

  &--success {
    background: rgba(34, 197, 94, 0.15);
    color: #4ade80;
  }

  &--error {
    background: rgba(239, 68, 68, 0.15);
    color: #f87171;
  }
}

// 按钮
.profile-btn {
  width: 100%;
  padding: $space-3;
  border-radius: $radius-md;
  border: none;
  font-size: $font-size-base;
  font-weight: $font-weight-semibold;
  font-family: $font-family;
  cursor: pointer;
  transition: all $transition-fast;

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }

  &--primary {
    background: linear-gradient(135deg, #6366f1, #8b5cf6);
    color: white;

    &:hover:not(:disabled) {
      transform: translateY(-1px);
      box-shadow: 0 4px 12px rgba(99, 102, 241, 0.4);
    }
  }

  &--danger {
    background: rgba(239, 68, 68, 0.15);
    color: #f87171;
    border: 1px solid rgba(239, 68, 68, 0.3);

    &:hover:not(:disabled) {
      background: rgba(239, 68, 68, 0.25);
    }
  }
}

// 动画
.profile-panel-enter-active {
  transition: all 0.3s ease;
}

.profile-panel-leave-active {
  transition: all 0.2s ease;
}

.profile-panel-enter-from {
  opacity: 0;

  .profile-panel {
    transform: translateY(20px) scale(0.95);
  }
}

.profile-panel-leave-to {
  opacity: 0;

  .profile-panel {
    transform: translateY(10px) scale(0.98);
  }
}
</style>
