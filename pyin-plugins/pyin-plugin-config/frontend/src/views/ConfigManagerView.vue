<template>
  <section class="config-manager" tabindex="-1">
    <el-splitter layout="horizontal" class="config-splitter">
      <el-splitter-panel :size="sidebarPanelSize" :min="sidebarPanelMin" max="75%" @update:size="onSidebarSizeChange">
        <aside class="resource-explorer">
          <nav class="tool-rail" aria-label="配置工作区工具栏">
            <button :class="{ active: spacePanelVisible }" title="空间" aria-label="空间" @click="toggleSidebarPanel('spaces')">空间</button>
          </nav>
          <div class="sidebar-content">
            <el-splitter v-if="spacePanelVisible" layout="horizontal" class="side-panel-splitter">
              <el-splitter-panel size="50%" min="25%">
                <ConfigSpaceManager
                  :spaces="namespaces"
                  :active-space-id="activeNamespaceId"
                  @select="selectNamespaceFromManager"
                  @create="openNamespaceCreate"
                  @edit="openNamespaceEdit"
                  @delete="confirmNamespaceDelete"
                  @refresh="reloadWorkspace"
                />
              </el-splitter-panel>
              <el-splitter-panel :min="210">
                <ConfigDirectoryTree
                  ref="treeRef"
                  :items="items"
                  @node-click="handleTreeNodeClick"
                  @create-item="openItemCreate"
                  @edit-item="openTreeItemEdit"
                  @copy-key="copyTreeItemKey"
                  @set-status="setTreeItemStatus"
                  @delete-item="deleteTreeItem"
                  @refresh="reloadWorkspace"
                />
              </el-splitter-panel>
            </el-splitter>
            <ConfigSpaceManager
              v-else-if="spacePanelVisible"
              :spaces="namespaces"
              :active-space-id="activeNamespaceId"
              @select="selectNamespaceFromManager"
              @create="openNamespaceCreate"
              @edit="openNamespaceEdit"
              @delete="confirmNamespaceDelete"
              @refresh="reloadWorkspace"
            />
            <ConfigDirectoryTree
              v-else
              ref="treeRef"
              :items="items"
              @node-click="handleTreeNodeClick"
              @create-item="openItemCreate"
              @edit-item="openTreeItemEdit"
              @copy-key="copyTreeItemKey"
              @set-status="setTreeItemStatus"
              @delete-item="deleteTreeItem"
              @refresh="reloadWorkspace"
            />
          </div>
        </aside>
      </el-splitter-panel>

      <el-splitter-panel>
        <main class="config-workspace">
      <section class="details-section">
        <div class="details-title"><span class="accent-line" /><span v-if="isDetailDraft" class="draft-indicator" aria-hidden="true" /><span v-if="detailChanged" class="modified-indicator" aria-hidden="true" />{{ isDetailDraft ? '新建配置' : '配置详情' }} <span v-if="selectedItem" class="detail-space-inline">所属空间：{{ detailSpaceLabel }}</span><span v-else class="details-empty">选择一项配置以直接编辑</span><el-button v-if="selectedItem" class="detail-delete-button" size="small" type="danger" plain @click="confirmItemDelete(selectedItem)">删除</el-button><el-button v-if="selectedItem" type="primary" size="small" :loading="detailSaving" :disabled="!detailChanged" @click="submitDetail">保存</el-button></div>
        <template v-if="selectedItem">
          <el-form ref="detailFormRef" class="detail-editor" :model="detailForm" :rules="itemRules" label-position="top" @submit.prevent="submitDetail">
            <el-form-item label="Key" prop="itemKey" class="detail-key-field"><el-input v-model="detailForm.itemKey" readonly><template #append><button class="copy-key-button" type="button" title="复制 Key" @click="copyKey(detailForm)">⧉</button></template></el-input><div class="key-preview"><span>空间路径：{{ detailKeyAnalysis.namespace || '—' }}</span><span>配置名称：{{ detailKeyAnalysis.name || '—' }}</span></div></el-form-item>
            <el-form-item label="说明" class="detail-description-field"><el-input v-model.trim="detailForm.description" placeholder="说明该配置的用途" /></el-form-item>
            <el-form-item label="类型" prop="valueType"><el-select v-model="detailForm.valueType" disabled><el-option label="String" value="STRING" /><el-option label="Integer" value="INTEGER" /><el-option label="Boolean" value="BOOLEAN" /><el-option label="JSON" value="JSON" /></el-select></el-form-item>
            <el-form-item label="状态"><el-radio-group v-model="detailForm.status"><el-radio value="ENABLED">启用</el-radio><el-radio value="DISABLED">停用</el-radio></el-radio-group></el-form-item>
            <section class="value-editor-block wide">
              <div class="value-editor-heading"><span>当前值</span><small>修改后点击右上角“保存”生效</small></div>
              <el-form-item prop="itemValue" :show-message="detailForm.valueType !== 'JSON'">
                <JsonValueEditor v-if="detailForm.valueType === 'JSON'" v-model="detailForm.itemValue" :validation-message="detailValueError" @validation-change="handleDetailJsonValidation" />
                <el-input v-else v-model="detailForm.itemValue" type="textarea" :rows="16" resize="vertical" placeholder="请输入配置值" @input="restrictDetailValue" />
              </el-form-item>
            </section>
            <el-alert v-if="detailValueError && detailForm.valueType !== 'JSON'" :title="detailValueError" type="error" :closable="false" class="detail-value-error wide" />
          </el-form>
        </template>
      </section>
        </main>
      </el-splitter-panel>
    </el-splitter>

    <el-dialog v-model="itemEditorVisible" class="config-editor-dialog item-editor-dialog" width="480px" :show-close="false" destroy-on-close :close-on-click-modal="false">
      <el-form ref="itemFormRef" class="item-editor-form" :model="itemForm" :rules="itemRules" label-position="right" label-width="42px">
        <el-form-item label="Key" prop="itemKey" :show-message="false"><el-input v-model.trim="itemForm.itemKey" placeholder="例如：order:timeout 或 timeout" @blur="keyTouched = true" /><div class="create-key-hint" :class="{ invalid: keyTouched && !keyPattern.test(itemForm.itemKey || '') }">{{ keyTouched && !keyPattern.test(itemForm.itemKey || '') ? 'Key 仅支持字母、数字、下划线、短横线和冒号分层。' : '支持单个 Key 或使用冒号分层。' }}</div></el-form-item>
        <el-form-item label="类型" prop="valueType"><el-select v-model="itemForm.valueType" @change="validateValue"><el-option label="String" value="STRING" /><el-option label="Integer" value="INTEGER" /><el-option label="Boolean" value="BOOLEAN" /><el-option label="JSON" value="JSON" /></el-select></el-form-item>
      </el-form>
      <template #footer><div class="drawer-footer"><el-button @click="itemEditorVisible = false">取消</el-button><el-button type="primary" :loading="itemSaving" @click="continueItemCreate">下一步</el-button></div></template>
    </el-dialog>

    <el-dialog v-model="namespaceDialogVisible" class="config-editor-dialog namespace-editor-dialog" :title="namespaceEditorMode === 'create' ? '新建空间' : '编辑空间'" width="430px" destroy-on-close :close-on-click-modal="false" @closed="resetNamespaceDraft">
      <el-form ref="namespaceFormRef" :model="namespaceForm" :rules="namespaceRules" label-position="top" @submit.prevent="submitNamespace">
        <el-form-item label="空间名称" prop="displayName"><el-input v-model.trim="namespaceForm.displayName" maxlength="128" placeholder="例如：订单服务生产空间" /></el-form-item>
        <el-form-item label="空间编码" prop="namespaceCode"><el-input v-model.trim="namespaceForm.namespaceCode" maxlength="128" placeholder="例如：order-service" /></el-form-item>
        <el-form-item label="环境" prop="env"><el-input v-model.trim="namespaceForm.env" maxlength="64" placeholder="例如：prod" /></el-form-item>
        <el-form-item label="说明"><el-input v-model.trim="namespaceForm.description" type="textarea" :rows="3" maxlength="512" show-word-limit placeholder="可选" /></el-form-item>
      </el-form>
      <template #footer><div class="drawer-footer"><el-button @click="namespaceDialogVisible = false">取消</el-button><el-button type="primary" :loading="namespaceSaving" @click="submitNamespace">保存空间</el-button></div></template>
    </el-dialog>
  </section>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import ConfigDirectoryTree from '../components/ConfigDirectoryTree.vue'
