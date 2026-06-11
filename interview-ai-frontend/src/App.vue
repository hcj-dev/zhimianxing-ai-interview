<script setup lang="ts">
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
if (userStore.isLoggedIn) {
  userStore.fetchProfile()
}
</script>

<template>
  <router-view />
</template>
