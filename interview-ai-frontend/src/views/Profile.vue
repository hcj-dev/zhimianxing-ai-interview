<script setup lang="ts">
import { reactive, ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { userApi } from '@/api/user'

const userStore = useUserStore()
const saving = ref(false)
const changingPassword = ref(false)
const uploadingAvatar = ref(false)
const avatarVersion = ref(0)

// 头像 src：走公开 API（无需 Token），浏览器原生 <img> 直接加载
const avatarSrc = computed(() => {
  const uid = userStore.profile?.id
  if (!uid || !userStore.profile?.avatarUrl) return undefined
  return `/api/v1/user/avatar/${uid}?v=${avatarVersion.value}`
})

const form = reactive({
  nickname: userStore.profile?.nickname || '',
  email: userStore.profile?.email || '',
})

const pwdForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})

async function handleSave() {
  saving.value = true
  try {
    await userApi.updateProfile(form.nickname, form.email)
    await userStore.fetchProfile()
    ElMessage.success('保存成功')
  } finally {
    saving.value = false
  }
}

async function handleToggleRanking() {
  try {
    await userApi.toggleRankingParticipation()
    await userStore.fetchProfile()
    ElMessage.success(userStore.profile?.participateRanking === 1 ? '已参与排行榜' : '已退出排行榜')
  } catch { /* ignore */ }
}

async function handleChangePassword() {
  if (!pwdForm.oldPassword) {
    ElMessage.warning('请输入旧密码')
    return
  }
  if (!pwdForm.newPassword) {
    ElMessage.warning('请输入新密码')
    return
  }
  if (pwdForm.newPassword.length < 6) {
    ElMessage.warning('新密码长度不能少于6位')
    return
  }
  if (pwdForm.newPassword === pwdForm.oldPassword) {
    ElMessage.warning('新密码不能与旧密码相同')
    return
  }
  if (pwdForm.newPassword !== pwdForm.confirmPassword) {
    ElMessage.warning('两次输入的新密码不一致')
    return
  }
  changingPassword.value = true
  try {
    await userApi.changePassword(pwdForm.oldPassword, pwdForm.newPassword)
    ElMessage.success('密码修改成功，请妥善保管')
    pwdForm.oldPassword = ''
    pwdForm.newPassword = ''
    pwdForm.confirmPassword = ''
  } catch {
    // 后端会返回具体错误（旧密码错误等）
  } finally {
    changingPassword.value = false
  }
}

function triggerAvatarUpload() {
  const input = document.createElement('input')
  input.type = 'file'
  input.accept = 'image/png, image/jpeg, image/gif, image/webp'
  input.onchange = async (e: Event) => {
    const file = (e.target as HTMLInputElement).files?.[0]
    if (!file) return
    if (file.size > 2 * 1024 * 1024) {
      ElMessage.warning('头像大小不能超过 2MB')
      return
    }
    uploadingAvatar.value = true
    try {
      const res = await userApi.uploadAvatar(file)
      // 即时更新本地 store，避免 fetchProfile 异步导致的闪现
      if (userStore.profile) {
        userStore.profile.avatarUrl = res.data.data.avatarUrl
      }
      avatarVersion.value++  // 刷新 img src 避免浏览器缓存
      await userStore.fetchProfile()
      ElMessage.success('头像更新成功')
    } catch { /* ignore */ }
    finally { uploadingAvatar.value = false }
  }
  input.click()
}
</script>

<template>
  <div class="page-container" style="max-width: 640px;">
    <div class="page-header">
      <h2>个人设置</h2>
      <p>修改个人信息</p>
    </div>

    <el-card>
      <div class="avatar-section">
        <div class="avatar-wrapper" @click="triggerAvatarUpload" :class="{ uploading: uploadingAvatar }">
          <el-avatar :size="72" :src="avatarSrc">
            <el-icon :size="28"><UserFilled /></el-icon>
          </el-avatar>
          <div class="avatar-overlay">
            <el-icon :size="20"><Camera /></el-icon>
          </div>
        </div>
        <div>
          <div class="username-display">{{ userStore.profile?.username }}</div>
          <div class="role-badge">普通用户</div>
        </div>
      </div>

      <el-form :model="form" label-position="top" style="margin-top: 24px;">
        <el-form-item label="昵称">
          <el-input v-model="form.nickname" placeholder="给自己起个昵称" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="form.email" placeholder="选填，用于找回密码" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="saving" @click="handleSave">保存修改</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card style="margin-top: 16px;">
      <template #header>账号信息</template>
      <div class="info-row">
        <span class="info-label">用户名</span>
        <span class="info-value">{{ userStore.profile?.username }}</span>
      </div>
      <div class="info-row">
        <span class="info-label">注册时间</span>
        <span class="info-value">{{ userStore.profile?.createdAt || '--' }}</span>
      </div>
      <div class="info-row">
        <span class="info-label">账号状态</span>
        <el-tag type="success" size="small" effect="light">正常</el-tag>
      </div>
      <div class="info-row">
        <span class="info-label">参与排行</span>
        <el-switch
          :model-value="userStore.profile?.participateRanking === 1"
          @change="handleToggleRanking"
          size="small"
        />
        <span style="margin-left: 8px; font-size: 12px; color: var(--text-muted);">
          {{ userStore.profile?.participateRanking === 1 ? '正在参与排行榜' : '已退出排行榜' }}
        </span>
      </div>
    </el-card>

    <el-card style="margin-top: 16px;">
      <template #header>修改密码</template>
      <el-form :model="pwdForm" label-position="top" style="max-width: 360px;">
        <el-form-item label="旧密码">
          <el-input v-model="pwdForm.oldPassword" type="password" show-password placeholder="输入当前密码" />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="至少6位" />
        </el-form-item>
        <el-form-item label="确认新密码">
          <el-input v-model="pwdForm.confirmPassword" type="password" show-password placeholder="再次输入新密码" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="changingPassword" @click="handleChangePassword">修改密码</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<style scoped>
.avatar-section {
  display: flex;
  align-items: center;
  gap: 16px;
}

.avatar-wrapper {
  position: relative;
  border-radius: 50%;
  cursor: pointer;
  flex-shrink: 0;
  transition: opacity 0.2s;
}
.avatar-wrapper:hover .avatar-overlay {
  opacity: 1;
}
.avatar-wrapper.uploading {
  opacity: 0.6;
  pointer-events: none;
}

.avatar-overlay {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.2s;
  color: #fff;
}

.username-display {
  font-size: 18px;
  font-weight: 700;
}

.role-badge {
  font-size: 12px;
  color: var(--text-muted);
  margin-top: 4px;
}

.info-row {
  display: flex;
  align-items: center;
  padding: 10px 0;
  border-bottom: 1px solid var(--border-light);
}

.info-label {
  width: 100px;
  font-size: 13px;
  color: var(--text-secondary);
}

.info-value {
  font-size: 13px;
  font-weight: 500;
}
</style>
