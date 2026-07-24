<template>
  <section class="console-page">
    <div class="page-header">
      <div>
        <h2>接入凭证管理</h2>
        <p>管理 C 端系统接入凭证、密钥轮换状态与请求审计日志。</p>
      </div>
      <div class="page-header-meta">
        <span>当前 {{ credentials.length }} 个凭证</span>
      </div>
    </div>

    <el-card shadow="never" class="toolbar-card">
      <div class="toolbar-grid">
        <div class="toolbar-title">
          <span>筛选条件</span>
        </div>
        <el-input v-model="filters.credentialName" placeholder="凭证名称" clearable @keyup.enter="loadCredentials" />
        <el-input v-model="filters.accessKey" placeholder="Access Key" clearable @keyup.enter="loadCredentials" />
        <el-select v-model="filters.status" placeholder="状态" clearable>
          <el-option label="启用" value="ENABLED" />
          <el-option label="停用" value="DISABLED" />
        </el-select>
        <div class="toolbar-actions">
          <el-button @click="resetFilters">重置</el-button>
          <el-button type="primary" @click="loadCredentials">查询</el-button>
          <el-button type="success" @click="openCreate">新建凭证</el-button>
        </div>
      </div>
    </el-card>

    <el-card shadow="never" class="table-card">
      <el-table :data="credentials" v-loading="loading">
        <el-table-column prop="credentialName" label="凭证名称" min-width="180" />
        <el-table-column prop="accessKey" label="Access Key" min-width="260" />
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ENABLED' ? 'success' : 'info'" effect="plain">
              {{ row.status === 'ENABLED' ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" min-width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="更新时间" min-width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.updatedAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <div class="row-actions">
              <el-button link type="primary" @click="openLogs(row)">查看日志</el-button>
              <el-button link type="warning" @click="confirmRotate(row)">轮换密钥</el-button>
              <el-button
                link
                :type="row.status === 'ENABLED' ? 'danger' : 'success'"
                @click="confirmToggleStatus(row)"
              >
                {{ row.status === 'ENABLED' ? '停用' : '启用' }}
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-drawer v-model="editorVisible" title="新建接入凭证" size="480px" destroy-on-close>
      <el-form ref="editorFormRef" :model="editorForm" :rules="editorRules" label-position="top">
        <el-form-item label="凭证名称" prop="credentialName">
          <el-input v-model="editorForm.credentialName" placeholder="例如：订单系统生产环境" />
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="drawer-footer">
          <el-button @click="editorVisible = false">取消</el-button>
          <el-button type="primary" :loading="saving" @click="submitCreate">创建并生成密钥</el-button>
        </div>
      </template>
    </el-drawer>

    <el-dialog v-model="secretVisible" title="密钥信息" width="560px" destroy-on-close>
      <div class="secret-panel">
        <el-alert type="warning" :closable="false" show-icon title="Access Secret 仅显示一次，请立即妥善保存。" />
        <div class="secret-field">
          <label>凭证名称</label>
          <code>{{ secretResult?.credentialName || '-' }}</code>
        </div>
        <div class="secret-field">
          <label>Access Key</label>
          <code>{{ secretResult?.accessKey || '-' }}</code>
        </div>
        <div class="secret-field">
          <label>Access Secret</label>
          <code class="secret-value">{{ secretResult?.accessSecret || '-' }}</code>
        </div>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="secretVisible = false">我已保存</el-button>
        </div>
      </template>
    </el-dialog>

    <el-drawer v-model="logsVisible" :title="`请求日志 · ${logTarget?.credentialName || ''}`" size="760px" destroy-on-close>
      <div class="log-panel">
        <div class="search-grid">
          <el-select v-model="logFilters.requestStatus" placeholder="结果状态" clearable>
            <el-option label="成功" value="SUCCESS" />
            <el-option label="失败" value="FAILED" />
          </el-select>
          <el-select v-model="logFilters.requestType" placeholder="请求类型" clearable>
            <el-option label="认证申请" value="AUTH_TOKEN" />
            <el-option label="插件调用" value="PLUGIN_CLIENT_API" />
          </el-select>
          <el-input v-model="logFilters.keyword" placeholder="按 URI 搜索" clearable @keyup.enter="loadLogs" />
          <div class="toolbar-actions">
            <el-button @click="resetLogFilters">重置</el-button>
            <el-button type="primary" @click="loadLogs">查询</el-button>
          </div>
        </div>

        <el-table :data="requestLogs" v-loading="logsLoading" class="logs-table">
          <el-table-column label="时间" min-width="170">
            <template #default="{ row }">
              {{ formatDateTime(row.createdAt) }}
            </template>
          </el-table-column>
          <el-table-column prop="requestType" label="类型" min-width="130" />
          <el-table-column prop="httpMethod" label="方法" width="90" />
          <el-table-column prop="requestUri" label="URI" min-width="220" />
          <el-table-column label="结果" width="100">
            <template #default="{ row }">
              <el-tag :type="row.requestStatus === 'SUCCESS' ? 'success' : 'danger'" effect="plain">
                {{ row.requestStatus === 'SUCCESS' ? '成功' : '失败' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="clientIp" label="来源 IP" min-width="130" />
          <el-table-column label="失败信息" min-width="220">
            <template #default="{ row }">
              <span v-if="row.failureCode || row.failureMessage">
                {{ [row.failureCode, row.failureMessage].filter(Boolean).join(' / ') }}
              </span>
              <span v-else class="muted-text">-</span>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-drawer>
  </section>
</template>

<script setup>
import { nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createClientCredential,
  disableClientCredential,
  enableClientCredential,
  fetchClientCredentialRequestLogs,
  fetchClientCredentials,
  rotateClientCredentialSecret
} from '../api/credentials'

const loading = ref(false)
const saving = ref(false)
const logsLoading = ref(false)
const credentials = ref([])
const requestLogs = ref([])
const editorVisible = ref(false)
const secretVisible = ref(false)
const logsVisible = ref(false)
const logTarget = ref(null)
const secretResult = ref(null)
const editorFormRef = ref()

const filters = reactive({
  credentialName: '',
  accessKey: '',
  status: ''
})

const logFilters = reactive({
  requestStatus: '',
  requestType: '',
  keyword: ''
})

const editorForm = reactive({
  credentialName: ''
})

const editorRules = {
  credentialName: [{ required: true, message: '请输入凭证名称', trigger: 'blur' }]
}

onMounted(() => {
  loadCredentials()
})

async function loadCredentials() {
  loading.value = true
  try {
    credentials.value = await fetchClientCredentials(filters)
  } catch (error) {
    ElMessage.error(error.message || '加载接入凭证失败')
  } finally {
    loading.value = false
  }
}

function resetFilters() {
  filters.credentialName = ''
  filters.accessKey = ''
  filters.status = ''
  loadCredentials()
}

async function openCreate() {
  editorForm.credentialName = ''
  editorVisible.value = true
  await nextTick()
  editorFormRef.value?.clearValidate()
}

async function submitCreate() {
  if (!editorFormRef.value) {
    return
  }
  try {
    await editorFormRef.value.validate()
  } catch {
    return
  }
  saving.value = true
  try {
    const result = await createClientCredential({ credentialName: editorForm.credentialName })
    editorVisible.value = false
    secretResult.value = result
    secretVisible.value = true
    ElMessage.success('接入凭证已创建')
    await loadCredentials()
  } catch (error) {
    ElMessage.error(error.message || '创建接入凭证失败')
  } finally {
    saving.value = false
  }
}

async function confirmToggleStatus(row) {
  const action = row.status === 'ENABLED' ? '停用' : '启用'
  try {
    await ElMessageBox.confirm(`确定${action}凭证“${row.credentialName}”吗？`, `${action}接入凭证`, {
      type: 'warning',
      confirmButtonText: action,
      cancelButtonText: '取消'
    })
    if (row.status === 'ENABLED') {
      await disableClientCredential(row.id)
    } else {
      await enableClientCredential(row.id)
    }
    ElMessage.success(`凭证已${action}`)
    await loadCredentials()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error.message || `${action}接入凭证失败`)
    }
  }
}

async function confirmRotate(row) {
  try {
    await ElMessageBox.confirm(`确定轮换凭证“${row.credentialName}”的密钥吗？旧密钥会立即失效。`, '轮换密钥', {
      type: 'warning',
      confirmButtonText: '轮换',
      cancelButtonText: '取消'
    })
    secretResult.value = await rotateClientCredentialSecret(row.id)
    secretVisible.value = true
    ElMessage.success('密钥已轮换')
    await loadCredentials()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error.message || '轮换密钥失败')
    }
  }
}