import ConfigSpaceManager from '../components/ConfigSpaceManager.vue'
import JsonValueEditor from '../components/JsonValueEditor.vue'
import { deleteItem, deleteNamespace, fetchItemDetail, fetchItems, fetchNamespaces, saveItem, saveNamespace } from '../api/configAdminApi'

const namespaces = ref([])
const activeNamespaceId = ref(Number(localStorage.getItem('config.active-namespace-id')) || null)
const items = ref([])
const keyword = ref('')
const selectedNode = ref()
const selectedItem = ref()
const selectedRows = ref([])
const itemsLoading = ref(false)
const itemSaving = ref(false)
const itemEditorVisible = ref(false)
const itemEditorMode = ref('create')
const valueError = ref('')
const keyTouched = ref(false)
const spacePanelVisible = ref(localStorage.getItem('config.space-panel') !== 'collapsed')
const sidebarWidth = ref(Number(localStorage.getItem('config.sidebar-width-v2')) || null)
const sidebarPanelSize = computed(() => sidebarWidth.value == null ? '66%' : sidebarWidth.value + 28)
const sidebarPanelMin = computed(() => minimumSidebarWidth() + 28)
const treeRef = ref()
const tableRef = ref()
const topSearchRef = ref()
const itemFormRef = ref()
const itemForm = reactive(newItem())
const detailFormRef = ref()
const detailForm = reactive(newItem())
const detailSaving = ref(false)
const detailValueError = ref('')
const detailBaseline = ref('')
const namespaceDialogVisible = ref(false)
const namespaceEditorMode = ref('create')
const namespaceSaving = ref(false)
const namespaceFormRef = ref()
const namespaceForm = reactive(newNamespace())
const keyPattern = /^[a-z0-9][A-Za-z0-9_-]{0,63}(?::[a-z0-9][A-Za-z0-9_-]{0,63})*$/
const itemRules = {
  namespaceId: [{ required: true, message: '请选择所属空间', trigger: 'change' }],
  itemKey: [{ validator: (_rule, value, callback) => keyPattern.test(value || '') ? callback() : callback(new Error('Key 可直接填写，或使用冒号分隔层级；每段以小写字母或数字开头')), trigger: 'blur' }],
  valueType: [{ required: true, message: '请选择数据类型', trigger: 'change' }],
  itemValue: []
}
const namespaceRules = {
  displayName: [{ required: true, message: '请输入空间名称', trigger: 'blur' }],
  namespaceCode: [{ required: true, message: '请输入空间编码', trigger: 'blur' }],
  env: [{ required: true, message: '请输入环境', trigger: 'blur' }]
}

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
const detailKeyAnalysis = computed(() => {
  const parts = (detailForm.itemKey || '').split(':').filter(Boolean)
  return { namespace: parts.slice(0, -1).join(' > '), name: parts.at(-1) || '' }
})
const detailSpaceLabel = computed(() => namespaceLabel(namespaces.value.find((namespace) => namespace.id === detailForm.namespaceId)))
const isDetailDraft = computed(() => Boolean(selectedItem.value?.id) && (detailForm.itemValue == null || detailForm.itemValue === ''))
const detailChanged = computed(() => Boolean(selectedItem.value?.id) && detailBaseline.value !== detailSnapshot(detailForm))
const numericEditorValue = computed({ get: () => itemForm.itemValue === '' ? undefined : Number(itemForm.itemValue), set: (value) => { itemForm.itemValue = value == null ? '' : String(value) } })
const booleanEditorValue = computed({ get: () => itemForm.itemValue === 'true', set: (value) => { itemForm.itemValue = value ? 'true' : 'false' } })
const detailNumericValue = computed({ get: () => detailForm.itemValue === '' ? undefined : Number(detailForm.itemValue), set: (value) => { detailForm.itemValue = value == null ? '' : String(value) } })
const detailBooleanValue = computed({ get: () => detailForm.itemValue === 'true', set: (value) => { detailForm.itemValue = value ? 'true' : 'false' } })

