<template>
  <section class="config-manager" tabindex="-1">
    <aside class="resource-explorer">
      <header class="explorer-header">
        <div>
          <p class="eyebrow">CONFIGURATION</p>
          <h2>配置资源管理器</h2>
        </div>
        <span class="item-total">{{ items.length }}</span>
      </header>

      <el-input v-model.trim="treeKeyword" class="tree-search" placeholder="搜索配置目录或 Key" clearable>
        <template #suffix><span class="search-glyph">⌕</span></template>
      </el-input>

      <div class="tree-caption"><span>配置目录</span><span>Key 投影</span></div>
      <el-tree
        ref="treeRef"
        :key="treeVersion"
        class="key-tree"
        :data="filteredTreeData"
        node-key="nodeKey"
        :props="treeProps"
        :default-expanded-keys="expandedKeys"
        :expand-on-click-node="false"
        highlight-current
        @node-click="handleTreeNodeClick"
      >
        <template #default="{ data }">
          <span class="tree-node" :class="{ 'is-key': data.kind === 'item' }">
            <span class="tree-node-main"><span class="tree-icon">{{ data.kind === 'item' ? '⌘' : '▾' }}</span><span>{{ data.label }}</span></span>
            <small>{{ data.itemCount }}</small>
          </span>
        </template>
      </el-tree>

      <footer class="explorer-footer">
        <el-button text @click="openItemCreate"><span class="button-glyph">＋</span> 新建配置</el-button>
        <el-button text @click="reloadWorkspace"><span class="button-glyph">↻</span> 刷新目录</el-button>
      </footer>
    </aside>

    <main class="config-workspace">
      <header class="workspace-toolbar">
        <div class="toolbar-actions">
          <el-button type="primary" @click="openItemCreate"><span class="button-glyph">＋</span> 新建配置</el-button>
          <el-button :disabled="!selectedItem" @click="openItemEdit(selectedItem)">编辑</el-button>
          <el-button :disabled="!selectedItem" @click="confirmItemDelete(selectedItem)">删除</el-button>
          <el-button :disabled="!selectedItem || selectedItem.status === 'ENABLED'" @click="setStatus('ENABLED')">启用</el-button>
          <el-button :disabled="!selectedItem || selectedItem.status === 'DISABLED'" @click="setStatus('DISABLED')">停用</el-button>
          <span class="toolbar-divider" />
          <el-button @click="notImplemented('导入')">导入</el-button>
          <el-button @click="notImplemented('导出')">导出</el-button>
          <el-dropdown @command="handleMoreCommand">
            <el-button>更多 <span class="caret">⌄</span></el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="namespaces">命名空间信息</el-dropdown-item>
                <el-dropdown-item command="copy" :disabled="!selectedItem">复制完整 Key</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
        <div class="search-actions">
          <el-input ref="topSearchRef" v-model.trim="keyword" placeholder="搜索 Key / 值 / 说明" clearable @keyup.enter="loadItems">
            <template #suffix><span class="search-glyph">⌕</span></template>
          </el-input>
          <el-button circle title="筛选" @click="notImplemented('高级筛选')">⏷</el-button>
          <el-button circle title="刷新" @click="reloadWorkspace">↻</el-button>
        </div>
      </header>

      <div class="breadcrumb-bar">
        <span>当前位置：</span>
        <template v-for="(segment, index) in breadcrumb" :key="segment.path">
          <button type="button" :class="{ active: index === breadcrumb.length - 1 }" @click="selectPath(segment.path)">{{ segment.label }}</button>
          <span v-if="index < breadcrumb.length - 1" class="breadcrumb-separator">›</span>
        </template>
      </div>

      <section class="list-section">
        <el-table
          ref="tableRef"
          v-loading="itemsLoading"
          class="items-table"
          :data="visibleItems"
          row-key="id"
          highlight-current-row
          @row-click="selectItem"
          @row-dblclick="openItemEdit"
          @selection-change="selectedRows = $event"
        >
          <el-table-column type="selection" width="42" />
          <el-table-column label="Key" min-width="265" class-name="code-cell">
            <template #default="{ row }"><span class="item-key">{{ row.itemKey }}</span></template>
          </el-table-column>
          <el-table-column label="值" min-width="150">
            <template #default="{ row }"><el-tooltip :content="row.itemValue"><span class="value-preview">{{ row.itemValue }}</span></el-tooltip></template>
          </el-table-column>
          <el-table-column label="类型" width="104">
            <template #default="{ row }"><span class="type-tag" :class="`type-${row.valueType?.toLowerCase()}`">{{ typeLabel(row.valueType) }}</span></template>
          </el-table-column>
          <el-table-column label="状态" width="105">
            <template #default="{ row }"><span class="status-tag" :class="row.status === 'DISABLED' ? 'disabled' : ''"><i />{{ row.status === 'DISABLED' ? '停用' : '启用' }}</span></template>
          </el-table-column>
          <el-table-column label="更新时间" width="176"><template #default="{ row }">{{ formatTime(row.updatedAt) }}</template></el-table-column>
          <el-table-column label="说明" min-width="175"><template #default="{ row }"><span class="description-cell">{{ row.description || '—' }}</span></template></el-table-column>
        </el-table>
        <footer class="table-footer">
          <span>共 {{ visibleItems.length }} 条</span>
          <div><span>20 条/页</span><button type="button" aria-label="上一页">‹</button><button type="button" class="current-page">1</button><button type="button" aria-label="下一页">›</button></div>
        </footer>
      </section>

      <section class="details-section">
        <div class="details-title"><span class="accent-line" />配置详情 <span v-if="!selectedItem" class="details-empty">选择一项配置以查看详情</span></div>
        <template v-if="selectedItem">
          <div class="details-grid">
            <div class="detail-form">
              <div class="detail-field wide"><label>完整 Key</label><div class="read-value code-value"><span>{{ selectedItem.itemKey }}</span><button type="button" title="复制 Key" @click="copyKey">⧉</button></div></div>
              <div class="detail-field"><label>类型</label><div class="read-value"><span class="type-tag" :class="`type-${selectedItem.valueType?.toLowerCase()}`">{{ typeLabel(selectedItem.valueType) }}</span></div></div>
              <div class="detail-field"><label>值</label><div class="read-value code-value">{{ selectedItem.itemValue }}</div></div>
              <div class="detail-field"><label>默认值</label><div class="read-value code-value">{{ selectedItem.defaultValue || '未设置' }}</div></div>
              <div class="detail-field wide"><label>说明</label><div class="read-value">{{ selectedItem.description || '未填写说明' }}</div></div>
              <div class="detail-field"><label>状态</label><el-radio-group :model-value="selectedItem.status === 'DISABLED' ? 'DISABLED' : 'ENABLED'" size="small" @change="setStatus"><el-radio value="ENABLED">启用</el-radio><el-radio value="DISABLED">停用</el-radio></el-radio-group></div>
              <div class="detail-field"><label>更新时间</label><div class="read-value">{{ formatTime(selectedItem.updatedAt) }}</div></div>
              <div class="detail-field"><label>创建时间</label><div class="read-value">{{ formatTime(selectedItem.createdAt) }}</div></div>
            </div>
            <aside class="key-guide">
              <p class="guide-title">命名空间路径</p>
              <div class="path-chips"><span v-for="part in selectedItem.itemKey.split(':').slice(0, -1)" :key="part">{{ part }}</span></div>
              <div class="guide-card"><strong>命名规则</strong><ol><li>Key 必须以小写字母开头。</li><li>使用冒号 <code>:</code> 分隔层级。</li><li>每段允许字母、数字、下划线和短横线。</li><li>至少包含三个层级，不允许空段。</li></ol><p>示例：system:module:feature:name</p></div>
              <div class="guide-card"><strong>支持的数据类型</strong><p><span class="type-tag type-string">字符串</span> 任意文本内容</p><p><span class="type-tag type-integer">整数</span> 64 位整数</p><p><span class="type-tag type-boolean">布尔</span> true / false</p></div>
            </aside>
          </div>
        </template>
      </section>
    </main>

    <el-drawer v-model="itemEditorVisible" :title="itemEditorMode === 'create' ? '新建配置' : '编辑配置'" size="520px" destroy-on-close>
      <el-form ref="itemFormRef" :model="itemForm" :rules="itemRules" label-position="top">
        <el-form-item label="所属命名空间" prop="namespaceId"><el-select v-model="itemForm.namespaceId" :disabled="itemEditorMode === 'edit'" placeholder="请选择命名空间"><el-option v-for="namespace in namespaces" :key="namespace.id" :label="namespaceLabel(namespace)" :value="namespace.id" /></el-select></el-form-item>
        <el-form-item label="Key" prop="itemKey"><el-input v-model.trim="itemForm.itemKey" placeholder="system:auth:login:maxRetry" /><div class="key-preview"><span>命名空间：{{ keyAnalysis.namespace || '—' }}</span><span>配置名称：{{ keyAnalysis.name || '—' }}</span></div></el-form-item>
        <el-form-item label="类型" prop="valueType"><el-select v-model="itemForm.valueType" @change="validateValue"><el-option label="字符串 String" value="STRING" /><el-option label="整数 Integer" value="INTEGER" /><el-option label="布尔 Boolean" value="BOOLEAN" /></el-select></el-form-item>
        <el-form-item label="值" prop="itemValue"><el-input-number v-if="itemForm.valueType === 'INTEGER'" v-model="numericEditorValue" :precision="0" :step="1" controls-position="right" /><el-switch v-else-if="itemForm.valueType === 'BOOLEAN'" v-model="booleanEditorValue" active-text="true" inactive-text="false" /><el-input v-else v-model="itemForm.itemValue" placeholder="请输入字符串值" /></el-form-item>
        <el-form-item label="默认值"><el-input-number v-if="itemForm.valueType === 'INTEGER'" v-model="numericDefaultValue" :precision="0" :step="1" controls-position="right" /><el-switch v-else-if="itemForm.valueType === 'BOOLEAN'" v-model="booleanDefaultValue" active-text="true" inactive-text="false" /><el-input v-else v-model="itemForm.defaultValue" placeholder="可选" /></el-form-item>
        <el-alert v-if="valueError" :title="valueError" type="error" :closable="false" class="value-error" />
        <el-form-item label="说明"><el-input v-model.trim="itemForm.description" type="textarea" :rows="3" placeholder="说明该配置的用途" /></el-form-item>
        <el-form-item label="状态"><el-radio-group v-model="itemForm.status"><el-radio value="ENABLED">启用</el-radio><el-radio value="DISABLED">停用</el-radio></el-radio-group></el-form-item>
      </el-form>
      <template #footer><div class="drawer-footer"><el-button @click="itemEditorVisible = false">取消</el-button><el-button type="primary" :loading="itemSaving" @click="submitItem">保存配置</el-button></div></template>
    </el-drawer>
  </section>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { deleteItem, fetchItemDetail, fetchItems, fetchNamespaces, saveItem } from '../api/configAdminApi'