async function openLogs(row) {
  logTarget.value = row
  logsVisible.value = true
  await loadLogs()
}

async function loadLogs() {
  if (!logTarget.value) {
    return
  }
  logsLoading.value = true
  try {
    requestLogs.value = await fetchClientCredentialRequestLogs(logTarget.value.id, logFilters)
  } catch (error) {
    ElMessage.error(error.message || '加载请求日志失败')
  } finally {
    logsLoading.value = false
  }
}

function resetLogFilters() {
  logFilters.requestStatus = ''
  logFilters.requestType = ''
  logFilters.keyword = ''
  loadLogs()
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

.row-actions,
.drawer-footer,
.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.log-panel,
.secret-panel {
  display: grid;
  gap: 12px;
}

.secret-field {
  display: grid;
  gap: 6px;
}

.secret-field label {
  font-size: 12px;
  color: var(--shell-tool-subtle-text);
}

.secret-field code {
  display: block;
  padding: 10px 12px;
  border-radius: 6px;
  border: 1px solid var(--shell-tool-divider);
  background: var(--shell-tool-code-bg);
  color: var(--shell-tool-code-text);
  word-break: break-all;
}

.secret-value {
  font-weight: 600;
}

.search-grid {
  display: grid;
  grid-template-columns: 160px 180px 1fr auto;
  gap: 8px;
  align-items: center;
}

.muted-text {
  color: var(--shell-text-muted);
  font-size: 12px;
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

.table-card :deep(.el-table),
.logs-table {
  --el-table-border-color: var(--shell-tool-divider);
  --el-table-header-bg-color: var(--shell-tool-toolbar-bg);
  --el-table-row-hover-bg-color: var(--shell-tool-hover);
  font-size: 12px;
}

.table-card :deep(.el-table th.el-table__cell),
.logs-table :deep(th.el-table__cell) {
  padding: 10px 0;
  font-size: 12px;
  font-weight: 600;
  color: var(--shell-tool-subtle-text);
}

.table-card :deep(.el-table .el-table__cell),
.logs-table :deep(.el-table__cell) {
  padding: 10px 0;
}

.table-card :deep(.el-tag),
.logs-table :deep(.el-tag) {
  --el-tag-bg-color: var(--shell-tool-tag-bg);
  --el-tag-border-color: var(--shell-tool-tag-border);
  --el-tag-text-color: var(--shell-tool-tag-text);
  border-radius: 4px;
  font-size: 11px;
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
  .toolbar-actions,
  .search-grid {
    grid-column: 1 / -1;
  }
}

@media (max-width: 960px) {
  .toolbar-grid,
  .search-grid {
    grid-template-columns: 1fr;
  }
}
</style>
