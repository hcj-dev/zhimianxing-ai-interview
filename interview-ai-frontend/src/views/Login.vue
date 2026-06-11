<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { User, Lock } from '@element-plus/icons-vue'
import { userApi } from '@/api/user'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref()
const loading = ref(false)

const form = reactive({ username: '', password: '' })

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

async function handleLogin() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    const res = await userApi.login(form)
    const { token } = res.data.data
    localStorage.setItem('token', token)
    userStore.isLoggedIn = true
    await userStore.fetchProfile()
    router.push('/dashboard')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="auth-page">
    <!-- Background decoration -->
    <div class="bg-decoration">
      <div class="bg-blob bg-blob--1" />
      <div class="bg-blob bg-blob--2" />
      <div class="bg-blob bg-blob--3" />
    </div>

    <div class="auth-card">
      <div class="auth-header">
        <div class="auth-logo">
          <svg width="36" height="36" viewBox="0 0 48 48" fill="none">
            <circle cx="24" cy="13" r="7" fill="#4F46E5"/>
            <path d="M8 44 L8 34 C8 26 16 23 24 23 C32 23 40 26 40 34 L40 44 Z" fill="#4F46E5"/>
            <polygon points="22.5,26 25.5,26 25,35 24,43 23,35" fill="white" opacity="0.85"/>
            <polygon points="21,23 27,23 25.5,27 22.5,27" fill="white" opacity="0.95"/>
            <line x1="24" y1="24" x2="24" y2="42" stroke="#E2E8F0" stroke-width="0.8" opacity="0.5"/>
            <polygon points="33,32 35,37 40,38 36,41 37,46 33,43 29,46 30,41 26,38 31,37" fill="white" opacity=".4"/>
            <polygon points="12,8 13.5,11.2 17,11.5 14,13.8 14.5,17 12,14.5 9.5,17 10,13.8 7,11.5 10.5,11.2" fill="#A5B4FC" opacity=".6"/>
          </svg>
        </div>
        <h1>欢迎回来</h1>
        <p>登录智面星，继续你的面试备战</p>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" size="large">
        <el-form-item prop="username" label="用户名">
          <el-input v-model="form.username" placeholder="请输入用户名" :prefix-icon="User" />
        </el-form-item>
        <el-form-item prop="password" label="密码">
          <el-input v-model="form.password" type="password" placeholder="请输入密码"
            show-password :prefix-icon="Lock" @keyup.enter="handleLogin" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" class="auth-btn" @click="handleLogin">
            登 录
          </el-button>
        </el-form-item>
      </el-form>

      <div class="auth-footer">
        还没有账号？<router-link to="/register">立即注册</router-link>
      </div>
    </div>
  </div>
</template>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #F1F5F9;
  position: relative;
  overflow: hidden;
}

/* Decorative gradient blobs */
.bg-decoration {
  position: absolute;
  inset: 0;
  overflow: hidden;
}

.bg-blob {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.15;
}

.bg-blob--1 {
  width: 500px;
  height: 500px;
  background: var(--color-primary);
  top: -150px;
  right: -100px;
}

.bg-blob--2 {
  width: 400px;
  height: 400px;
  background: var(--color-accent);
  bottom: -100px;
  left: -80px;
  opacity: 0.1;
}

.bg-blob--3 {
  width: 300px;
  height: 300px;
  background: #818CF8;
  top: 40%;
  left: 50%;
  transform: translate(-50%, -50%);
  opacity: 0.08;
}

.auth-card {
  position: relative;
  width: 420px;
  padding: 44px 40px 36px;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border: 1px solid rgba(0, 0, 0, 0.06);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-xl);
}

.auth-header {
  text-align: center;
  margin-bottom: 32px;
}

.auth-logo {
  margin-bottom: 16px;
  display: inline-block;
}

.auth-header h1 {
  font-size: 24px;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 6px;
}

.auth-header p {
  font-size: 14px;
  color: var(--text-secondary);
}

.auth-btn {
  width: 100%;
  height: 44px;
  font-size: 15px;
  font-weight: 600;
  border-radius: var(--radius-sm);
}

.auth-footer {
  text-align: center;
  font-size: 14px;
  color: var(--text-secondary);
  margin-top: 4px;
}

.auth-footer a {
  font-weight: 600;
}
</style>
