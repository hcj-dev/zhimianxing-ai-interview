<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { userApi } from '@/api/user'

const router = useRouter()
const formRef = ref()
const loading = ref(false)

const form = reactive({
  username: '',
  password: '',
  confirmPassword: '',
})

const validateConfirm = (_rule: unknown, value: string, callback: (err?: Error) => void) => {
  callback(value !== form.password ? new Error('两次输入的密码不一致') : undefined)
}

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度3-20个字符', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 30, message: '密码长度6-30个字符', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    { validator: validateConfirm, trigger: 'blur' },
  ],
}

async function handleRegister() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    await userApi.register({ username: form.username, password: form.password })
    ElMessage.success('注册成功，请登录')
    router.push('/login')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="auth-page">
    <div class="bg-decoration">
      <div class="bg-blob bg-blob--1" />
      <div class="bg-blob bg-blob--2" />
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
        <h1>创建账号</h1>
        <p>加入智面星，开启 AI 驱动的面试备战之旅</p>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" size="large">
        <el-form-item prop="username" label="用户名">
          <el-input v-model="form.username" placeholder="3-20个字符" />
        </el-form-item>
        <el-form-item prop="password" label="密码">
          <el-input v-model="form.password" type="password" placeholder="6-30个字符" show-password />
        </el-form-item>
        <el-form-item prop="confirmPassword" label="确认密码">
          <el-input v-model="form.confirmPassword" type="password" placeholder="再次输入密码"
            show-password @keyup.enter="handleRegister" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" class="auth-btn" @click="handleRegister">
            注 册
          </el-button>
        </el-form-item>
      </el-form>

      <div class="auth-footer">
        已有账号？<router-link to="/login">立即登录</router-link>
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
.bg-decoration { position: absolute; inset: 0; overflow: hidden; }
.bg-blob {
  position: absolute; border-radius: 50%; filter: blur(80px); opacity: 0.15;
}
.bg-blob--1 {
  width: 500px; height: 500px; background: var(--color-primary);
  top: -150px; right: -100px;
}
.bg-blob--2 {
  width: 400px; height: 400px; background: var(--color-accent);
  bottom: -100px; left: -80px; opacity: 0.1;
}
.auth-card {
  position: relative; width: 420px; padding: 44px 40px 36px;
  background: rgba(255,255,255,0.9); backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px); border: 1px solid rgba(0,0,0,0.06);
  border-radius: var(--radius-xl); box-shadow: var(--shadow-xl);
}
.auth-header { text-align: center; margin-bottom: 32px; }
.auth-logo { margin-bottom: 16px; display: inline-block; }
.auth-header h1 { font-size: 24px; font-weight: 700; color: var(--text-primary); margin-bottom: 6px; }
.auth-header p { font-size: 14px; color: var(--text-secondary); }
.auth-btn { width: 100%; height: 44px; font-size: 15px; font-weight: 600; border-radius: var(--radius-sm); }
.auth-footer { text-align: center; font-size: 14px; color: var(--text-secondary); margin-top: 4px; }
.auth-footer a { font-weight: 600; }
</style>
