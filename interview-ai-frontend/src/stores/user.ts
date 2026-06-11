import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { userApi } from '@/api/user'
import type { UserProfile } from '@/types/user'

export const useUserStore = defineStore('user', () => {
  const profile = ref<UserProfile | null>(null)
  const isLoggedIn = ref(!!localStorage.getItem('token'))
  const isAdmin = computed(() => profile.value?.role === 'ADMIN')

  async function fetchProfile() {
    try {
      const res = await userApi.getProfile()
      profile.value = res.data.data
    } catch { /* ignore */ }
  }

  function logout() {
    localStorage.removeItem('token')
    profile.value = null
    isLoggedIn.value = false
  }

  return { profile, isLoggedIn, isAdmin, fetchProfile, logout }
})
