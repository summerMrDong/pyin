<template>
  <RouterView v-if="isAuthLayout" />
  <div v-else class="desktop-shell">
    <header class="application-bar">
      <div class="application-bar-leading">
        <div class="brand" aria-label="Pyin 配置中心">
          <span class="brand-mark">P</span>
          <span class="brand-name">Pyin</span>
        </div>
        <nav class="plugin-tabs" aria-label="插件工作台">
          <button
            v-for="module in modules"
            :key="module.code"
            type="button"
            class="plugin-tab"
            :class="{
              'module-tab-active': module.code === currentModule?.code,
              'plugin-tab-loading': isModuleLoading(module)
            }"
            :aria-busy="isModuleLoading(module)"
            @click="selectModule(module.code)"
          >
            <span v-if="isModuleLoading(module)" class="plugin-tab-spinner" aria-hidden="true" />
            <component v-else :is="resolveIconComponent(module.icon)" :size="18" />
            <span>{{ module.name }}</span>
          </button>
        </nav>
      </div>
      <div class="application-actions">
        <button type="button" class="application-action theme-toggle" :title="themeLabel" @click="toggleTheme">
          <component :is="themeIcon" :size="17" />
          <span>{{ themeLabel }}</span>
        </button>
        <div class="account-summary">
          <strong>{{ currentUser?.username ?? 'admin' }}</strong>
        </div>
        <button type="button" class="application-action logout-action" @click="handleLogout">退出</button>
      </div>
    </header>
    <main class="plugin-workspace">
      <RouterView />
      <Transition name="workspace-loading">
        <div v-if="isCurrentModuleLoading" class="workspace-loading" role="status" aria-live="polite">
          <span class="workspace-loading-spinner" aria-hidden="true" />
          <span>正在加载 {{ currentModule?.name }} 工作区…</span>
        </div>
      </Transition>
    </main>
  </div>
</template>

