<script setup lang="ts">
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

interface MenuItem {
  path: string; title: string; icon: string; admin?: boolean
}

const menuItems: MenuItem[] = [
  { path: '/dashboard', title: '仪表盘', icon: 'Odometer' },
  { path: '/resumes', title: '简历管理', icon: 'Document' },
  { path: '/interview/start', title: '模拟面试', icon: 'VideoCamera' },
  { path: '/questions', title: '题库浏览', icon: 'Collection' },
  { path: '/questions/wrong', title: '错题本', icon: 'WarningFilled' },
  { path: '/leaderboard', title: '排行榜', icon: 'Trophy' },
  { path: '/profile', title: '个人设置', icon: 'User' },
]

const adminItems: MenuItem[] = [
  { path: '/admin/stats', title: '数据概览', icon: 'DataAnalysis', admin: true },
  { path: '/admin/users', title: '用户管理', icon: 'UserFilled', admin: true },
  { path: '/admin/questions', title: '题库管理', icon: 'Edit', admin: true },
]

// 管理员：个人设置放到所有模块之下
const profileItem = menuItems.find(m => m.path === '/profile')!
const userNavItems = menuItems.filter(m => m.path !== '/profile')

const visibleItems = computed(() => {
  if (userStore.isAdmin) return [...userNavItems, ...adminItems, profileItem]
  return menuItems
})

const activeMenu = computed(() => {
  const path = route.path
  if (path.startsWith('/resumes')) return '/resumes'
  if (path.startsWith('/interview/') && path !== '/interview/start') return '/interview/start'
  if (path.startsWith('/questions/wrong')) return '/questions/wrong'
  if (path.startsWith('/questions')) return '/questions'
  return path
})

function navigateTo(path: string) {
  router.push(path)
}
</script>

<template>
  <aside class="sidebar">
    <div class="sidebar-brand" @click="navigateTo('/dashboard')">
      <div class="brand-icon">
        <svg width="28" height="28" viewBox="0 0 48 48" fill="none">
          <circle cx="24" cy="13" r="7" fill="#4F46E5"/>
          <path d="M8 44 L8 34 C8 26 16 23 24 23 C32 23 40 26 40 34 L40 44 Z" fill="#4F46E5"/>
          <polygon points="22.5,26 25.5,26 25,35 24,43 23,35" fill="white" opacity="0.85"/>
          <polygon points="21,23 27,23 25.5,27 22.5,27" fill="white" opacity="0.95"/>
          <line x1="24" y1="24" x2="24" y2="42" stroke="#E2E8F0" stroke-width="0.8" opacity="0.5"/>
          <polygon points="33,32 35,37 40,38 36,41 37,46 33,43 29,46 30,41 26,38 31,37" fill="white" opacity=".4"/>
          <polygon points="12,8 13.5,11.2 17,11.5 14,13.8 14.5,17 12,14.5 9.5,17 10,13.8 7,11.5 10.5,11.2" fill="#A5B4FC" opacity=".6"/>
        </svg>
      </div>
      <span class="brand-text">智面星</span>
    </div>

    <nav class="sidebar-nav">
      <div
        v-for="item in visibleItems"
        :key="item.path"
        class="nav-item"
        :class="{ active: activeMenu === item.path }"
        @click="navigateTo(item.path)"
      >
        <el-icon :size="18"><component :is="item.icon" /></el-icon>
        <span class="nav-label">{{ item.title }}</span>
        <div v-if="activeMenu === item.path" class="active-indicator" />
      </div>
    </nav>

    <div class="sidebar-footer">
      <div class="version-tag">v1.0.0</div>
    </div>
  </aside>
</template>

<style scoped>
.sidebar {
  position: fixed;
  top: 0;
  left: 0;
  bottom: 0;
  width: var(--sidebar-width);
  background: var(--sidebar-bg);
  display: flex;
  flex-direction: column;
  z-index: 100;
  user-select: none;
}

.sidebar-brand {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 20px 18px;
  cursor: pointer;
  transition: opacity var(--transition-fast);
}

.sidebar-brand:hover {
  opacity: 0.9;
}

.brand-icon {
  flex-shrink: 0;
}

.brand-text {
  font-size: 16px;
  font-weight: 700;
  color: var(--text-inverse);
  letter-spacing: -0.02em;
}

.sidebar-nav {
  flex: 1;
  padding: 8px 12px;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.nav-item {
  position: relative;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  border-radius: var(--radius-sm);
  color: var(--sidebar-text);
  cursor: pointer;
  transition: all var(--transition-fast);
  font-size: 13.5px;
  font-weight: 500;
}

.nav-item:hover {
  background: var(--sidebar-hover);
  color: var(--sidebar-text-active);
}

.nav-item.active {
  background: var(--sidebar-active);
  color: var(--sidebar-text-active);
}

.active-indicator {
  position: absolute;
  right: -12px;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 20px;
  background: var(--color-primary-light);
  border-radius: 0 2px 2px 0;
}

.nav-label {
  white-space: nowrap;
}

.sidebar-footer {
  padding: 12px 18px;
  border-top: 1px solid rgba(255,255,255,0.06);
}

.version-tag {
  font-size: 11px;
  color: var(--sidebar-text);
  opacity: 0.5;
}
</style>