const namespaces = ref([])
const items = ref([])
const keyword = ref('')
const treeKeyword = ref('')
const selectedNode = ref()
const selectedItem = ref()
const selectedRows = ref([])
const expandedKeys = ref([])
const treeVersion = ref(0)
const itemsLoading = ref(false)
const itemSaving = ref(false)
const itemEditorVisible = ref(false)
const itemEditorMode = ref('create')
const valueError = ref('')
const treeRef = ref()
const tableRef = ref()
const topSearchRef = ref()
const itemFormRef = ref()
const itemForm = reactive(newItem())
const treeProps = { label: 'label', children: 'children' }
const keyPattern = /^[a-z][A-Za-z0-9_-]{0,63}(?::[a-z][A-Za-z0-9_-]{0,63}){2,}$/
const itemRules = {
  namespaceId: [{ required: true, message: '请选择所属命名空间', trigger: 'change' }],
  itemKey: [{ validator: (_rule, value, callback) => keyPattern.test(value || '') ? callback() : callback(new Error('Key 至少包含三个冒号分隔的段，且每段以小写字母开头')), trigger: 'blur' }],
  valueType: [{ required: true, message: '请选择数据类型', trigger: 'change' }],
  itemValue: [{ required: true, message: '请输入配置值', trigger: 'blur' }]
}