watch(keyword, () => { window.clearTimeout(loadItems.timer); loadItems.timer = window.setTimeout(loadItems, 220) })

onMounted(async () => {
  await loadNamespaces()
  await loadItems()
  window.addEventListener('keydown', handleShortcut)
  document.addEventListener('contextmenu', preventBrowserContextMenu, true)
})
onBeforeUnmount(() => {
  window.removeEventListener('keydown', handleShortcut)
  document.removeEventListener('contextmenu', preventBrowserContextMenu, true)
})

function preventBrowserContextMenu(event) {
  event.preventDefault()
}

async function loadNamespaces() {
  try {
    namespaces.value = await fetchNamespaces()
    if (!namespaces.value.some((space) => space.id === activeNamespaceId.value)) {
      activeNamespaceId.value = namespaces.value[0]?.id ?? null
    }
    persistActiveNamespace()
  } catch (error) { ElMessage.error(error.message || '加载空间失败') }
}

async function loadItems() {
  itemsLoading.value = true
  const currentId = selectedItem.value?.id
  try {
    items.value = activeNamespaceId.value ? await fetchItems(activeNamespaceId.value, keyword.value) : []
    const current = items.value.find((item) => item.id === currentId)
    if (current) await selectItem(current)
    else {
      Object.assign(detailForm, newItem())
      detailBaseline.value = ''
    }
    selectedItem.value = current
  } catch (error) {
    ElMessage.error(error.message || '加载配置项失败')
  } finally {
    itemsLoading.value = false
  }
}

async function reloadWorkspace(silent = false) {
  await loadNamespaces()
  await loadItems()
  if (!silent) ElMessage.success('空间与配置目录已刷新')
}

async function switchNamespace(namespaceId) {
  const nextId = Number(namespaceId)
  if (!nextId || nextId === activeNamespaceId.value) return
  activeNamespaceId.value = nextId
  persistActiveNamespace()
  selectedNode.value = undefined
  selectedItem.value = undefined
  treeRef.value?.clearSelection()
  await loadItems()
}

async function selectNamespaceFromManager(space) {
  await switchNamespace(space.id)
}

