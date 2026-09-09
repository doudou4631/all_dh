<script setup>
import { useRoute, useRouter } from 'vue-router'
import { authState, signOut } from '../stores/authStore'

defineProps({
  title: { type: String, default: 'TDX 二次申诉' },
  subtitle: { type: String, default: '' },
  active: { type: String, default: '' },
})

const router = useRouter()
const route = useRoute()

async function handleLogout() {
  await signOut()
  router.push('/login')
}
</script>

<template>
  <div class="app-shell">
    <header class="app-header">
      <div>
        <h1>{{ title }}</h1>
        <p v-if="subtitle" class="subtitle">{{ subtitle }}</p>
      </div>
      <div class="header-actions">
        <span v-if="authState.user" class="user-chip">{{ authState.user.username || authState.user.name }}</span>
        <button v-if="route.path !== '/login'" class="btn btn-soft" type="button" @click="router.push('/appeal')">申诉页</button>
        <button v-if="authState.token" class="btn btn-soft" type="button" @click="handleLogout">退出</button>
        <slot name="actions" />
      </div>
    </header>
    <main class="app-main">
      <slot />
    </main>
  </div>
</template>

<style scoped>
.app-shell { min-height: 100vh; background: #f4f6fb; }
.app-header {
  display: flex; justify-content: space-between; align-items: center;
  padding: 16px 24px; background: #fff; border-bottom: 1px solid #e5e7eb;
}
.app-header h1 { margin: 0; font-size: 20px; }
.subtitle { margin: 4px 0 0; color: #64748b; font-size: 13px; }
.header-actions { display: flex; gap: 8px; align-items: center; }
.user-chip { font-size: 13px; color: #475569; }
.app-main { padding: 20px 24px; max-width: 960px; margin: 0 auto; }
.btn { border: 1px solid #cbd5e1; background: #fff; padding: 6px 12px; border-radius: 8px; cursor: pointer; }
.btn-soft:hover { background: #f8fafc; }
</style>