const treeData = computed(() => buildKeyTree(items.value))
const filteredTreeData = computed(() => filterTree(treeData.value, treeKeyword.value))
const visibleItems = computed(() => {
  if (!selectedNode.value) return items.value
  if (selectedNode.value.kind === 'item') return items.value.filter((item) => item.id === selectedNode.value.itemId)
  const ids = new Set(selectedNode.value.itemIds || [])
  return items.value.filter((item) => ids.has(item.id))
})
const breadcrumb = computed(() => {
  const parts = selectedNode.value?.path?.split(':') || []
  if (!parts.length) return [{ label: '全部配置', path: '' }]
  return parts.map((label, index) => ({ label, path: parts.slice(0, index + 1).join(':') }))
})
const keyAnalysis = computed(() => {
  const parts = (itemForm.itemKey || '').split(':').filter(Boolean)
  return { namespace: parts.slice(0, -1).join(' > '), name: parts.at(-1) || '' }
})
const numericEditorValue = computed({ get: () => itemForm.itemValue === '' ? undefined : Number(itemForm.itemValue), set: (value) => { itemForm.itemValue = value == null ? '' : String(value) } })
const numericDefaultValue = computed({ get: () => itemForm.defaultValue === '' ? undefined : Number(itemForm.defaultValue), set: (value) => { itemForm.defaultValue = value == null ? '' : String(value) } })
const booleanEditorValue = computed({ get: () => itemForm.itemValue === 'true', set: (value) => { itemForm.itemValue = value ? 'true' : 'false' } })
const booleanDefaultValue = computed({ get: () => itemForm.defaultValue === 'true', set: (value) => { itemForm.defaultValue = value ? 'true' : 'false' } })

