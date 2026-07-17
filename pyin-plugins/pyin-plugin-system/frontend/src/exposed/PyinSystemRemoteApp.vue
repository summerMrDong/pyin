<template>
  <component :is="currentView" v-bind="currentProps" />
</template>

<script setup>
import { computed } from 'vue'
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

const pageRegistry = {
  dashboard: { component: DashboardView, props: {} },
  users: { component: UsersView, props: {} },
  roles: { component: RolesView, props: {} },
  permissions: {
    component: PlaceholderView,
    props: {
      title: '权限管理',
      description: '系统权限配置页已迁入 pyin-system 插件，后续可继续接入更细粒度功能。'
    }
  },
  plugins: {
    component: PlaceholderView,
    props: {
      title: '插件管理',
      description: '系统插件管理入口已纳入 pyin-system 插件页面。'
    }
  },
  credentials: { component: CredentialsView, props: {} },
  settings: {
    component: PlaceholderView,
    props: {
      title: '系统设置',
      description: '系统设置页已迁入 pyin-system 插件，后续可继续补充真实配置项。'
    }
  }
}

const currentPage = computed(() => pageRegistry[props.page] ?? pageRegistry.dashboard)
const currentView = computed(() => currentPage.value.component)
const currentProps = computed(() => currentPage.value.props)
</script>