<script setup>
import { computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { RouterView, useRoute, useRouter } from 'vue-router'
import { MoonStar, SunMedium } from 'lucide-vue-next'
import { resolveIconComponent } from './plugins/iconMap'
import { useAuthStore } from './stores/auth'
import { useShellNavigationStore } from './stores/shellNavigation'
import { useThemeStore } from './stores/theme'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const navigationStore = useShellNavigationStore()
const themeStore = useThemeStore()

const modules = computed(() => navigationStore.modules)
const currentModule = computed(() => navigationStore.currentModule)
const currentUser = computed(() => authStore.currentUser)
const isAuthLayout = computed(() => route.meta?.layout === 'auth')
const themeIcon = computed(() => (themeStore.activeTheme === 'dark' ? SunMedium : MoonStar))
const themeLabel = computed(() => (themeStore.activeTheme === 'dark' ? '浅色模式' : '深色模式'))
const isCurrentModuleLoading = computed(() => currentModule.value && isModuleLoading(currentModule.value))

watch(
  () => route.fullPath,
  () => navigationStore.syncCurrentModule(route),
  { immediate: true }
)

async function selectModule(moduleCode) {
  await navigationStore.activateModule(moduleCode, router)
}

function isModuleLoading(module) {
  return Boolean(module?.pluginId && navigationStore.loadingPluginIds[module.pluginId])
}

function toggleTheme() {
  themeStore.toggleTheme()
}

async function handleLogout() {
  await authStore.logout()
  ElMessage.success('已退出登录')
  await router.replace('/login')
}
</script>

<style scoped>
.desktop-shell {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  background: var(--shell-app-bg);
  color: var(--shell-text-primary);
}

.application-bar {
  position: sticky;
  top: 0;
  z-index: 20;
  min-height: 40px;
  padding: 0 12px;
  background: var(--shell-module-bar-bg);
  border-bottom: 1px solid var(--shell-panel-border);
  display: flex;
  align-items: stretch;
  justify-content: space-between;
  gap: 16px;
}

.application-bar-leading {
  min-width: 0;
  display: flex;
  align-items: stretch;
}

.brand {
  flex: 0 0 auto;
  min-width: 100px;
  padding: 0 16px 0 4px;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  border-right: 1px solid var(--shell-panel-border);
}

.brand-mark {
  display: inline-grid;
  place-items: center;
  width: 20px;
  height: 20px;
  background: var(--shell-accent);
  color: #fff;
  font-size: 13px;
  font-weight: 700;
  line-height: 1;
}

.brand-name {
  font-size: 14px;
  font-weight: 650;
  letter-spacing: 0.01em;
}

.plugin-tabs {
  min-width: 0;
  display: flex;
  align-items: stretch;
  overflow-x: auto;
  overflow-y: hidden;
  scrollbar-width: thin;
}

.plugin-tab {
  position: relative;
  flex: 0 0 auto;
  display: flex;
  align-items: center;
  gap: 8px;
  min-height: 40px;
  padding: 0 12px;
  border: 0;
  border-right: 1px solid transparent;
  border-left: 1px solid transparent;
  background: transparent;
  color: var(--shell-text-secondary);
  cursor: pointer;
  font-size: 13px;
  white-space: nowrap;
  transition: background-color 0.15s ease, color 0.15s ease;
}

.plugin-tab:hover {
  background: var(--shell-tool-hover);
  color: var(--shell-text-primary);
}

.plugin-tab-loading {
  cursor: progress;
}

.plugin-tab-spinner,
.workspace-loading-spinner {
  display: inline-block;
  width: 14px;
  height: 14px;
  flex: 0 0 auto;
  border: 2px solid currentColor;
  border-right-color: transparent;
  border-radius: 50%;
  animation: plugin-loading-spin 0.7s linear infinite;
}

.module-tab-active {
  background: var(--shell-module-active-bg);
  color: var(--shell-text-primary);
  border-right-color: var(--shell-panel-border);
  border-left-color: var(--shell-panel-border);
}

.module-tab-active::after {
  position: absolute;
  right: 0;
  bottom: 0;
  left: 0;
  height: 2px;
  background: var(--shell-accent);
  content: '';
}

.application-actions {
  flex: 0 0 auto;
  display: flex;
  align-items: center;
  gap: 4px;
}

.application-action {
  min-height: 30px;
  padding: 0 9px;
  border: 0;
  background: transparent;
  color: var(--shell-text-primary);
  display: inline-flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  font-size: 12px;
  transition: background-color 0.15s ease;
}

.application-action:hover {
  background: var(--shell-tool-hover);
}

.account-summary {
  display: inline-flex;
  align-items: flex-end;
  padding: 0 8px;
}

.account-summary strong {
  display: block;
  font-size: 12px;
}

.plugin-workspace {
  position: relative;
  flex: 1;
  min-width: 0;
}

.workspace-loading {
  position: absolute;
  inset: 0;
  z-index: 10;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  background: color-mix(in srgb, var(--shell-app-bg) 82%, transparent);
  color: var(--shell-text-secondary);
  font-size: 13px;
  backdrop-filter: blur(2px);
}

.workspace-loading-spinner {
  width: 18px;
  height: 18px;
  color: var(--shell-accent);
}

.workspace-loading-enter-active,
.workspace-loading-leave-active {
  transition: opacity 0.16s ease;
}

.workspace-loading-enter-from,
.workspace-loading-leave-to {
  opacity: 0;
}

@keyframes plugin-loading-spin {
  to {
    transform: rotate(360deg);
  }
}

@media (max-width: 960px) {
  .application-bar {
    gap: 8px;
  }

  .brand {
    min-width: auto;
    padding-right: 10px;
  }

  .brand-name,
  .theme-toggle span {
    display: none;
  }

  .account-summary {
    padding: 0 4px;
  }

  .logout-action {
    padding-right: 0;
  }
}
</style>
