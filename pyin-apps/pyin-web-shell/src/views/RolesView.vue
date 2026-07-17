<template>
  <section class="console-page">
    <div class="page-header">
      <div>
        <h2>角色管理</h2>
        <p>维护角色定义，并统一配置权限、资源与用户绑定关系。</p>
      </div>
      <div class="page-header-meta">
        <span>当前 {{ roles.length }} 个角色</span>
      </div>
    </div>

    <el-card shadow="never" class="toolbar-card">
      <div class="toolbar-grid roles-toolbar">
        <div class="toolbar-title">
          <span>筛选条件</span>
        </div>
        <el-input v-model="filters.code" placeholder="角色编码" clearable @keyup.enter="loadRoles" />
        <el-input v-model="filters.name" placeholder="角色名称" clearable @keyup.enter="loadRoles" />
        <div class="toolbar-actions">
          <el-button @click="resetFilters">重置</el-button>
          <el-button type="primary" @click="loadRoles">查询</el-button>
          <el-button type="success" @click="openCreate">新建角色</el-button>
        </div>
      </div>
    </el-card>

    <el-card shadow="never" class="table-card">
      <el-table :data="roles" v-loading="loading">
        <el-table-column prop="sort" label="排序" width="90" />
        <el-table-column prop="code" label="编码" min-width="140" />
        <el-table-column prop="name" label="名称" min-width="150" />
        <el-table-column prop="description" label="描述" min-width="220">
          <template #default="{ row }">
            <span>{{ row.description || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="权限数" width="110">
          <template #default="{ row }">
            <strong class="numeric">{{ row.permissionCount }}</strong>
          </template>
        </el-table-column>
        <el-table-column label="绑定用户" width="110">
          <template #default="{ row }">
            <strong class="numeric">{{ row.userCount }}</strong>
          </template>
        </el-table-column>
        <el-table-column label="权限预览" min-width="280">
          <template #default="{ row }">
            <div class="permission-preview">
              <el-tag
                v-for="permissionCode in (row.permissionCodes || []).slice(0, 3)"
                :key="permissionCode"
                size="small"
                effect="plain"
              >
                {{ permissionCode }}
              </el-tag>
              <span v-if="!row.permissionCodes?.length" class="muted-text">未分配权限</span>
              <span v-else-if="row.permissionCodes.length > 3" class="muted-text">
                +{{ row.permissionCodes.length - 3 }}
              </span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <div class="row-actions">
              <el-button link type="primary" @click="openEdit(row.id)">编辑</el-button>
              <el-dropdown trigger="click" @command="(command) => handleGrantCommand(command, row)">
                <el-button link type="warning">授权</el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="resource">授权资源</el-dropdown-item>
                    <el-dropdown-item command="permission">授权权限</el-dropdown-item>
                    <el-dropdown-item command="user">授权用户</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
              <el-button link type="danger" @click="confirmDelete(row)">删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-drawer
      v-model="editorVisible"
      :title="editorMode === 'create' ? '新建角色' : '编辑角色'"
      size="520px"
      destroy-on-close
    >
      <el-form ref="editorFormRef" :model="editorForm" :rules="editorRules" label-position="top">
        <el-form-item label="角色编码" prop="code">
          <el-input v-model="editorForm.code" :disabled="editorMode === 'edit'" placeholder="例如 ADMIN_AUDITOR" />
        </el-form-item>
        <el-form-item label="角色名称" prop="name">
          <el-input v-model="editorForm.name" placeholder="用于后台展示" />
        </el-form-item>
        <el-form-item label="排序" prop="sort">
          <el-input-number v-model="editorForm.sort" :min="0" :step="10" controls-position="right" />
        </el-form-item>
        <el-form-item label="角色描述">
          <el-input
            v-model="editorForm.description"
            type="textarea"
            :rows="3"
            resize="none"
            placeholder="简要说明角色适用范围"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="drawer-footer">
          <el-button @click="editorVisible = false">取消</el-button>
          <el-button type="primary" :loading="saving" @click="submitEditor">保存</el-button>
        </div>
      </template>
    </el-drawer>

    <el-drawer
      v-model="permissionVisible"
      :title="`授权权限 · ${grantTarget?.name || ''}`"
      size="720px"
      destroy-on-close
    >
      <div v-loading="permissionLoading" class="grant-panel">
        <div class="grant-summary">
          <div>
            <h3>权限配置</h3>
            <p>权限表示 API 接口访问能力，按系统与插件分组展示。</p>
          </div>
          <el-tag type="info" effect="plain">已选 {{ permissionForm.permissionCodes.length }} 项</el-tag>
        </div>

        <section class="grant-section">
          <div class="section-title">权限分组</div>
          <el-tabs ref="permissionTabsRef" v-model="activePermissionTab" type="border-card" class="permission-tabs">
            <el-tab-pane
              v-for="tab in permissionTabs"
              :key="tab.name"
              :label="tab.label"
              :name="tab.name"
            >
              <div v-if="tab.items.length" class="checkbox-grid">
                <el-checkbox-group v-model="permissionForm.permissionCodes">
                  <label
                    v-for="permission in tab.items"
                    :key="permission.code"
                    class="checkbox-card"
                  >
                    <el-checkbox :label="permission.code">
                      <span class="checkbox-title">{{ permission.name }}</span>
                    </el-checkbox>
                    <span class="checkbox-meta">{{ permission.code }}</span>
                  </label>
                </el-checkbox-group>
              </div>
              <el-empty v-else description="暂无可授权权限" :image-size="80" />
            </el-tab-pane>
          </el-tabs>
        </section>
      </div>

      <template #footer>
        <div class="drawer-footer">
          <el-button @click="permissionVisible = false">取消</el-button>
          <el-button type="primary" :loading="permissionSaving" @click="submitPermissions">保存权限</el-button>
        </div>
      </template>
    </el-drawer>

    <el-drawer
      v-model="resourceVisible"
      :title="`授权资源 · ${grantTarget?.name || ''}`"
      size="760px"
      destroy-on-close
    >
      <div v-loading="resourceLoading" class="grant-panel">
        <div class="grant-summary">
          <div>
            <h3>资源配置</h3>
            <p>资源表示页面和按钮能力，系统资源和插件资源分开展示。</p>
          </div>
          <el-tag type="info" effect="plain">已选 {{ selectedResourceCount }} 项</el-tag>
        </div>

        <section class="grant-section">
          <div class="section-title">资源分组</div>
          <el-tabs ref="resourceTabsRef" v-model="activeResourceTab" type="border-card" class="resource-tabs">
            <el-tab-pane
              v-for="tab in resourceTabs"
              :key="tab.name"
              :label="tab.label"
              :name="tab.name"
            >
              <el-tree
                v-if="tab.resources.length"
                :ref="tab.scope === 'SYSTEM' ? systemTreeTabRef : (el) => setPluginTreeRef(tab.pluginId, el)"
                node-key="resourceKey"
                show-checkbox
                default-expand-all
                :data="tab.resources"
                :props="resourceTreeProps"
                class="resource-tree"
                @check="syncResourceSelection"
              >
                <template #default="{ data }">
                  <div class="tree-node">
                    <span>{{ data.resourceName }}</span>
                    <span class="tree-node-meta">{{ data.resourceType }}</span>
                  </div>
                </template>
              </el-tree>
              <el-empty v-else description="暂无可授权资源" :image-size="80" />
            </el-tab-pane>
          </el-tabs>
        </section>
      </div>

      <template #footer>
        <div class="drawer-footer">
          <el-button @click="resourceVisible = false">取消</el-button>
          <el-button type="primary" :loading="resourceSaving" @click="submitResources">保存资源</el-button>
        </div>
      </template>
    </el-drawer>

    <el-drawer
      v-model="usersVisible"
      :title="`授权用户 · ${grantTarget?.name || ''}`"
      size="720px"
      destroy-on-close
    >
      <div v-loading="usersLoading" class="grant-panel">
        <div class="grant-summary">
          <div>
            <h3>用户绑定</h3>
            <p>支持按账号或显示名搜索后勾选，保存时会整体替换当前角色绑定用户。</p>
          </div>
          <el-tag type="info" effect="plain">已选 {{ userSelection.length }} 人</el-tag>
        </div>

        <div class="search-grid">
          <el-input
            v-model="userFilters.username"
            placeholder="搜索账号"
            clearable
            @keyup.enter="loadRoleUserCandidates"
          />
          <el-input
            v-model="userFilters.displayName"
            placeholder="搜索显示名"
            clearable
            @keyup.enter="loadRoleUserCandidates"
          />
          <div class="toolbar-actions">
            <el-button @click="resetUserFilters">重置</el-button>
            <el-button type="primary" @click="loadRoleUserCandidates">查询</el-button>
          </div>
        </div>

        <div class="user-list">
          <label
            v-for="user in roleUserCandidates"
            :key="user.id"
            class="user-card"
          >
            <el-checkbox :model-value="userSelection.includes(user.id)" @change="(checked) => toggleUserSelection(user.id, checked)">
              <span class="user-card-title">{{ user.displayName || user.username }}</span>
            </el-checkbox>
            <span class="user-card-meta">{{ user.username }}</span>
            <span class="user-card-meta">
              {{ user.roles?.length ? user.roles.map((role) => role.name).join(' / ') : '未分配其他角色' }}
            </span>
          </label>
          <el-empty v-if="!roleUserCandidates.length" description="未找到匹配用户" :image-size="80" />
        </div>
      </div>

      <template #footer>
        <div class="drawer-footer">
          <el-button @click="usersVisible = false">取消</el-button>
          <el-button type="primary" :loading="usersSaving" @click="submitUsers">保存用户</el-button>
        </div>
      </template>
    </el-drawer>
  </section>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { fetchPermissions } from '../api/permissions'
import { fetchResourceTree } from '../api/resources'
import {
  createRole,
  deleteRole,
  fetchRoleDetail,
  fetchRolePermissions,
  fetchRoleResources,
  fetchRoles,
  fetchRoleUsers,
  updateRole,
  updateRolePermissions,
  updateRoleResources,
  updateRoleUsers
} from '../api/roles'
import { fetchUsers } from '../api/users'

const loading = ref(false)
const saving = ref(false)
const permissionLoading = ref(false)
const permissionSaving = ref(false)
const resourceLoading = ref(false)
const resourceSaving = ref(false)
const usersLoading = ref(false)
const usersSaving = ref(false)

const roles = ref([])
const permissions = ref([])
const roleUserCandidates = ref([])

const editorVisible = ref(false)
const permissionVisible = ref(false)
const resourceVisible = ref(false)
const usersVisible = ref(false)

const editorMode = ref('create')
const grantTarget = ref(null)
const editorFormRef = ref()
const permissionTabsRef = ref()
const resourceTabsRef = ref()
const systemTreeRef = ref()
const pluginTreeRefs = reactive({})

const resourceTree = reactive({
  systemResources: [],
  pluginGroups: []
})

const filters = reactive({
  code: '',
  name: ''
})

const userFilters = reactive({
  username: '',
  displayName: ''
})

const editorForm = reactive({
  id: null,
  code: '',
  name: '',
  description: '',
  sort: 0
})

const permissionForm = reactive({
  permissionCodes: []
})

const userSelection = ref([])
const selectedResourceKeys = ref([])

const editorRules = {
  code: [{ required: true, message: '请输入角色编码', trigger: 'blur' }],
  name: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
  sort: [{ required: true, message: '请输入排序', trigger: 'change' }]
}

const resourceTreeProps = {
  label: 'resourceName',
  children: 'children'
}

const systemPermissionItems = computed(() => permissions.value
  .filter((permission) => permission.source === 'SYSTEM')
  .sort((left, right) => left.code.localeCompare(right.code, 'zh-CN')))

const pluginPermissionGroups = computed(() => Object.values(
  permissions.value
    .filter((permission) => permission.source === 'PLUGIN')
    .reduce((accumulator, permission) => {
      const pluginId = permission.pluginId || 'unknown'
      if (!accumulator[pluginId]) {
        accumulator[pluginId] = {
          pluginId,
          pluginName: permission.pluginName || pluginId,
          items: []
        }
      }
      accumulator[pluginId].items.push(permission)
      return accumulator
    }, {})
).map((group) => ({
  ...group,
  items: group.items.sort((left, right) => left.code.localeCompare(right.code, 'zh-CN'))
})).sort((left, right) => left.pluginId.localeCompare(right.pluginId, 'zh-CN')))

const activePermissionTab = ref('system')
const permissionTabDragState = reactive({
  cleanup: null,
  isDragging: false
})
const permissionTabs = computed(() => [
  {
    name: 'system',
    label: '系统权限',
    items: systemPermissionItems.value
  },
  ...pluginPermissionGroups.value.map((group) => ({
    name: `plugin:${group.pluginId}`,
    label: group.pluginName,
    items: group.items
  }))
])
const selectedResourceCount = computed(() => selectedResourceKeys.value.length)
const activeResourceTab = ref('system')
const resourceTabDragState = reactive({
  cleanup: null,
  isDragging: false
})
const resourceTabs = computed(() => [
  {
    name: 'system',
    label: '系统资源',
    scope: 'SYSTEM',
    resources: resourceTree.systemResources,
    pluginId: null
  },
  ...resourceTree.pluginGroups.map((group) => ({
    name: `plugin:${group.pluginId}`,
    label: group.pluginName,
    scope: 'PLUGIN',
    resources: group.resources,
    pluginId: group.pluginId
  }))
])

onMounted(async () => {
  await Promise.all([loadRoles(), loadPermissions(), loadResourceTree()])
})

watch(resourceVisible, async (visible) => {
  if (visible) {
    await nextTick()
    initResourceTabDrag()
    return
  }
  destroyResourceTabDrag()
})

watch(permissionVisible, async (visible) => {
  if (visible) {
    await nextTick()
    initPermissionTabDrag()
    return
  }
  destroyPermissionTabDrag()
})

watch(permissionTabs, async () => {
  if (!permissionVisible.value) {
    return
  }
  await nextTick()
  initPermissionTabDrag()
})

watch(resourceTabs, async () => {
  if (!resourceVisible.value) {
    return
  }
  await nextTick()
  initResourceTabDrag()
})

onBeforeUnmount(() => {
  destroyPermissionTabDrag()
  destroyResourceTabDrag()
})

async function loadRoles() {
  loading.value = true
  try {
    roles.value = await fetchRoles(filters)
  } catch (error) {
    ElMessage.error(error.message || '加载角色列表失败')
  } finally {
    loading.value = false
  }
}

async function loadPermissions() {
  try {
    permissions.value = await fetchPermissions()
  } catch (error) {
    ElMessage.error(error.message || '加载权限列表失败')
  }
}

async function loadResourceTree() {
  try {
    const tree = await fetchResourceTree()
    resourceTree.systemResources = tree.systemResources || []
    resourceTree.pluginGroups = tree.pluginGroups || []
  } catch (error) {
    ElMessage.error(error.message || '加载资源树失败')
  }
}

async function loadRoleUserCandidates() {
  usersLoading.value = true
  try {
    roleUserCandidates.value = await fetchUsers(userFilters)
  } catch (error) {
    ElMessage.error(error.message || '加载用户列表失败')
  } finally {
    usersLoading.value = false
  }
}

function resetFilters() {
  filters.code = ''
  filters.name = ''
  loadRoles()
}

function resetUserFilters() {
  userFilters.username = ''
  userFilters.displayName = ''
  loadRoleUserCandidates()
}

function resetEditorForm() {
  editorForm.id = null
  editorForm.code = ''
  editorForm.name = ''
  editorForm.description = ''
  editorForm.sort = 0
}

async function openCreate() {
  editorMode.value = 'create'
  resetEditorForm()
  editorVisible.value = true
  await nextTick()
  editorFormRef.value?.clearValidate()
}

async function openEdit(id) {
  try {
    const detail = await fetchRoleDetail(id)
    editorMode.value = 'edit'
    editorForm.id = detail.id
    editorForm.code = detail.code
    editorForm.name = detail.name
    editorForm.description = detail.description || ''
    editorForm.sort = detail.sort ?? 0
    editorVisible.value = true
    await nextTick()
    editorFormRef.value?.clearValidate()
  } catch (error) {
    ElMessage.error(error.message || '加载角色详情失败')
  }
}

async function submitEditor() {
  try {
    await editorFormRef.value?.validate()
    saving.value = true

    if (editorMode.value === 'create') {
      await createRole({
        code: editorForm.code,
        name: editorForm.name,
        description: editorForm.description,
        sort: editorForm.sort,
        permissionCodes: []
      })
      ElMessage.success('角色已创建')
    } else {
      await updateRole(editorForm.id, {
        name: editorForm.name,
        description: editorForm.description,
        sort: editorForm.sort
      })
      ElMessage.success('角色已更新')
    }

    editorVisible.value = false
    await loadRoles()
  } catch (error) {
    if (error?.message) {
      ElMessage.error(error.message)
    }
  } finally {
    saving.value = false
  }
}

function handleGrantCommand(command, row) {
  if (command === 'permission') {
    openPermissionGrant(row)
    return
  }
  if (command === 'resource') {
    openResourceGrant(row)
    return
  }
  if (command === 'user') {
    openUserGrant(row)
  }
}

async function openPermissionGrant(row) {
  grantTarget.value = row
  permissionLoading.value = true
  permissionVisible.value = true
  try {
    permissionForm.permissionCodes = await fetchRolePermissions(row.id)
  } catch (error) {
    ElMessage.error(error.message || '加载角色权限失败')
  } finally {
    permissionLoading.value = false
  }
}

async function submitPermissions() {
  if (!grantTarget.value) {
    return
  }
  try {
    permissionSaving.value = true
    await updateRolePermissions(grantTarget.value.id, permissionForm.permissionCodes)
    ElMessage.success('角色权限已更新')
    permissionVisible.value = false
    await loadRoles()
  } catch (error) {
    if (error?.message) {
      ElMessage.error(error.message)
    }
  } finally {
    permissionSaving.value = false
  }
}

async function openResourceGrant(row) {
  grantTarget.value = row
  resourceLoading.value = true
  resourceVisible.value = true
  try {
    if (!resourceTree.systemResources.length && !resourceTree.pluginGroups.length) {
      await loadResourceTree()
    }
    selectedResourceKeys.value = await fetchRoleResources(row.id)
    await nextTick()
    applyCheckedResourceKeys()
  } catch (error) {
    ElMessage.error(error.message || '加载角色资源失败')
  } finally {
    resourceLoading.value = false
  }
}

async function submitResources() {
  if (!grantTarget.value) {
    return
  }
  try {
    resourceSaving.value = true
    syncResourceSelection()
    await updateRoleResources(grantTarget.value.id, selectedResourceKeys.value)
    ElMessage.success('角色资源已更新')
    resourceVisible.value = false
  } catch (error) {
    if (error?.message) {
      ElMessage.error(error.message)
    }
  } finally {
    resourceSaving.value = false
  }
}

async function openUserGrant(row) {
  grantTarget.value = row
  usersVisible.value = true
  usersLoading.value = true
  try {
    userSelection.value = (await fetchRoleUsers(row.id)).map((user) => user.id)
    await loadRoleUserCandidates()
  } catch (error) {
    ElMessage.error(error.message || '加载角色用户失败')
  } finally {
    usersLoading.value = false
  }
}

async function submitUsers() {
  if (!grantTarget.value) {
    return
  }
  try {
    usersSaving.value = true
    await updateRoleUsers(grantTarget.value.id, userSelection.value)
    ElMessage.success('角色用户已更新')
    usersVisible.value = false
    await loadRoles()
  } catch (error) {
    if (error?.message) {
      ElMessage.error(error.message)
    }
  } finally {
    usersSaving.value = false
  }
}

function toggleUserSelection(userId, checked) {
  const next = new Set(userSelection.value)
  if (checked) {
    next.add(userId)
  } else {
    next.delete(userId)
  }
  userSelection.value = Array.from(next)
}

function setPluginTreeRef(pluginId, el) {
  if (el) {
    pluginTreeRefs[pluginId] = el
  } else {
    delete pluginTreeRefs[pluginId]
  }
}

function systemTreeTabRef(el) {
  systemTreeRef.value = el || null
}

function initPermissionTabDrag() {
  initTabsDrag(permissionTabsRef, permissionTabDragState, 'dragging-permission-tabs')
}

function destroyPermissionTabDrag() {
  permissionTabDragState.cleanup?.()
  permissionTabDragState.cleanup = null
}

function initResourceTabDrag() {
  initTabsDrag(resourceTabsRef, resourceTabDragState, 'dragging-resource-tabs')
}

function initTabsDrag(tabsRef, dragState, bodyClassName) {
  dragState.cleanup?.()
  dragState.cleanup = null
  const tabsRoot = tabsRef.value?.$el
  const navWrap = tabsRoot?.querySelector('.el-tabs__nav-wrap')
  const navScroll = tabsRoot?.querySelector('.el-tabs__nav-scroll')
  if (!navWrap || !navScroll) {
    return
  }

  let startX = 0
  let startScrollLeft = 0
  let moved = false

  const resetDragging = () => {
    dragState.isDragging = false
    navWrap.classList.remove('is-dragging')
    document.body.classList.remove(bodyClassName)
  }

  const handleMouseDown = (event) => {
    if (event.button !== 0) {
      return
    }
    dragState.isDragging = true
    moved = false
    startX = event.clientX
    startScrollLeft = navWrap.scrollLeft
    navWrap.classList.add('is-dragging')
    document.body.classList.add(bodyClassName)
  }

  const handleMouseMove = (event) => {
    if (!dragState.isDragging) {
      return
    }
    const deltaX = event.clientX - startX
    if (Math.abs(deltaX) > 4) {
      moved = true
    }
    navWrap.scrollLeft = startScrollLeft - deltaX
  }

  const handleMouseUp = () => {
    resetDragging()
  }

  const handleMouseLeaveWindow = () => {
    resetDragging()
  }

  const handleClickCapture = (event) => {
    if (!moved) {
      return
    }
    event.preventDefault()
    event.stopPropagation()
    moved = false
  }

  navScroll.addEventListener('mousedown', handleMouseDown)
  navScroll.addEventListener('click', handleClickCapture, true)
  window.addEventListener('mousemove', handleMouseMove)
  window.addEventListener('mouseup', handleMouseUp)
  window.addEventListener('mouseleave', handleMouseLeaveWindow)

  dragState.cleanup = () => {
    navScroll.removeEventListener('mousedown', handleMouseDown)
    navScroll.removeEventListener('click', handleClickCapture, true)
    window.removeEventListener('mousemove', handleMouseMove)
    window.removeEventListener('mouseup', handleMouseUp)
    window.removeEventListener('mouseleave', handleMouseLeaveWindow)
    resetDragging()
  }
}

function destroyResourceTabDrag() {
  resourceTabDragState.cleanup?.()
  resourceTabDragState.cleanup = null
}

function applyCheckedResourceKeys() {
  systemTreeRef.value?.setCheckedKeys(selectedResourceKeys.value)
  for (const group of resourceTree.pluginGroups) {
    pluginTreeRefs[group.pluginId]?.setCheckedKeys(selectedResourceKeys.value)
  }
}

function syncResourceSelection() {
  const keys = new Set(systemTreeRef.value?.getCheckedKeys(false) || [])
  for (const group of resourceTree.pluginGroups) {
    const treeRef = pluginTreeRefs[group.pluginId]
    for (const key of treeRef?.getCheckedKeys(false) || []) {
      keys.add(key)
    }
  }
  selectedResourceKeys.value = Array.from(keys).sort()
}

function countResourceNodes(nodes) {
  return nodes.reduce((count, node) => count + 1 + countResourceNodes(node.children || []), 0)
}

async function confirmDelete(row) {
  try {
    await ElMessageBox.confirm(`确定删除角色“${row.name}”吗？`, '删除角色', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
    await deleteRole(row.id)
    ElMessage.success('角色已删除')
    await loadRoles()
  } catch (error) {
    if (error === 'cancel' || error === 'close') {
      return
    }
    if (error?.message) {
      ElMessage.error(error.message)
    }
  }
}
</script>

<style scoped>
.console-page {
  display: grid;
  gap: 12px;
}

.page-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
}

.page-header h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: var(--shell-tool-header-text);
}

.page-header p {
  margin: 6px 0 0;
  font-size: 13px;
  color: var(--shell-text-secondary);
}

.page-header-meta {
  display: inline-flex;
  align-items: center;
  min-height: 32px;
  padding: 0 12px;
  border: 1px solid var(--shell-tool-border-strong);
  border-radius: 6px;
  background: var(--shell-tool-surface-muted);
  color: var(--shell-tool-subtle-text);
  font-size: 12px;
}

.toolbar-card,
.table-card {
  border-radius: 8px;
  border: 1px solid var(--shell-tool-border-strong);
  background: var(--shell-tool-surface);
  box-shadow: none;
}

.toolbar-grid {
  display: grid;
  gap: 8px;
  align-items: center;
}

.roles-toolbar {
  grid-template-columns: minmax(180px, 1fr) 180px 180px auto;
}

.toolbar-title span {
  display: inline-flex;
  align-items: center;
  min-height: 32px;
  color: var(--shell-tool-subtle-text);
  font-size: 12px;
  font-weight: 600;
}

.toolbar-actions {
  display: inline-flex;
  gap: 6px;
  justify-content: flex-end;
}

.row-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.numeric {
  font-variant-numeric: tabular-nums;
}

.permission-preview {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.muted-text {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.drawer-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.grant-panel {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.grant-summary {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 16px;
  border: 1px solid var(--shell-tool-border-strong);
  border-radius: 8px;
  background: var(--shell-tool-surface-muted);
}

.grant-summary h3 {
  margin: 0 0 6px;
  font-size: 14px;
  color: var(--shell-tool-header-text);
}

.grant-summary p {
  margin: 0;
  color: var(--shell-text-secondary);
  line-height: 1.5;
  font-size: 12px;
}

.grant-section {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.section-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--shell-tool-header-text);
}

.checkbox-grid {
  display: grid;
  gap: 8px;
}

.checkbox-card {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 10px 12px;
  border: 1px solid var(--shell-tool-divider);
  border-radius: 6px;
  background: var(--shell-tool-surface);
}

.checkbox-card:hover {
  border-color: var(--shell-tool-border-strong);
  background: var(--shell-tool-hover);
}

.checkbox-title {
  font-weight: 600;
  font-size: 13px;
}

.checkbox-meta {
  color: var(--shell-tool-subtle-text);
  font-size: 11px;
}

.resource-tree {
  padding: 10px 12px;
  border: 1px solid var(--shell-tool-divider);
  border-radius: 6px;
  background: var(--shell-tool-surface);
}

.permission-tabs :deep(.el-tabs__nav-wrap),
.resource-tabs :deep(.el-tabs__nav-wrap) {
  cursor: grab;
  scroll-behavior: smooth;
}

.permission-tabs :deep(.el-tabs__nav-wrap.is-dragging),
.resource-tabs :deep(.el-tabs__nav-wrap.is-dragging) {
  cursor: grabbing;
}

.permission-tabs :deep(.el-tabs__nav-scroll),
.resource-tabs :deep(.el-tabs__nav-scroll) {
  user-select: none;
}

.tree-node {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.tree-node-meta {
  color: var(--shell-tool-subtle-text);
  font-size: 11px;
}

.search-grid {
  display: grid;
  grid-template-columns: 1fr 1fr auto;
  gap: 8px;
  align-items: center;
}

.user-list {
  display: grid;
  gap: 8px;
}

.user-card {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 10px 12px;
  border: 1px solid var(--shell-tool-divider);
  border-radius: 6px;
  background: var(--shell-tool-surface);
}

.user-card-title {
  font-weight: 600;
  font-size: 13px;
}

.user-card-meta {
  color: var(--shell-tool-subtle-text);
  font-size: 11px;
}

.permission-preview :deep(.el-tag),
.grant-summary :deep(.el-tag) {
  border-radius: 4px;
}

.resource-tree :deep(.el-tree-node__content) {
  min-height: 34px;
}

.toolbar-card :deep(.el-card__body),
.table-card :deep(.el-card__body) {
  padding: 10px 12px;
}

.toolbar-card :deep(.el-input__wrapper),
.toolbar-card :deep(.el-select__wrapper) {
  background: var(--shell-tool-toolbar-bg);
  box-shadow: 0 0 0 1px var(--shell-tool-border-strong) inset;
  border-radius: 6px;
}

.toolbar-card :deep(.el-button) {
  min-height: 30px;
  border-radius: 6px;
  font-size: 12px;
}

.table-card :deep(.el-table) {
  --el-table-border-color: var(--shell-tool-divider);
  --el-table-header-bg-color: var(--shell-tool-toolbar-bg);
  --el-table-row-hover-bg-color: var(--shell-tool-hover);
  font-size: 12px;
}

.table-card :deep(.el-table th.el-table__cell) {
  padding: 10px 0;
  font-size: 12px;
  font-weight: 600;
  color: var(--shell-tool-subtle-text);
}

.table-card :deep(.el-table .el-table__cell) {
  padding: 10px 0;
}

.table-card :deep(.el-table__inner-wrapper::before) {
  background-color: var(--shell-tool-divider);
}

.table-card :deep(.el-button.is-link) {
  font-size: 12px;
  font-weight: 500;
}

.permission-preview :deep(.el-tag),
.table-card :deep(.el-tag) {
  --el-tag-bg-color: var(--shell-tool-tag-bg);
  --el-tag-border-color: var(--shell-tool-tag-border);
  --el-tag-text-color: var(--shell-tool-tag-text);
  font-size: 11px;
}

@media (max-width: 960px) {
  .page-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .roles-toolbar,
  .search-grid {
    grid-template-columns: 1fr;
  }
}
</style>
