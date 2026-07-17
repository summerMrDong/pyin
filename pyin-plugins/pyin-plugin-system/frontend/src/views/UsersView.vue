<template>
  <section class="console-page">
    <div class="page-header">
      <div>
        <h2>用户管理</h2>
        <p>维护后台账号、启停状态与角色分配。</p>
      </div>
      <div class="page-header-meta">
        <span>当前 {{ users.length }} 个用户</span>
      </div>
    </div>

    <el-card shadow="never" class="toolbar-card">
      <div class="toolbar-grid">
        <div class="toolbar-title">
          <span>筛选条件</span>
        </div>
        <el-input v-model="filters.username" placeholder="账号" clearable @keyup.enter="loadUsers" />
        <el-input v-model="filters.displayName" placeholder="显示名" clearable @keyup.enter="loadUsers" />
        <el-select v-model="filters.status" placeholder="状态" clearable>
          <el-option label="启用" value="ENABLED" />
          <el-option label="停用" value="DISABLED" />
        </el-select>
        <div class="toolbar-actions">
          <el-button @click="resetFilters">重置</el-button>
          <el-button type="primary" @click="loadUsers">查询</el-button>
          <el-button type="success" @click="openCreate">新建用户</el-button>
        </div>
      </div>
    </el-card>

    <el-card shadow="never" class="table-card">
      <el-table :data="users" v-loading="loading" class="users-table">
        <el-table-column prop="username" label="账号" min-width="150" />
        <el-table-column prop="displayName" label="显示名" min-width="160">
          <template #default="{ row }">
            <span>{{ row.displayName || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ENABLED' ? 'success' : 'info'" effect="plain">
              {{ row.status === 'ENABLED' ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="角色" min-width="240">
          <template #default="{ row }">
            <div class="tag-list">
              <el-tag
                v-for="role in row.roles"
                :key="role.id"
                size="small"
                effect="plain"
                class="role-tag"
              >
                {{ role.name }}
              </el-tag>
              <span v-if="!row.roles?.length" class="muted-text">未分配角色</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" min-width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="300" fixed="right">
          <template #default="{ row }">
            <div class="row-actions">
              <el-button link type="primary" @click="openEdit(row.id)">编辑</el-button>
              <el-button link @click="toggleStatus(row)">
                {{ row.status === 'ENABLED' ? '停用' : '启用' }}
              </el-button>
              <el-button link type="warning" @click="openResetPassword(row)">重置密码</el-button>
              <el-button link type="danger" @click="confirmDelete(row)">删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-drawer
      v-model="editorVisible"
      :title="editorMode === 'create' ? '新建用户' : '编辑用户'"
      size="520px"
      destroy-on-close
    >
      <el-form ref="editorFormRef" :model="editorForm" :rules="editorRules" label-position="top">
        <el-form-item label="账号" prop="username">
          <el-input v-model="editorForm.username" :disabled="editorMode === 'edit'" placeholder="请输入唯一账号" />
        </el-form-item>
        <el-form-item label="显示名" prop="displayName">
          <el-input v-model="editorForm.displayName" placeholder="用于页面展示，可为空" />
        </el-form-item>
        <el-form-item v-if="editorMode === 'create'" label="初始密码" prop="password">
          <el-input v-model="editorForm.password" type="password" show-password placeholder="至少输入一个可登录密码" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="editorForm.status" class="status-grid">
            <el-radio-button label="ENABLED">启用</el-radio-button>
            <el-radio-button label="DISABLED">停用</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="角色分配">
          <el-select
            v-model="editorForm.roleIds"
            multiple
            clearable
            collapse-tags
            collapse-tags-tooltip
            placeholder="选择一个或多个角色"
          >
            <el-option
              v-for="role in roleOptions"
              :key="role.id"
              :label="`${role.name} (${role.code})`"
              :value="role.id"
            />
          </el-select>
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="drawer-footer">
          <el-button @click="editorVisible = false">取消</el-button>
          <el-button type="primary" :loading="saving" @click="submitEditor">保存</el-button>
        </div>
      </template>
    </el-drawer>

    <el-dialog v-model="passwordVisible" title="重置密码" width="420px" destroy-on-close>
      <el-form ref="passwordFormRef" :model="passwordForm" :rules="passwordRules" label-position="top">
        <el-form-item label="账号">
          <el-input :model-value="passwordTarget?.username || ''" disabled />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="passwordForm.newPassword" type="password" show-password placeholder="请输入新的登录密码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="passwordVisible = false">取消</el-button>
          <el-button type="primary" :loading="savingPassword" @click="submitPasswordReset">确认重置</el-button>
        </div>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createUser,
  deleteUser,
  fetchUserDetail,
  fetchUsers,
  resetUserPassword,
  updateUser
} from '../api/users'
import { fetchRoleOptions } from '../api/roles'

const loading = ref(false)
const saving = ref(false)
const savingPassword = ref(false)
const users = ref([])
const roleOptions = ref([])
const editorVisible = ref(false)
const editorMode = ref('create')
const passwordVisible = ref(false)
const passwordTarget = ref(null)
const editorFormRef = ref()
const passwordFormRef = ref()

const filters = reactive({
  username: '',
  displayName: '',
  status: ''
})

const editorForm = reactive({
  id: null,
  username: '',
  displayName: '',
  password: '',
  status: 'ENABLED',
  roleIds: []
})

const passwordForm = reactive({
  newPassword: ''
})

const editorRules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入初始密码', trigger: 'blur' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
}

const passwordRules = {
  newPassword: [{ required: true, message: '请输入新密码', trigger: 'blur' }]
}

onMounted(async () => {
  await Promise.all([loadUsers(), loadRoleOptions()])
})

async function loadUsers() {
  loading.value = true
  try {
    users.value = await fetchUsers(filters)
  } catch (error) {
    ElMessage.error(error.message || '加载用户列表失败')
  } finally {
    loading.value = false
  }
}

async function loadRoleOptions() {
  try {
    roleOptions.value = await fetchRoleOptions()
  } catch (error) {
    ElMessage.error(error.message || '加载角色选项失败')
  }
}

function resetFilters() {
  filters.username = ''
  filters.displayName = ''
  filters.status = ''
  loadUsers()
}

function resetEditorForm() {
  editorForm.id = null
  editorForm.username = ''
  editorForm.displayName = ''
  editorForm.password = ''
  editorForm.status = 'ENABLED'
  editorForm.roleIds = []
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
    const detail = await fetchUserDetail(id)
    editorMode.value = 'edit'
    editorForm.id = detail.id
    editorForm.username = detail.username
    editorForm.displayName = detail.displayName || ''
    editorForm.password = ''
    editorForm.status = detail.status || 'ENABLED'
    editorForm.roleIds = detail.roleIds || []
    editorVisible.value = true
    await nextTick()
    editorFormRef.value?.clearValidate()
  } catch (error) {
    ElMessage.error(error.message || '加载用户详情失败')
  }
}

async function submitEditor() {
  try {
    await editorFormRef.value?.validate()
    saving.value = true

    if (editorMode.value === 'create') {
      await createUser({
        username: editorForm.username,
        displayName: editorForm.displayName,
        password: editorForm.password,
        status: editorForm.status,
        roleIds: editorForm.roleIds
      })
      ElMessage.success('用户已创建')
    } else {
      await updateUser(editorForm.id, {
        displayName: editorForm.displayName,
        status: editorForm.status,
        roleIds: editorForm.roleIds
      })
      ElMessage.success('用户已更新')
    }

    editorVisible.value = false
    await loadUsers()
  } catch (error) {
    if (error?.message) {
      ElMessage.error(error.message)
    }
  } finally {
    saving.value = false
  }
}

async function openResetPassword(row) {
  passwordTarget.value = row
  passwordForm.newPassword = ''
  passwordVisible.value = true
  await nextTick()
  passwordFormRef.value?.clearValidate()
}

async function submitPasswordReset() {
  try {
    await passwordFormRef.value?.validate()
    savingPassword.value = true
    await resetUserPassword(passwordTarget.value.id, { newPassword: passwordForm.newPassword })
    ElMessage.success('密码已重置')
    passwordVisible.value = false
  } catch (error) {
    if (error?.message) {
      ElMessage.error(error.message)
    }
  } finally {
    savingPassword.value = false
  }
}

async function toggleStatus(row) {
  try {
    await updateUser(row.id, {
      displayName: row.displayName,
      status: row.status === 'ENABLED' ? 'DISABLED' : 'ENABLED',
      roleIds: (row.roles || []).map((role) => role.id)
    })
    ElMessage.success(row.status === 'ENABLED' ? '用户已停用' : '用户已启用')
    await loadUsers()
  } catch (error) {
    ElMessage.error(error.message || '更新用户状态失败')
  }
}

async function confirmDelete(row) {
  try {
    await ElMessageBox.confirm(`确定删除账号 ${row.username} 吗？`, '删除用户', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
    await deleteUser(row.id)
    ElMessage.success('用户已删除')
    await loadUsers()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error.message || '删除用户失败')
    }
  }
}

