<template>
  <RouterView v-if="isAuthLayout" />
  <div v-else class="shell">
    <aside class="sidebar">
      <div class="brand">
        <p class="eyebrow">Pyin Config Center</p>
        <h1>Pyin</h1>
        <p class="subtitle">{{ currentModule?.name ?? '系统模块' }}</p>
      </div>
      <nav class="menu-list">
        <template v-for="item in menus" :key="item.code">
          <div v-if="item.children?.length" class="menu-directory">
            <button
              type="button"
              class="menu-link menu-directory-trigger"
              :class="{ 'menu-link-active': isDirectoryActive(item) }"
              @click="toggleDirectory(item)"
            >
              <component :is="resolveMenuIcon(item)" :size="18" />
              <span>{{ item.name }}</span>
              <ChevronDown
                :size="16"
                class="menu-directory-arrow"
                :class="{ 'menu-directory-arrow-open': isDirectoryExpanded(item) }"
              />
            </button>
            <div v-show="isDirectoryExpanded(item)" class="menu-children">
              <component
                v-for="child in item.children"
                :key="child.code"
                :is="menuComponent(child)"
                :to="child.type === 'ROUTE' ? child.path : undefined"
                :href="child.type === 'LINK' ? navigationStore.resolveMenuTarget(child) : undefined"
                class="menu-link menu-child-link"
                :class="{ 'menu-link-active': isMenuActive(child) }"
                active-class="menu-link-active"
                @click.prevent="handleMenuClick(child)"
              >
                <component v-if="child.icon" :is="resolveIconComponent(child.icon)" :size="16" />
                <span>{{ child.name }}</span>
              </component>
            </div>
          </div>
          <component
            v-else
            :is="menuComponent(item)"
            :to="item.type === 'ROUTE' ? item.path : undefined"
            :href="item.type === 'LINK' ? navigationStore.resolveMenuTarget(item) : undefined"
            class="menu-link"
            :class="{ 'menu-link-active': isMenuActive(item) }"
            active-class="menu-link-active"
            @click.prevent="handleMenuClick(item)"
          >
            <component :is="resolveMenuIcon(item)" :size="18" />
            <span>{{ item.name }}</span>
          </component>
        </template>
      </nav>
    </aside>
    <main class="workspace">
      <header class="topbar">
        <div class="module-bar">
          <button
            v-for="module in modules"
            :key="module.code"
            type="button"
            class="module-tab"
            :class="{ 'module-tab-active': module.code === currentModule?.code }"
            @click="selectModule(module.code)"
          >
            <component :is="resolveIconComponent(module.icon)" :size="18" />
            <span>{{ module.name }}</span>
          </button>
        </div>
        <div class="topbar-actions">
          <button type="button" class="theme-toggle" @click="toggleTheme">
            <component :is="themeIcon" :size="18" />
            <span>{{ themeLabel }}</span>
          </button>
          <div class="account-card">
            <div>
              <strong>{{ currentUser?.displayName ?? '管理员' }}</strong>
              <p>{{ currentUser?.username ?? 'admin' }}</p>
            </div>
            <el-button type="primary" plain @click="handleLogout">退出登录</el-button>
          </div>
        </div>
      </header>
      <section class="content">
        <RouterView />
      </section>
    </main>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { RouterLink, RouterView, useRoute, useRouter } from 'vue-router'
import { ChevronDown, FolderTree, MoonStar, SunMedium } from 'lucide-vue-next'
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
const menus = computed(() => navigationStore.currentMenus)
const currentUser = computed(() => authStore.currentUser)
const isAuthLayout = computed(() => route.meta?.layout === 'auth')
const themeIcon = computed(() => (themeStore.activeTheme === 'dark' ? SunMedium : MoonStar))
const themeLabel = computed(() => (themeStore.activeTheme === 'dark' ? '浅色模式' : '深色模式'))
const expandedDirectories = ref({})

watch(
  () => route.fullPath,
  () => navigationStore.syncCurrentModule(route),
  { immediate: true }
)

watch(
  [menus, () => route.fullPath],
  () => {
    const nextExpanded = { ...expandedDirectories.value }

    for (const item of menus.value) {
      if (item.children?.length && isDirectoryActive(item)) {
        nextExpanded[item.code] = true
      }
    }

    expandedDirectories.value = nextExpanded
  },
  { immediate: true }
)

async function selectModule(moduleCode) {
  await navigationStore.activateModule(moduleCode, router)
}