watch(treeData, (tree) => {
  if (!selectedNode.value) expandedKeys.value = tree.map((node) => node.nodeKey)
})
watch(keyword, () => { window.clearTimeout(loadItems.timer); loadItems.timer = window.setTimeout(loadItems, 220) })

onMounted(async () => {
  await Promise.all([loadNamespaces(), loadItems()])
  window.addEventListener('keydown', handleShortcut)
})
onBeforeUnmount(() => window.removeEventListener('keydown', handleShortcut))

async function loadNamespaces() {
  try { namespaces.value = await fetchNamespaces() } catch (error) { ElMessage.error(error.message || '加载命名空间失败') }
}

async function loadItems() {
  itemsLoading.value = true
  const currentId = selectedItem.value?.id
  try {
    items.value = await fetchItems(undefined, keyword.value)
    selectedItem.value = items.value.find((item) => item.id === currentId) || selectedItem.value
    treeVersion.value++
  } catch (error) {
    ElMessage.error(error.message || '加载配置项失败')
  } finally {
    itemsLoading.value = false
  }
}

async function reloadWorkspace() {
  await Promise.all([loadNamespaces(), loadItems()])
  ElMessage.success('配置目录已刷新')
}

function handleTreeNodeClick(node) {
  selectedNode.value = node
  if (node.kind === 'item') {
    const item = items.value.find((row) => row.id === node.itemId)
    if (item) selectItem(item)
  }
}

function selectPath(path) {
  if (!path) {
    selectedNode.value = undefined
    treeRef.value?.setCurrentKey(undefined)
    return
  }
  const node = findTreeNode(treeData.value, `directory:${path}`)
  if (node) {
    selectedNode.value = node
    treeRef.value?.setCurrentKey(node.nodeKey)
  }
}

function selectItem(row) {
  selectedItem.value = row
  tableRef.value?.setCurrentRow(row)
}

async function openItemCreate() {
  Object.assign(itemForm, newItem({ namespaceId: namespaces.value[0]?.id ?? null }))
  itemEditorMode.value = 'create'
  valueError.value = ''
  itemEditorVisible.value = true
  await nextTick()
  itemFormRef.value?.clearValidate()
}

async function openItemEdit(row) {
  if (!row) return
  try {
    Object.assign(itemForm, newItem(await fetchItemDetail(row.id)))
    if (!['STRING', 'INTEGER', 'BOOLEAN'].includes(itemForm.valueType)) {
      ElMessage.warning('该历史配置使用了已退役的数据类型，请先迁移为字符串、整数或布尔值。')
    }
    itemEditorMode.value = 'edit'
    valueError.value = ''
    itemEditorVisible.value = true
    await nextTick()
    itemFormRef.value?.clearValidate()
  } catch (error) {
    ElMessage.error(error.message || '加载配置详情失败')
  }
}