function toggleSidebarPanel(panel) {
  if (panel !== 'spaces') return
  spacePanelVisible.value = !spacePanelVisible.value
  if (sidebarWidth.value != null) sidebarWidth.value = Math.max(sidebarWidth.value, minimumSidebarWidth())
  persistSidebarPanels()
}

function persistSidebarPanels() {
  localStorage.setItem('config.space-panel', spacePanelVisible.value ? 'open' : 'collapsed')
}

function minimumSidebarWidth() {
  return spacePanelVisible.value ? 390 : 210
}

function persistActiveNamespace() {
  if (activeNamespaceId.value) localStorage.setItem('config.active-namespace-id', String(activeNamespaceId.value))
  else localStorage.removeItem('config.active-namespace-id')
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
    treeRef.value?.clearSelection()
    return
  }
  treeRef.value?.selectPath(path)
}

async function selectItem(row) {
  selectedItem.value = row
  tableRef.value?.setCurrentRow(row)
  Object.assign(detailForm, newItem(row))
  detailBaseline.value = detailSnapshot(detailForm)
  detailValueError.value = ''
  try {
    const detail = await fetchItemDetail(row.id)
    if (selectedItem.value?.id === row.id) {
      Object.assign(detailForm, newItem(detail))
      detailBaseline.value = detailSnapshot(detailForm)
    }
  } catch (error) {
    ElMessage.error(error.message || '加载配置详情失败')
  }
}

async function openItemCreate(directoryPath = '') {
  if (!activeNamespaceId.value) {
    ElMessage.warning('请先创建并选择空间')
    return
  }
  Object.assign(itemForm, newItem({ namespaceId: activeNamespaceId.value, itemKey: directoryPath ? `${directoryPath}:` : '' }))
  itemEditorMode.value = 'create'
  valueError.value = ''
  keyTouched.value = false
  itemEditorVisible.value = true
  await nextTick()
  itemFormRef.value?.clearValidate()
}

async function openItemEdit(row) {
  if (!row) return
  await selectItem(row)
  document.querySelector('.details-section')?.scrollIntoView({ behavior: 'smooth', block: 'nearest' })
}

async function openNamespaceCreate() {
  Object.assign(namespaceForm, newNamespace())
  namespaceEditorMode.value = 'create'
  namespaceDialogVisible.value = true
  await nextTick()
  namespaceFormRef.value?.clearValidate()
}

async function openNamespaceEdit(space) {
  if (!space) return
  Object.assign(namespaceForm, newNamespace(space))
  namespaceEditorMode.value = 'edit'
  namespaceDialogVisible.value = true
  await nextTick()
  namespaceFormRef.value?.clearValidate()
}

function resetNamespaceDraft() {
  Object.assign(namespaceForm, newNamespace())
}

async function submitNamespace() {
  try {
    await namespaceFormRef.value?.validate()
    namespaceSaving.value = true
    const created = namespaceEditorMode.value === 'create'
    const savedCode = namespaceForm.namespaceCode
    const savedEnv = namespaceForm.env
    await saveNamespace({ ...namespaceForm, directoryMode: 'KEY_PROJECTION' })
    namespaceDialogVisible.value = false
    await loadNamespaces()
    if (created) {
      const saved = namespaces.value.find((space) => space.namespaceCode === savedCode && space.env === savedEnv)
      if (saved) await switchNamespace(saved.id)
    }
    if (!created) ElMessage.success('空间已保存')
  } catch (error) {
    if (error?.message) ElMessage.error(error.message)
  } finally {
    namespaceSaving.value = false
  }
}

