<template>
  <section class="system-workbench">
    <aside class="system-navigation" aria-label="系统管理目录">
      <div class="system-navigation-title">系统管理</div>
      <nav class="system-navigation-list">
        <RouterLink
          v-for="item in navigationItems"
          :key="item.code"
          :to="item.path"
          class="system-navigation-item"
          :class="{ 'system-navigation-item-active': item.code === page }"
        >
          <span class="system-navigation-marker" />
          <span>{{ item.title }}</span>
        </RouterLink>
      </nav>
    </aside>
    <main class="system-content">
      <component :is="currentView" v-bind="currentProps" />
    </main>
  </section>
</template>

<script setup>
import { computed } from 'vue'
import { RouterLink } from 'vue-router'
import CredentialsView from '../views/CredentialsView.vue'
import DashboardView from '../views/DashboardView.vue'
import PlaceholderView from '../views/PlaceholderView.vue'
import RolesView from '../views/RolesView.vue'
import UsersView from '../views/UsersView.vue'

const props = defineProps({
  page: {
    type: String,
    default: 'dashboard'
  }
})

const navigationItems = [
  { code: 'dashboard', path: '/plugins/system', title: '控制台' },
  { code: 'users', path: '/plugins/system/users', title: '用户管理' },
  { code: 'roles', path: '/plugins/system/roles', title: '角色管理' },
  { code: 'permissions', path: '/plugins/system/permissions', title: '权限管理' },
  { code: 'plugins', path: '/plugins/system/plugins', title: '插件管理' },
  { code: 'credentials', path: '/plugins/system/credentials', title: '接入凭证' },
  { code: 'settings', path: '/plugins/system/settings', title: '系统设置' }
]

const pageRegistry = {
  dashboard: { component: DashboardView, props: {} },
  users: { component: UsersView, props: {} },
  roles: { component: RolesView, props: {} },
  permissions: {
    component: PlaceholderView,
    props: {
      title: '权限管理',
      description: '系统权限配置页已迁入 system 插件，后续可继续接入更细粒度功能。'
    }
  },
  plugins: {
    component: PlaceholderView,
    props: {
      title: '插件管理',
      description: '系统插件管理入口已纳入 system 插件页面。'
    }
  },
  credentials: { component: CredentialsView, props: {} },
  settings: {
    component: PlaceholderView,
    props: {
      title: '系统设置',
      description: '系统设置页已迁入 system 插件，后续可继续补充真实配置项。'
    }
  }
}

const currentPage = computed(() => pageRegistry[props.page] ?? pageRegistry.dashboard)
const currentView = computed(() => currentPage.value.component)
const currentProps = computed(() => currentPage.value.props)
</script>

<style scoped>
.system-workbench {
  display: grid;
  grid-template-columns: 208px minmax(0, 1fr);
  min-height: calc(100vh - 40px);
  background: var(--shell-app-bg);
}

.system-navigation {
  padding: 18px 12px;
  border-right: 1px solid var(--shell-panel-border);
  background: var(--shell-tool-surface-muted);
}

.system-navigation-title {
  padding: 0 10px 12px;
  color: var(--shell-tool-subtle-text);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.08em;
}

.system-navigation-list {
  display: grid;
  gap: 2px;
}

.system-navigation-item {
  display: flex;
  align-items: center;
  gap: 8px;
  min-height: 32px;
  padding: 0 10px;
  color: var(--shell-text-secondary);
  text-decoration: none;
  font-size: 13px;
}

.system-navigation-item:hover {
  background: var(--shell-tool-hover);
  color: var(--shell-text-primary);
}

.system-navigation-item-active {
  background: var(--shell-module-active-bg);
  color: var(--shell-text-primary);
  font-weight: 600;
}

.system-navigation-marker {
  width: 5px;
  height: 5px;
  background: currentColor;
  opacity: 0.65;
}

.system-content {
  min-width: 0;
  padding: 20px 24px 28px;
}

@media (max-width: 760px) {
  .system-workbench {
    grid-template-columns: 1fr;
  }

  .system-navigation {
    padding: 8px 12px;
    border-right: 0;
    border-bottom: 1px solid var(--shell-panel-border);
  }

  .system-navigation-title {
    display: none;
  }

  .system-navigation-list {
    display: flex;
    overflow-x: auto;
  }

  .system-navigation-item {
    flex: 0 0 auto;
  }

  .system-content {
    padding: 16px;
  }
}
</style>