function validateValue() {
  const checks = [itemForm.itemValue, itemForm.defaultValue].filter((value) => value !== null && value !== undefined && value !== '')
  if (itemForm.valueType === 'INTEGER') valueError.value = checks.every((value) => /^-?(0|[1-9]\d*)$/.test(value)) ? '' : '当前值无法转换为整数。'
  else if (itemForm.valueType === 'BOOLEAN') valueError.value = checks.every((value) => value === 'true' || value === 'false') ? '' : '布尔值只能为 true 或 false。'
  else valueError.value = ''
}

async function submitItem() {
  try {
    validateValue()
    await itemFormRef.value?.validate()
    if (valueError.value) return
    itemSaving.value = true
    await saveItem({ ...itemForm })
    itemEditorVisible.value = false
    await reloadWorkspace()
    ElMessage.success(itemEditorMode.value === 'create' ? '配置已创建' : '配置已保存')
  } catch (error) {
    if (error?.message) ElMessage.error(error.message)
  } finally {
    itemSaving.value = false
  }
}

async function setStatus(status) {
  if (!selectedItem.value || selectedItem.value.status === status) return
  try {
    const detail = await fetchItemDetail(selectedItem.value.id)
    await saveItem({ ...detail, status })
    selectedItem.value = { ...detail, status }
    const index = items.value.findIndex((item) => item.id === detail.id)
    if (index >= 0) items.value[index] = selectedItem.value
    ElMessage.success(status === 'ENABLED' ? '配置已启用' : '配置已停用')
  } catch (error) {
    ElMessage.error(error.message || '更新配置状态失败')
  }
}