function menuComponent(menu) {
  return menu.type === 'ROUTE' ? RouterLink : 'a'
}

function resolveMenuIcon(menu) {
  if (!menu.icon && menu.children?.length) {
    return FolderTree
  }

  return resolveIconComponent(menu.icon)
}

function isMenuActive(menu) {
  if (menu.type === 'ROUTE') {
    return route.path === menu.path
  }

  if (menu.type === 'LINK') {
    return route.path === '/__link-viewer' && route.query?.menuCode === menu.code
  }

  return false
}

function isDirectoryActive(menu) {
  return (menu.children ?? []).some((child) => isMenuActive(child))
}

function isDirectoryExpanded(menu) {
  return expandedDirectories.value[menu.code] ?? false
}

function toggleDirectory(menu) {
  expandedDirectories.value = {
    ...expandedDirectories.value,
    [menu.code]: !isDirectoryExpanded(menu)
  }
}

async function handleMenuClick(menu) {
  const target = navigationStore.resolveMenuTarget(menu)
  if (target) {
    await router.push(target)
  }
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
.shell {
  display: grid;
  grid-template-columns: 240px 1fr;
  min-height: 100vh;
  background: var(--shell-app-bg);
  color: var(--shell-text-primary);
}

.sidebar {
  padding: 24px 20px;
  background: var(--shell-sidebar-bg);
  border-right: 1px solid var(--shell-sidebar-border);
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.brand h1 {
  margin: 0;
  font-size: 28px;
}

.eyebrow {
  margin: 0 0 8px;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  font-size: 11px;
  color: var(--shell-accent-strong);
}

.subtitle {
  margin: 8px 0 0;
  color: var(--shell-text-secondary);
}

.menu-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.menu-link {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  border-radius: 14px;
  color: var(--shell-text-secondary);
  text-decoration: none;
  transition: background-color 0.2s ease, color 0.2s ease, transform 0.2s ease;
}

.menu-link:hover {
  background: var(--shell-menu-hover);
  color: var(--shell-text-primary);
  transform: translateX(2px);
}

.menu-link-active {
  background: var(--shell-menu-active-bg);
  color: var(--shell-menu-active-text);
}

.menu-directory {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.menu-directory-trigger {
  width: 100%;
  border: 0;
  cursor: pointer;
  background: transparent;
  font: inherit;
  text-align: left;
}

.menu-directory-arrow {
  margin-left: auto;
  transition: transform 0.2s ease;
}

.menu-directory-arrow-open {
  transform: rotate(180deg);
}

.menu-children {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding-left: 18px;
}

.menu-child-link {
  padding: 10px 12px;
  font-size: 13px;
}

.workspace {
  padding: 24px 28px 28px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 18px;
  border-radius: 22px;
  background: var(--shell-module-bar-bg);
  border: 1px solid var(--shell-panel-border);
  box-shadow: var(--shell-panel-shadow);
}

.module-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.topbar-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.module-tab,
.theme-toggle {
  border: 1px solid var(--shell-button-border);
  background: var(--shell-button-bg);
  color: var(--shell-text-primary);
  border-radius: 999px;
  padding: 10px 14px;
  display: inline-flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  transition: transform 0.2s ease, border-color 0.2s ease, background-color 0.2s ease;
}

.module-tab:hover,
.theme-toggle:hover {
  transform: translateY(-1px);
}

.module-tab-active {
  background: var(--shell-module-active-bg);
  border-color: var(--shell-module-active-border);
}

.account-card {
  display: inline-flex;
  align-items: center;
  gap: 16px;
  padding: 8px 8px 8px 16px;
  border-radius: 18px;
  background: var(--shell-button-bg);
  border: 1px solid var(--shell-button-border);
}

.account-card strong {
  display: block;
  font-size: 14px;
}

.account-card p {
  margin: 4px 0 0;
  font-size: 12px;
  color: var(--shell-text-secondary);
}

.content {
  min-height: 0;
  flex: 1;
}

@media (max-width: 960px) {
  .shell {
    grid-template-columns: 1fr;
  }

  .sidebar {
    border-right: none;
    border-bottom: 1px solid var(--shell-sidebar-border);
  }

  .workspace {
    padding-top: 20px;
  }

  .topbar {
    flex-direction: column;
    align-items: stretch;
  }

  .topbar-actions {
    flex-direction: column;
    align-items: stretch;
  }

  .theme-toggle {
    justify-content: center;
  }

  .account-card {
    justify-content: space-between;
  }
}
</style>