async function confirmNamespaceDelete(space) {
  if (!space) return
  try {
    await ElMessageBox.confirm(`确定删除空间“${space.displayName}”吗？空间内仍有配置或目录时无法删除。`, '删除空间', { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' })
    await deleteNamespace(space.id)
    if (space.id === activeNamespaceId.value) activeNamespaceId.value = null
    await loadNamespaces()
    selectedNode.value = undefined
    selectedItem.value = undefined
    await loadItems()
    ElMessage.success('空间已删除')
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') ElMessage.error(error.message || '删除空间失败')
  }
}

function isJsonContainer(value) {
  try {
    const parsed = JSON.parse(value)
    return parsed !== null && typeof parsed === 'object'
  } catch {
    return false
  }
}

function handleItemJsonValidation(valid) {
  if (itemForm.valueType === 'JSON') valueError.value = valid ? '' : 'JSON 配置值必须是合法的对象或数组。'
}

function handleDetailJsonValidation(valid) {
  if (detailForm.valueType === 'JSON') detailValueError.value = valid || detailForm.itemValue == null || detailForm.itemValue === '' ? '' : 'JSON 配置值必须是合法的对象或数组。'
}

function validateValue() {
  const checks = [itemForm.itemValue].filter((value) => value !== null && value !== undefined && value !== '')
  if (itemForm.valueType === 'INTEGER') valueError.value = checks.every((value) => /^-?(0|[1-9]\d*)$/.test(value)) ? '' : '当前值无法转换为整数。'
  else if (itemForm.valueType === 'BOOLEAN') valueError.value = checks.every((value) => value === 'true' || value === 'false') ? '' : '布尔值只能为 true 或 false。'
  else if (itemForm.valueType === 'JSON') valueError.value = isJsonContainer(itemForm.itemValue) ? '' : 'JSON 配置值必须是合法的对象或数组。'
  else valueError.value = ''
}

function validateDetailValue() {
  if (detailForm.itemValue == null || detailForm.itemValue === '') {
    detailValueError.value = ''
    return
  }
  const checks = [detailForm.itemValue].filter((value) => value !== null && value !== undefined && value !== '')
  if (detailForm.valueType === 'INTEGER') detailValueError.value = checks.every((value) => /^-?(0|[1-9]\d*)$/.test(value)) ? '' : '当前值无法转换为整数。'
  else if (detailForm.valueType === 'BOOLEAN') detailValueError.value = checks.every((value) => value === 'true' || value === 'false') ? '' : '布尔值只能为 true 或 false。'
  else if (detailForm.valueType === 'JSON') detailValueError.value = isJsonContainer(detailForm.itemValue) ? '' : 'JSON 配置值必须是合法的对象或数组。'
  else detailValueError.value = ''
}

function restrictDetailValue(value) {
  const rawValue = String(value ?? '')
  if (detailForm.valueType === 'INTEGER') {
    detailForm.itemValue = rawValue.replace(/[^\d-]/g, '').replace(/(?!^)-/g, '')
    return
  }
  if (detailForm.valueType === 'BOOLEAN') {
    const normalized = rawValue.toLowerCase()
    detailForm.itemValue = ['', 't', 'tr', 'tru', 'true', 'f', 'fa', 'fal', 'fals', 'false'].includes(normalized) ? normalized : ''
  }
}

async function submitDetail() {
  if (!detailForm.id || !detailChanged.value) return
  try {
    validateDetailValue()
    await detailFormRef.value?.validate()
    if (detailValueError.value) return
    detailSaving.value = true
    const savedDetail = newItem(detailForm)
    await saveItem(savedDetail)
    Object.assign(detailForm, savedDetail)
    selectedItem.value = { ...selectedItem.value, ...savedDetail }
    const index = items.value.findIndex((item) => item.id === savedDetail.id)
    if (index >= 0) items.value[index] = selectedItem.value
    detailBaseline.value = detailSnapshot(detailForm)
  } catch (error) {
    if (error?.message) ElMessage.error(error.message)
  } finally {
    detailSaving.value = false
  }
}

async function continueItemCreate() {
  try {
    keyTouched.value = true
    await itemFormRef.value?.validate()
    if (!keyPattern.test(itemForm.itemKey || '')) return
    itemSaving.value = true
    const draftItem = {
      ...newItem({
      namespaceId: itemForm.namespaceId,
      itemKey: itemForm.itemKey,
      valueType: itemForm.valueType,
      status: 'ENABLED',
      description: ''
      }),
      itemValue: null
    }
    await saveItem(draftItem)
    await reloadWorkspace(true)
    const createdItem = items.value.find((item) => item.namespaceId === draftItem.namespaceId && item.itemKey === draftItem.itemKey)
    if (createdItem) {
      await selectItem(createdItem)
      treeRef.value?.selectItemKey(createdItem.id)
    }
    itemEditorVisible.value = false
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
    if (detailForm.id === detail.id) Object.assign(detailForm, newItem(selectedItem.value))
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

async function copyKey(row = selectedItem.value) {
  if (!row) return
  try { await navigator.clipboard.writeText(row.itemKey); ElMessage.success('完整 Key 已复制') } catch { ElMessage.warning('浏览器未授予剪贴板权限') }
}

function itemForTreeNode(node) {
  return items.value.find((item) => item.id === node?.itemId)
}
function openTreeItemEdit(node) {
  const row = itemForTreeNode(node)
  if (row) void openItemEdit(row)
}
function copyTreeItemKey(node) {
  void copyKey(itemForTreeNode(node))
}
function setTreeItemStatus(node, status) {
  const row = itemForTreeNode(node)
  if (!row) return
  selectItem(row)
  void setStatus(status)
}
function deleteTreeItem(node) {
  const row = itemForTreeNode(node)
  if (row) void confirmItemDelete(row)
}

function handleShortcut(event) {
  if (event.ctrlKey && event.key.toLowerCase() === 'f') { event.preventDefault(); topSearchRef.value?.focus() }
  if (event.ctrlKey && event.key.toLowerCase() === 's' && selectedItem.value && !itemEditorVisible.value && !namespaceDialogVisible.value) { event.preventDefault(); void submitDetail() }
  if (event.key === 'Delete' && selectedItem.value && !itemEditorVisible.value && !namespaceDialogVisible.value) { event.preventDefault(); void confirmItemDelete(selectedItem.value) }
}
function namespaceLabel(namespace) { return namespace ? `${namespace.displayName} (${namespace.namespaceCode} / ${namespace.env})` : '' }
function onSidebarSizeChange(size) {
  const width = Math.round(Number(size) - 28)
  if (!Number.isFinite(width)) return
  sidebarWidth.value = Math.max(width, minimumSidebarWidth())
  localStorage.setItem('config.sidebar-width-v2', String(sidebarWidth.value))
}
function typeLabel(type) { return ({ STRING: '字符串', INTEGER: '整数', BOOLEAN: '布尔', JSON: 'JSON' })[type] || type }
function formatTime(value) { return value ? new Date(value).toLocaleString('sv-SE').replace('T', ' ') : '—' }
function newItem(source = {}) { return { id: source.id ?? null, namespaceId: source.namespaceId ?? null, directoryId: null, itemKey: source.itemKey ?? '', itemValue: source.itemValue ?? '', defaultValue: source.defaultValue ?? '', valueType: source.valueType ?? 'STRING', status: source.status ?? 'ENABLED', description: source.description ?? '' } }
function detailSnapshot(item) { return JSON.stringify({ itemValue: item.itemValue ?? '', status: item.status ?? 'ENABLED', description: item.description ?? '' }) }
function newNamespace(source = {}) { return { id: source.id ?? null, namespaceCode: source.namespaceCode ?? '', env: source.env ?? 'prod', displayName: source.displayName ?? '', description: source.description ?? '', directoryMode: 'KEY_PROJECTION' } }

</script>

<style scoped>
.config-manager { min-height: calc(100vh - 40px); height: calc(100vh - 40px); overflow: hidden; background: var(--shell-tool-surface); color: var(--shell-text-primary); border-top: 1px solid var(--shell-tool-divider); font-size: 12px; }.config-splitter { width: 100%; height: 100%; min-width: 0; min-height: 0; }.config-splitter :deep(.el-splitter-panel) { min-width: 0; min-height: 0; }.config-splitter :deep(.el-splitter-bar__dragger-horizontal:before) { width: 1px; background: var(--shell-tool-divider); }.config-splitter :deep(.el-splitter-bar__dragger-horizontal:hover:not(.is-disabled):before) { width: 2px; background: var(--shell-accent); }.config-splitter :deep(.el-splitter-bar) { background: var(--shell-tool-surface-muted); }
.resource-explorer { display: grid; height: 100%; min-width: 0; grid-template-columns: 28px minmax(0, 1fr); background: var(--shell-tool-surface-muted); }.tool-rail { display: flex; flex-direction: column; align-items: center; border-right: 1px solid var(--shell-tool-divider); background: var(--shell-tool-surface-muted); }.tool-rail button { display: grid; width: 26px; height: 56px; flex: 0 0 auto; place-items: center; padding: 11px 0; border: 0; border-bottom: 1px solid var(--shell-tool-divider); border-radius: 0; background: transparent; color: var(--shell-tool-subtle-text); cursor: pointer; font: 11px/1 var(--shell-font-sans, "Segoe UI", sans-serif); writing-mode: vertical-rl; }.tool-rail button:hover { background: var(--shell-tool-hover); color: var(--shell-text-primary); }.tool-rail button.active { background: var(--shell-tool-surface); color: var(--shell-accent); box-shadow: inset 2px 0 var(--shell-accent); }.sidebar-content, .side-panel-splitter { display: flex; width: 100%; min-width: 0; min-height: 0; }.side-panel-splitter :deep(.el-splitter-panel) { min-width: 0; min-height: 0; }.side-panel-splitter :deep(.el-splitter-bar__dragger-horizontal:before) { width: 1px; background: var(--shell-tool-divider); }.side-panel-splitter :deep(.el-splitter-bar__dragger-horizontal:hover:not(.is-disabled):before) { width: 2px; background: var(--shell-accent); }
.button-glyph { margin-right: 2px; font-size: 14px; }.config-workspace { display: grid; width: 100%; height: 100%; min-width: 0; min-height: 0; grid-template-rows: minmax(0, 1fr); }
.workspace-toolbar, .breadcrumb-bar, .list-section, .details-section { min-width: 0; }.workspace-toolbar { display: flex; align-items: center; justify-content: space-between; gap: 10px; padding: 6px 10px; border-bottom: 1px solid var(--shell-tool-divider); background: var(--shell-tool-surface); }.toolbar-actions, .search-actions { display: flex; align-items: center; gap: 4px; }.toolbar-actions { min-width: 0; overflow: auto; }.toolbar-actions :deep(.el-button), .search-actions :deep(.el-button) { min-height: 26px; margin: 0; border-radius: 3px; font-size: 11px; white-space: nowrap; }.toolbar-divider { width: 1px; height: 17px; margin: 0 2px; background: var(--shell-tool-divider); }.search-actions { flex: 0 0 auto; }.search-actions :deep(.el-input) { width: 200px; }.search-actions :deep(.el-button) { width: 26px; padding: 0; font-size: 13px; }
.breadcrumb-bar { display: flex; align-items: center; gap: 6px; padding: 0 12px; border-bottom: 1px solid var(--shell-tool-divider); background: var(--shell-tool-toolbar-bg); color: var(--shell-tool-subtle-text); font-size: 11px; }.breadcrumb-bar button { padding: 2px 3px; border: 0; border-radius: 3px; background: transparent; color: var(--shell-text-secondary); cursor: pointer; }.breadcrumb-bar button:hover, .breadcrumb-bar button.active { background: var(--shell-tool-hover); color: var(--shell-accent); }.breadcrumb-separator { color: var(--shell-text-muted); font-size: 14px; }
.list-section { display: flex; min-height: 0; flex-direction: column; padding: 0 12px; border-bottom: 1px solid var(--shell-tool-divider); }.items-table { flex: 1; --el-table-border-color: var(--shell-tool-divider); --el-table-header-bg-color: var(--shell-tool-toolbar-bg); --el-table-tr-bg-color: transparent; --el-table-row-hover-bg-color: var(--shell-tool-hover); --el-table-current-row-bg-color: var(--shell-tool-selected-bg); font-size: 11px; }.items-table :deep(th.el-table__cell) { height: 34px; padding: 0; color: var(--shell-tool-subtle-text); font-size: 11px; font-weight: 600; }.items-table :deep(.el-table__cell) { height: 33px; padding: 0; }.items-table :deep(.code-cell .cell) { font-family: Consolas, "JetBrains Mono", monospace; }.items-table :deep(.el-table__inner-wrapper::before) { height: 0; }.item-key { color: var(--shell-text-primary); }.value-preview, .description-cell { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }.value-preview { font-family: Consolas, "JetBrains Mono", monospace; }.description-cell { color: var(--shell-text-secondary); }.type-tag, .status-tag { display: inline-flex; align-items: center; min-height: 19px; padding: 1px 5px; border-radius: 3px; font-size: 10px; font-weight: 600; line-height: 1.35; }.type-string { background: color-mix(in srgb, #8a6fd4 22%, transparent); color: #9f8be8; }.type-integer { background: color-mix(in srgb, #4b92dc 22%, transparent); color: #6ba9e9; }.type-boolean { background: color-mix(in srgb, #76a64f 22%, transparent); color: #92c66b; }.status-tag { gap: 4px; background: color-mix(in srgb, var(--shell-accent) 15%, transparent); color: var(--shell-accent); }.status-tag i { width: 5px; height: 5px; border-radius: 50%; background: currentColor; }.status-tag.disabled { background: color-mix(in srgb, #a0a7b1 16%, transparent); color: var(--shell-text-muted); }.table-footer { display: flex; align-items: center; justify-content: space-between; height: 38px; color: var(--shell-text-secondary); font-size: 11px; }.table-footer div { display: flex; align-items: center; gap: 6px; }.table-footer button { width: 22px; height: 22px; border: 1px solid var(--shell-tool-border-strong); border-radius: 3px; background: var(--shell-tool-surface); color: var(--shell-text-secondary); cursor: pointer; }.table-footer button.current-page { border-color: var(--shell-accent); color: var(--shell-accent); }
.details-section { display: flex; min-height: 0; flex-direction: column; padding: 0 20px 20px; overflow: auto; background: var(--shell-tool-surface-muted); }.details-title { display: flex; align-items: center; gap: 7px; height: 48px; flex: 0 0 auto; border-bottom: 1px solid var(--shell-tool-divider); color: var(--shell-tool-header-text); font-size: 13px; font-weight: 650; }.details-title :deep(.detail-delete-button) { margin-left: auto; }.accent-line { width: 2px; height: 16px; background: var(--shell-accent); }.details-empty { color: var(--shell-text-muted); font-size: 11px; font-weight: 400; }.detail-editor { display: grid; width: 100%; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 14px 18px; padding: 20px 0; }.detail-editor :deep(.el-form-item) { min-width: 0; margin-bottom: 0; }.detail-editor :deep(.el-form-item.wide), .detail-editor :deep(.detail-value-error), .value-editor-block { grid-column: 1 / -1; }.detail-editor :deep(.el-form-item__label) { padding-bottom: 5px; color: var(--shell-text-secondary); font-size: 11px; line-height: 1.3; }.detail-editor :deep(.el-select), .detail-editor :deep(.el-input-number) { width: 100%; }.detail-editor :deep(.el-radio) { margin-right: 14px; }.detail-editor :deep(.el-input__wrapper), .detail-editor :deep(.el-textarea__inner), .detail-editor :deep(.el-select__wrapper) { border-radius: 3px; }.value-editor-block { min-width: 0; min-height: 360px; padding: 14px; border: 1px solid var(--shell-tool-divider); border-radius: 5px; background: var(--shell-tool-surface); }.value-editor-heading { display: flex; align-items: baseline; justify-content: space-between; gap: 12px; margin-bottom: 10px; color: var(--shell-text-primary); font-size: 12px; font-weight: 650; }.value-editor-heading small { color: var(--shell-text-muted); font-size: 10px; font-weight: 400; }.value-editor-block :deep(.el-form-item) { margin: 0; }.value-editor-block :deep(.el-input-number) { width: min(420px, 100%); }.value-editor-block :deep(.el-textarea__inner) { min-height: 300px; padding: 11px 12px; font-family: Consolas, "JetBrains Mono", monospace; font-size: 12px; line-height: 1.6; }.copy-key-button { padding: 0; border: 0; background: transparent; color: var(--shell-text-secondary); cursor: pointer; font-size: 14px; }.copy-key-button:hover { color: var(--shell-accent); }.detail-value-error { margin-top: 1px; }.key-preview { display: flex; flex-wrap: wrap; gap: 12px; margin-top: 6px; color: var(--shell-tool-subtle-text); font-size: 10px; }
.detail-editor { grid-template-columns: minmax(150px, 3fr) minmax(0, 7fr); }.draft-indicator, .modified-indicator { width: 7px; height: 7px; flex: 0 0 auto; border-radius: 50%; }.draft-indicator { background: #e6a23c; box-shadow: 0 0 0 3px color-mix(in srgb, #e6a23c 17%, transparent); }.modified-indicator { background: var(--shell-accent); box-shadow: 0 0 0 3px color-mix(in srgb, var(--shell-accent) 17%, transparent); }.detail-space-inline { margin-left: 7px; overflow: hidden; color: var(--shell-text-secondary); font-size: 11px; font-weight: 400; text-overflow: ellipsis; white-space: nowrap; }.key-preview { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 5px; color: var(--shell-tool-subtle-text); font-size: 10px; }.item-editor-form { display: grid; grid-template-columns: minmax(0, 1fr); gap: 9px; }.item-editor-form :deep(.el-form-item) { min-width: 0; margin: 0; }.item-editor-form :deep(.wide) { grid-column: auto; }.item-editor-form :deep(.space-field) { width: min(275px, 100%); }.item-editor-form :deep(.el-form-item__label) { padding-bottom: 3px; font-size: 10px; line-height: 1.2; }.item-editor-form :deep(.el-select), .item-editor-form :deep(.el-input-number) { width: 100%; }.item-editor-form :deep(.el-input__wrapper), .item-editor-form :deep(.el-textarea__inner), .item-editor-form :deep(.el-select__wrapper) { border-radius: 4px; font-size: 11px; }.item-editor-form :deep(.el-input__wrapper), .item-editor-form :deep(.el-select__wrapper) { min-height: 28px; }.item-editor-form :deep(.space-field .el-input__inner) { color: var(--shell-text-secondary); font-size: 10px; }.item-editor-form :deep(.item-value-field .el-textarea__inner) { min-height: 104px; font-family: Consolas, "JetBrains Mono", monospace; font-size: 11px; line-height: 1.5; }.value-error { margin: -3px 0 0; }.drawer-footer { display: flex; justify-content: flex-end; gap: 8px; }.el-input-number { width: 100%; }.config-editor-dialog :deep(.el-dialog__body) { max-height: min(620px, calc(100vh - 210px)); padding-top: 8px; overflow-y: auto; }.item-editor-dialog :deep(.el-dialog__header) { display: none; }.item-editor-dialog :deep(.el-dialog__body) { padding: 16px 18px 12px; }
.value-editor-block :deep(.el-form-item__content), .item-value-field :deep(.el-form-item__content) { display: block; width: 100%; min-width: 0; }.value-editor-block :deep(.json-editor), .item-value-field :deep(.json-editor) { width: 100%; }.item-editor-form { grid-template-columns: minmax(0, 1fr); align-items: center; gap: 8px; }.item-editor-form :deep(.el-form-item__label) { align-self: start; padding-top: 7px; text-align: right; }.create-key-hint { margin-top: 4px; color: var(--shell-text-muted); font-size: 10px; line-height: 1.25; }.create-key-hint.invalid { color: var(--el-color-danger, #d14d4d); }
@media (max-width: 1120px) { .detail-editor { grid-template-columns: repeat(2, minmax(0, 1fr)); } }
@media (max-width: 860px) { .config-manager { height: auto; min-height: calc(100vh - 40px); overflow: visible; }.config-splitter { height: auto; min-height: calc(100vh - 40px); }.config-splitter :deep(.el-splitter-panel:first-child), .config-splitter :deep(.el-splitter-bar) { display: none; }.resource-explorer { display: none; }.details-section { min-height: calc(100vh - 40px); padding: 0 12px 16px; }.detail-editor, .item-editor-form { grid-template-columns: 1fr; }.detail-editor :deep(.el-form-item.wide), .detail-editor :deep(.detail-value-error), .item-editor-form :deep(.wide) { grid-column: auto; } }
</style>