async function confirmItemDelete(row) {
  if (!row) return
  try {
    await ElMessageBox.confirm(`确定删除配置“${row.itemKey}”吗？`, '删除配置', { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' })
    await deleteItem(row.id)
    selectedItem.value = undefined
    selectedNode.value = undefined
    await reloadWorkspace()
    ElMessage.success('配置已删除')
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') ElMessage.error(error.message || '删除配置失败')
  }
}

async function copyKey() {
  if (!selectedItem.value) return
  try { await navigator.clipboard.writeText(selectedItem.value.itemKey); ElMessage.success('完整 Key 已复制') } catch { ElMessage.warning('浏览器未授予剪贴板权限') }
}

function handleMoreCommand(command) {
  if (command === 'copy') void copyKey()
  if (command === 'namespaces') ElMessage.info(`当前共 ${namespaces.value.length} 个命名空间；新建配置时可选择归属命名空间。`)
}
function notImplemented(name) { ElMessage.info(`${name}功能将在后续版本提供。`) }
function handleShortcut(event) {
  if (event.ctrlKey && event.key.toLowerCase() === 'f') { event.preventDefault(); topSearchRef.value?.focus() }
  if (event.ctrlKey && event.key.toLowerCase() === 's' && selectedItem.value) { event.preventDefault(); void openItemEdit(selectedItem.value) }
  if (event.key === 'Delete' && selectedItem.value && !itemEditorVisible.value) { event.preventDefault(); void confirmItemDelete(selectedItem.value) }
}
function namespaceLabel(namespace) { return `${namespace.displayName} (${namespace.namespaceCode} / ${namespace.env})` }
function typeLabel(type) { return ({ STRING: '字符串', INTEGER: '整数', BOOLEAN: '布尔' })[type] || type }
function formatTime(value) { return value ? new Date(value).toLocaleString('sv-SE').replace('T', ' ') : '—' }
function newItem(source = {}) { return { id: source.id ?? null, namespaceId: source.namespaceId ?? null, directoryId: null, itemKey: source.itemKey ?? '', itemValue: source.itemValue ?? '', defaultValue: source.defaultValue ?? '', valueType: source.valueType ?? 'STRING', status: source.status ?? 'ENABLED', description: source.description ?? '' } }

function buildKeyTree(source) {
  const roots = [], directories = new Map()
  for (const item of source) {
    const parts = item.itemKey.split(':')
    let children = roots
    let path = ''
    parts.forEach((part, index) => {
      path = path ? `${path}:${part}` : part
      if (index === parts.length - 1) {
        children.push({ nodeKey: `item:${item.id}`, label: part, path, kind: 'item', itemId: item.id, itemCount: 1, fullKey: item.itemKey, children: [] })
        return
      }
      let node = directories.get(path)
      if (!node) {
        node = { nodeKey: `directory:${path}`, label: part, path, kind: 'directory', itemIds: [], itemCount: 0, children: [] }
        directories.set(path, node)
        children.push(node)
      }
      node.itemIds.push(item.id)
      node.itemCount++
      children = node.children
    })
  }
  return roots
}

function filterTree(nodes, rawKeyword) {
  const query = rawKeyword.trim().toLowerCase()
  if (!query) return nodes
  return nodes.reduce((result, node) => {
    const children = filterTree(node.children || [], query)
    if (node.label.toLowerCase().includes(query) || node.fullKey?.toLowerCase().includes(query) || children.length) result.push({ ...node, children })
    return result
  }, [])
}

function findTreeNode(nodes, key) {
  for (const node of nodes) {
    if (node.nodeKey === key) return node
    const match = findTreeNode(node.children || [], key)
    if (match) return match
  }
}
</script>

<style scoped>
.config-manager { display: grid; grid-template-columns: 282px minmax(0, 1fr); min-height: calc(100vh - 40px); background: var(--shell-tool-surface); color: var(--shell-text-primary); border-top: 1px solid var(--shell-tool-divider); font-size: 13px; }
.resource-explorer { display: flex; min-width: 0; flex-direction: column; background: var(--shell-tool-surface-muted); border-right: 1px solid var(--shell-tool-divider); }
.explorer-header { display: flex; align-items: flex-start; justify-content: space-between; padding: 18px 16px 13px; }.eyebrow { margin: 0 0 4px; color: var(--shell-accent); font-family: Consolas, monospace; font-size: 10px; letter-spacing: .12em; }.explorer-header h2 { margin: 0; color: var(--shell-tool-header-text); font-size: 14px; font-weight: 650; }.item-total { min-width: 30px; padding: 3px 7px; border-radius: 4px; background: var(--shell-tool-tag-bg); color: var(--shell-tool-subtle-text); font-family: Consolas, monospace; font-size: 11px; text-align: center; }
.tree-search { padding: 0 14px; }.tree-search :deep(.el-input__wrapper), .search-actions :deep(.el-input__wrapper) { background: var(--shell-tool-toolbar-bg); box-shadow: 0 0 0 1px var(--shell-tool-border-strong) inset; border-radius: 4px; }.search-glyph { color: var(--shell-tool-subtle-text); font-size: 19px; line-height: 1; }.tree-caption { display: flex; justify-content: space-between; margin: 18px 16px 7px; color: var(--shell-tool-subtle-text); font-size: 11px; font-weight: 600; }.tree-caption span:last-child { font-weight: 400; }
.key-tree { flex: 1; min-height: 0; overflow: auto; padding: 0 8px 12px; background: transparent; color: var(--shell-text-primary); --el-tree-node-hover-bg-color: var(--shell-tool-hover); }.key-tree :deep(.el-tree-node__content) { height: 27px; border-radius: 4px; }.key-tree :deep(.el-tree-node__expand-icon) { color: var(--shell-tool-subtle-text); font-size: 12px; }.key-tree :deep(.is-current > .el-tree-node__content) { background: var(--shell-tool-selected-bg); color: var(--shell-accent); }.tree-node, .tree-node-main { display: flex; align-items: center; }.tree-node { width: 100%; justify-content: space-between; gap: 8px; }.tree-node-main { min-width: 0; gap: 6px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }.tree-icon { width: 13px; color: #d6a153; font-size: 11px; }.tree-node.is-key .tree-icon { color: var(--shell-accent); font-size: 10px; }.tree-node small { min-width: 22px; padding: 1px 5px; border-radius: 3px; background: var(--shell-tool-tag-bg); color: var(--shell-text-muted); font-size: 10px; text-align: center; }
.explorer-footer { display: flex; gap: 4px; padding: 10px 10px; border-top: 1px solid var(--shell-tool-divider); }.explorer-footer :deep(.el-button) { margin: 0; padding: 5px; color: var(--shell-text-secondary); font-size: 12px; }.button-glyph { margin-right: 3px; font-size: 15px; }.config-workspace { display: grid; min-width: 0; grid-template-rows: 62px 42px minmax(310px, 1fr) minmax(305px, .82fr); }
.workspace-toolbar, .breadcrumb-bar, .list-section, .details-section { min-width: 0; }.workspace-toolbar { display: flex; align-items: center; justify-content: space-between; gap: 16px; padding: 11px 16px; border-bottom: 1px solid var(--shell-tool-divider); background: var(--shell-tool-surface); }.toolbar-actions, .search-actions { display: flex; align-items: center; gap: 6px; }.toolbar-actions { min-width: 0; overflow: auto; }.toolbar-actions :deep(.el-button), .search-actions :deep(.el-button) { min-height: 30px; margin: 0; border-radius: 4px; font-size: 12px; white-space: nowrap; }.toolbar-divider { width: 1px; height: 20px; margin: 0 3px; background: var(--shell-tool-divider); }.search-actions { flex: 0 0 auto; }.search-actions :deep(.el-input) { width: 248px; }.search-actions :deep(.el-button) { width: 30px; padding: 0; font-size: 16px; }
.breadcrumb-bar { display: flex; align-items: center; gap: 7px; padding: 0 16px; border-bottom: 1px solid var(--shell-tool-divider); background: var(--shell-tool-toolbar-bg); color: var(--shell-tool-subtle-text); font-size: 12px; }.breadcrumb-bar button { padding: 3px 4px; border: 0; border-radius: 3px; background: transparent; color: var(--shell-text-secondary); cursor: pointer; }.breadcrumb-bar button:hover, .breadcrumb-bar button.active { background: var(--shell-tool-hover); color: var(--shell-accent); }.breadcrumb-separator { color: var(--shell-text-muted); font-size: 16px; }
.list-section { display: flex; min-height: 0; flex-direction: column; padding: 0 16px; border-bottom: 1px solid var(--shell-tool-divider); }.items-table { flex: 1; --el-table-border-color: var(--shell-tool-divider); --el-table-header-bg-color: var(--shell-tool-toolbar-bg); --el-table-tr-bg-color: transparent; --el-table-row-hover-bg-color: var(--shell-tool-hover); --el-table-current-row-bg-color: var(--shell-tool-selected-bg); font-size: 12px; }.items-table :deep(th.el-table__cell) { height: 40px; padding: 0; color: var(--shell-tool-subtle-text); font-size: 12px; font-weight: 600; }.items-table :deep(.el-table__cell) { height: 37px; padding: 0; }.items-table :deep(.code-cell .cell) { font-family: Consolas, "JetBrains Mono", monospace; }.items-table :deep(.el-table__inner-wrapper::before) { height: 0; }.item-key { color: var(--shell-text-primary); }.value-preview, .description-cell { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }.value-preview { font-family: Consolas, "JetBrains Mono", monospace; }.description-cell { color: var(--shell-text-secondary); }.type-tag, .status-tag { display: inline-flex; align-items: center; min-height: 21px; padding: 1px 6px; border-radius: 3px; font-size: 11px; font-weight: 600; line-height: 1.4; }.type-string { background: color-mix(in srgb, #8a6fd4 22%, transparent); color: #9f8be8; }.type-integer { background: color-mix(in srgb, #4b92dc 22%, transparent); color: #6ba9e9; }.type-boolean { background: color-mix(in srgb, #76a64f 22%, transparent); color: #92c66b; }.status-tag { gap: 5px; background: color-mix(in srgb, var(--shell-accent) 15%, transparent); color: var(--shell-accent); }.status-tag i { width: 6px; height: 6px; border-radius: 50%; background: currentColor; }.status-tag.disabled { background: color-mix(in srgb, #a0a7b1 16%, transparent); color: var(--shell-text-muted); }.table-footer { display: flex; align-items: center; justify-content: space-between; height: 48px; color: var(--shell-text-secondary); font-size: 12px; }.table-footer div { display: flex; align-items: center; gap: 8px; }.table-footer button { width: 25px; height: 25px; border: 1px solid var(--shell-tool-border-strong); border-radius: 3px; background: var(--shell-tool-surface); color: var(--shell-text-secondary); cursor: pointer; }.table-footer button.current-page { border-color: var(--shell-accent); color: var(--shell-accent); }
.details-section { min-height: 0; padding: 0 16px 12px; overflow: auto; background: var(--shell-tool-surface-muted); }.details-title { display: flex; align-items: center; gap: 8px; height: 44px; color: var(--shell-tool-header-text); font-size: 13px; font-weight: 650; }.accent-line { width: 2px; height: 17px; background: var(--shell-accent); }.details-empty { color: var(--shell-text-muted); font-size: 12px; font-weight: 400; }.details-grid { display: grid; grid-template-columns: minmax(510px, 1.16fr) minmax(340px, .84fr); gap: 14px; }.detail-form { display: grid; grid-template-columns: 1fr 1fr; gap: 12px 18px; padding-right: 4px; }.detail-field { min-width: 0; }.detail-field.wide { grid-column: 1 / -1; }.detail-field label { display: block; margin-bottom: 6px; color: var(--shell-text-secondary); font-size: 12px; }.read-value { display: flex; min-height: 31px; align-items: center; padding: 5px 10px; overflow: hidden; border: 1px solid var(--shell-tool-border-strong); border-radius: 4px; background: var(--shell-tool-surface); color: var(--shell-text-primary); }.code-value { justify-content: space-between; font-family: Consolas, "JetBrains Mono", monospace; }.code-value span { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }.code-value button { padding: 0 0 0 10px; border: 0; background: transparent; color: var(--shell-text-secondary); cursor: pointer; font-size: 16px; }.detail-field :deep(.el-radio) { margin-right: 14px; }.key-guide { padding: 0 0 0 14px; border-left: 1px solid var(--shell-tool-divider); }.guide-title { margin: 0 0 9px; color: var(--shell-text-secondary); font-size: 12px; }.path-chips { display: flex; flex-wrap: wrap; gap: 6px; margin-bottom: 10px; }.path-chips span { padding: 4px 9px; border: 1px solid var(--shell-tool-border-strong); border-radius: 3px; background: var(--shell-tool-surface); font-family: Consolas, monospace; font-size: 11px; }.guide-card { margin-top: 10px; padding: 10px 12px; border: 1px solid var(--shell-tool-divider); border-radius: 4px; background: color-mix(in srgb, var(--shell-tool-surface) 80%, transparent); color: var(--shell-text-secondary); font-size: 11px; line-height: 1.55; }.guide-card strong { color: var(--shell-text-primary); font-size: 12px; }.guide-card ol { margin: 6px 0; padding-left: 17px; }.guide-card p { display: flex; align-items: center; gap: 6px; margin: 5px 0 0; }.guide-card code { color: var(--shell-accent); }
.key-preview { display: flex; flex-wrap: wrap; gap: 10px; margin-top: 7px; color: var(--shell-tool-subtle-text); font-size: 11px; }.value-error { margin: -5px 0 12px; }.drawer-footer { display: flex; justify-content: flex-end; gap: 8px; }.el-input-number { width: 100%; }
@media (max-width: 1120px) { .config-workspace { grid-template-rows: 62px 42px minmax(310px, 1fr) auto; }.details-grid { grid-template-columns: 1fr; }.key-guide { padding: 12px 0 0; border-top: 1px solid var(--shell-tool-divider); border-left: 0; }.search-actions :deep(.el-input) { width: 180px; } }
@media (max-width: 860px) { .config-manager { grid-template-columns: 1fr; }.resource-explorer { max-height: 280px; border-right: 0; border-bottom: 1px solid var(--shell-tool-divider); }.config-workspace { grid-template-rows: auto 42px 370px auto; }.workspace-toolbar { align-items: flex-start; flex-direction: column; }.toolbar-actions { width: 100%; }.search-actions { width: 100%; }.search-actions :deep(.el-input) { flex: 1; width: auto; }.detail-form { grid-template-columns: 1fr; }.detail-field.wide { grid-column: auto; } }
</style>