function formatDateTime(value) {
  if (!value) {
    return '-'
  }

  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }

  const year = date.getFullYear()
  const month = `${date.getMonth() + 1}`.padStart(2, '0')
  const day = `${date.getDate()}`.padStart(2, '0')
  const hour = `${date.getHours()}`.padStart(2, '0')
  const minute = `${date.getMinutes()}`.padStart(2, '0')
  return `${year}-${month}-${day} ${hour}:${minute}`
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
  grid-template-columns: auto repeat(3, minmax(0, 1fr)) auto;
  gap: 8px;
  align-items: center;
}

.toolbar-title h2 {
  margin: 0;
  font-size: 13px;
  font-weight: 600;
  color: var(--shell-tool-header-text);
  letter-spacing: 0.01em;
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

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.muted-text {
  color: var(--shell-text-muted);
  font-size: 12px;
}

.row-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 2px 6px;
}

.drawer-footer,
.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.status-grid {
  display: flex;
  width: 100%;
}

.table-card :deep(.el-card__body),
.toolbar-card :deep(.el-card__body) {
  padding: 10px 12px;
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

.toolbar-card :deep(.el-button--success) {
  --el-button-bg-color: var(--el-color-primary);
  --el-button-border-color: var(--el-color-primary);
  --el-button-hover-bg-color: var(--el-color-primary-light-3);
  --el-button-hover-border-color: var(--el-color-primary-light-3);
}

.table-card :deep(.el-tag),
.role-tag {
  --el-tag-bg-color: var(--shell-tool-tag-bg);
  --el-tag-border-color: var(--shell-tool-tag-border);
  --el-tag-text-color: var(--shell-tool-tag-text);
  border-radius: 4px;
  font-size: 11px;
  padding: 0 6px;
}

.table-card :deep(.el-button.is-link) {
  font-size: 12px;
  font-weight: 500;
}

.table-card :deep(.el-table__inner-wrapper::before) {
  background-color: var(--shell-tool-divider);
}

.table-card :deep(.el-empty__description),
.table-card :deep(.el-pagination) {
  color: var(--shell-text-secondary);
}

.drawer-footer :deep(.el-button),
.dialog-footer :deep(.el-button) {
  min-width: 72px;
}

.toolbar-card :deep(.el-card__body) {
  background: var(--shell-tool-surface);
}

.table-card :deep(.el-card__body) {
  background: var(--shell-tool-surface);
}

.table-card :deep(.el-tag--success),
.table-card :deep(.el-tag--info) {
  --el-tag-bg-color: var(--shell-tool-tag-bg);
  --el-tag-border-color: var(--shell-tool-tag-border);
}

.table-card :deep(.el-table td:nth-child(1) .cell),
.table-card :deep(.el-table td:nth-child(5) .cell) {
  font-family: Consolas, "JetBrains Mono", "SFMono-Regular", monospace;
  font-size: 11.5px;
}

.toolbar-card {
  overflow: hidden;
}

.toolbar-card :deep(.el-input__inner),
.toolbar-card :deep(.el-select__placeholder),
.toolbar-card :deep(.el-select__selected-item) {
  font-size: 12px;
}

.table-card :deep(.el-table tr) {
  transition: background-color 0.15s ease;
}

.table-card :deep(.el-table .el-table__row .el-button.is-link + .el-button.is-link) {
  margin-left: 0;
}

@media (max-width: 1280px) {
  .page-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .toolbar-grid {
    grid-template-columns: 1fr 1fr 1fr;
  }

  .toolbar-title,
  .toolbar-actions {
    grid-column: 1 / -1;
  }

  .toolbar-actions {
    justify-content: flex-start;
  }
}

@media (max-width: 1100px) {
  .toolbar-grid {
    grid-template-columns: 1fr 1fr;
  }

  .toolbar-title,
  .toolbar-actions {
    grid-column: 1 / -1;
    justify-content: flex-start;
  }
}

@media (max-width: 720px) {
  .toolbar-grid {
    grid-template-columns: 1fr;
  }
}
</style>
