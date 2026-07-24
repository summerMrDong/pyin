<template>
  <section class="console-page">
    <el-card shadow="never" class="workspace-card">
      <div class="dict-workspace">
        <aside class="type-panel">
          <div class="panel-header">
            <div class="panel-title">
              <span>字典类型</span>
              <el-tag size="small" effect="plain">{{ filteredTypes.length }}</el-tag>
            </div>
            <el-button type="success" size="small" @click="openTypeCreate">新建类型</el-button>
          </div>

          <div class="type-filters">
            <el-input v-model="typeKeyword" placeholder="搜索类型编码或名称" clearable />
            <el-select v-model="typeStatus" placeholder="全部状态" clearable>
              <el-option label="启用" value="ENABLED" />
              <el-option label="停用" value="DISABLED" />
            </el-select>
          </div>

          <div v-loading="typesLoading" class="type-list">
            <el-empty v-if="!filteredTypes.length && !typesLoading" description="暂无字典类型" :image-size="88">
              <el-button type="primary" size="small" @click="openTypeCreate">新建字典类型</el-button>
            </el-empty>
            <div
              v-for="type in filteredTypes"
              :key="type.id"
              class="type-row"
              :class="{ active: type.id === selectedTypeId }"
              role="button"
              tabindex="0"
              @click="selectType(type.id)"
              @keydown.enter.prevent="selectType(type.id)"
              @keydown.space.prevent="selectType(type.id)"
            >
              <div class="type-row-main">
                <span class="type-name">{{ type.typeName }}</span>
                <el-tag :type="type.status === 'ENABLED' ? 'success' : 'info'" effect="plain" size="small">
                  {{ type.status === 'ENABLED' ? '启用' : '停用' }}
                </el-tag>
              </div>
              <div class="type-code">{{ type.typeCode }}</div>
              <div class="type-row-footer">
                <span>{{ type.itemCount }} 个字典项</span>
                <span class="type-row-actions">
                  <el-button link type="primary" @click.stop="openTypeEdit(type)">编辑</el-button>
                  <el-tooltip :disabled="!type.itemCount" content="请先删除该类型下的全部字典项" placement="top">
                    <span>
                      <el-button link type="danger" :disabled="Boolean(type.itemCount)" @click.stop="confirmTypeDelete(type)">删除</el-button>
                    </span>
                  </el-tooltip>
                </span>
              </div>
            </div>
          </div>
        </aside>

        <main v-if="selectedType" class="items-panel">
          <div class="items-panel-header">
            <div class="selected-type-summary">
              <div class="selected-type-title">
                <h2>{{ selectedType.typeName }}</h2>
                <el-tag :type="selectedType.status === 'ENABLED' ? 'success' : 'info'" effect="plain" size="small">
                  {{ selectedType.status === 'ENABLED' ? '启用' : '停用' }}
                </el-tag>
              </div>
              <div class="selected-type-meta">
                <code>{{ selectedType.typeCode }}</code>
                <span>{{ selectedType.itemCount }} 个字典项</span>
                <span v-if="selectedType.description">{{ selectedType.description }}</span>
              </div>
            </div>
            <div class="header-actions">
              <el-button @click="openTypeEdit(selectedType)">编辑类型</el-button>
              <el-button :loading="itemsLoading" @click="loadItems">刷新</el-button>
              <el-button type="success" @click="openItemCreate">新建字典项</el-button>
            </div>
          </div>

          <div class="items-toolbar">
            <el-input v-model="itemKeyword" placeholder="搜索字典值或标签" clearable />
            <span>当前显示 {{ filteredItems.length }} 项</span>
          </div>

          <el-table :data="filteredItems" v-loading="itemsLoading" class="items-table">
            <el-table-column prop="itemValue" label="字典值" min-width="150" class-name="code-cell" />
            <el-table-column prop="itemLabel" label="字典标签" min-width="160" />
            <el-table-column prop="itemSort" label="排序" width="90" />
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="row.itemStatus === 'ENABLED' ? 'success' : 'info'" effect="plain">
                  {{ row.itemStatus === 'ENABLED' ? '启用' : '停用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="描述" min-width="220">
              <template #default="{ row }"><span>{{ row.description || '-' }}</span></template>
            </el-table-column>
            <el-table-column label="操作" width="150" fixed="right">
              <template #default="{ row }">
                <div class="row-actions">
                  <el-button link type="primary" @click="openItemEdit(row)">编辑</el-button>
                  <el-button link type="danger" @click="confirmItemDelete(row)">删除</el-button>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </main>

        <main v-else class="items-panel empty-panel">
          <el-empty description="请选择或新建一个字典类型">
            <el-button type="primary" @click="openTypeCreate">新建字典类型</el-button>
          </el-empty>
        </main>
      </div>
    </el-card>

    <el-drawer v-model="typeEditorVisible" :title="typeEditorMode === 'create' ? '新建字典类型' : '编辑字典类型'" size="480px" destroy-on-close>
      <el-form ref="typeFormRef" :model="typeForm" :rules="typeRules" label-position="top">
        <el-form-item label="类型编码" prop="typeCode"><el-input v-model.trim="typeForm.typeCode" placeholder="例如 order_status" /></el-form-item>
        <el-form-item label="类型名称" prop="typeName"><el-input v-model.trim="typeForm.typeName" placeholder="例如 订单状态" /></el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="typeForm.status"><el-radio-button label="ENABLED">启用</el-radio-button><el-radio-button label="DISABLED">停用</el-radio-button></el-radio-group>
        </el-form-item>
        <el-form-item label="描述"><el-input v-model.trim="typeForm.description" type="textarea" :rows="3" placeholder="说明该字典的用途" /></el-form-item>
      </el-form>
      <template #footer><div class="drawer-footer"><el-button @click="typeEditorVisible = false">取消</el-button><el-button type="primary" :loading="typeSaving" @click="submitType">保存</el-button></div></template>
    </el-drawer>

    <el-drawer v-model="itemEditorVisible" :title="itemEditorMode === 'create' ? '新建字典项' : '编辑字典项'" size="480px" destroy-on-close>
      <el-form ref="itemFormRef" :model="itemForm" :rules="itemRules" label-position="top">
        <el-form-item label="所属类型"><el-input :model-value="itemTypeLabel" disabled /></el-form-item>
        <el-form-item label="字典值" prop="itemValue"><el-input v-model.trim="itemForm.itemValue" placeholder="例如 PAID" /></el-form-item>
        <el-form-item label="字典标签" prop="itemLabel"><el-input v-model.trim="itemForm.itemLabel" placeholder="例如 已支付" /></el-form-item>
        <div class="form-split">
          <el-form-item label="排序"><el-input-number v-model="itemForm.itemSort" :min="0" controls-position="right" /></el-form-item>
          <el-form-item label="状态" prop="itemStatus"><el-radio-group v-model="itemForm.itemStatus"><el-radio-button label="ENABLED">启用</el-radio-button><el-radio-button label="DISABLED">停用</el-radio-button></el-radio-group></el-form-item>
        </div>
        <el-form-item label="描述"><el-input v-model.trim="itemForm.description" type="textarea" :rows="3" placeholder="描述该字典项的业务含义" /></el-form-item>
      </el-form>
      <template #footer><div class="drawer-footer"><el-button @click="itemEditorVisible = false">取消</el-button><el-button type="primary" :loading="itemSaving" @click="submitItem">保存</el-button></div></template>
    </el-drawer>
  </section>
</template>

<script setup>
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { deleteItem, deleteType, fetchItemDetail, fetchItems, fetchTypes, saveItem, saveType } from '../api/dictAdminApi'

const types = ref([])
const items = ref([])
const selectedTypeId = ref()
const typesLoading = ref(false)
const itemsLoading = ref(false)
const typeSaving = ref(false)
const itemSaving = ref(false)
const typeEditorVisible = ref(false)
const itemEditorVisible = ref(false)
const typeEditorMode = ref('create')
const itemEditorMode = ref('create')
const typeFormRef = ref()
const itemFormRef = ref()
const typeKeyword = ref('')
const typeStatus = ref('')
const itemKeyword = ref('')
const typeForm = reactive(createTypeForm())
const itemForm = reactive(createItemForm())

const typeRules = {
  typeCode: [{ required: true, message: '请输入类型编码', trigger: 'blur' }],
  typeName: [{ required: true, message: '请输入类型名称', trigger: 'blur' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
}
const itemRules = {
  itemValue: [{ required: true, message: '请输入字典值', trigger: 'blur' }],
  itemLabel: [{ required: true, message: '请输入字典标签', trigger: 'blur' }],
  itemStatus: [{ required: true, message: '请选择状态', trigger: 'change' }]
}

const filteredTypes = computed(() => {
  const keyword = typeKeyword.value.trim().toLowerCase()
  return types.value.filter((type) =>
    (!keyword || type.typeCode.toLowerCase().includes(keyword) || type.typeName.toLowerCase().includes(keyword))
    && (!typeStatus.value || type.status === typeStatus.value)
  )
})
const selectedType = computed(() => types.value.find((type) => type.id === selectedTypeId.value))
const itemTypeLabel = computed(() => selectedType.value ? `${selectedType.value.typeName} (${selectedType.value.typeCode})` : '')
const filteredItems = computed(() => {
  const keyword = itemKeyword.value.trim().toLowerCase()
  return items.value.filter((item) => !keyword || item.itemValue.toLowerCase().includes(keyword) || item.itemLabel.toLowerCase().includes(keyword))
})

watch([typeKeyword, typeStatus], () => { void synchronizeSelectedType() })

onMounted(async () => { await loadTypes() })

async function loadTypes(preferred = {}) {
  typesLoading.value = true
  try {
    types.value = await fetchTypes()
    const preferredType = types.value.find((type) => type.id === preferred.id || type.typeCode === preferred.code)
    if (preferredType) selectedTypeId.value = preferredType.id
    await synchronizeSelectedType()
  } catch (error) {
    ElMessage.error(error.message || '加载字典类型失败')
  } finally {
    typesLoading.value = false
  }
}

async function synchronizeSelectedType() {
  const visibleSelectedType = filteredTypes.value.find((type) => type.id === selectedTypeId.value)
  if (visibleSelectedType) return
  const fallbackType = filteredTypes.value[0]
  if (fallbackType) {
    await selectType(fallbackType.id)
  } else {
    selectedTypeId.value = undefined
    items.value = []
  }
}

async function selectType(typeId) {
  if (selectedTypeId.value === typeId && items.value.length) return
  selectedTypeId.value = typeId
  await loadItems()
}

async function loadItems() {
  if (!selectedTypeId.value) {
    items.value = []
    return
  }
  itemsLoading.value = true
  try {
    items.value = await fetchItems(selectedTypeId.value)
  } catch (error) {
    ElMessage.error(error.message || '加载字典项失败')
  } finally {
    itemsLoading.value = false
  }
}

async function openTypeCreate() {
  typeEditorMode.value = 'create'
  Object.assign(typeForm, createTypeForm())
  typeEditorVisible.value = true
  await nextTick()
  typeFormRef.value?.clearValidate()
}

async function openTypeEdit(row) {
  typeEditorMode.value = 'edit'
  Object.assign(typeForm, createTypeForm(row))
  typeEditorVisible.value = true
  await nextTick()
  typeFormRef.value?.clearValidate()
}

async function openItemCreate() {
  if (!selectedType.value) {
    ElMessage.warning('请先选择字典类型')
    return
  }
  itemEditorMode.value = 'create'
  Object.assign(itemForm, createItemForm({ typeId: selectedType.value.id }))
  itemEditorVisible.value = true
  await nextTick()
  itemFormRef.value?.clearValidate()
}

async function openItemEdit(row) {
  try {
    const detail = await fetchItemDetail(row.id)
    itemEditorMode.value = 'edit'
    Object.assign(itemForm, createItemForm(detail))
    itemEditorVisible.value = true
    await nextTick()
    itemFormRef.value?.clearValidate()
  } catch (error) {
    ElMessage.error(error.message || '加载字典项详情失败')
  }
}

async function submitType() {
  try {
    await typeFormRef.value?.validate()
    typeSaving.value = true
    const savedCode = typeForm.typeCode
    const created = typeEditorMode.value === 'create'
    await saveType({ ...typeForm })
    typeEditorVisible.value = false
    ElMessage.success(created ? '字典类型已创建' : '字典类型已更新')
    if (created) {
      typeKeyword.value = ''
      typeStatus.value = ''
    }
    await loadTypes({ code: savedCode })
  } catch (error) {
    if (error?.message) ElMessage.error(error.message)
  } finally {
    typeSaving.value = false
  }
}

async function submitItem() {
  try {
    await itemFormRef.value?.validate()
    itemSaving.value = true
    const created = itemEditorMode.value === 'create'
    await saveItem({ ...itemForm })
    itemEditorVisible.value = false
    ElMessage.success(created ? '字典项已创建' : '字典项已更新')
    await Promise.all([loadTypes(), loadItems()])
  } catch (error) {
    if (error?.message) ElMessage.error(error.message)
  } finally {
    itemSaving.value = false
  }
}

async function confirmTypeDelete(row) {
  if (row.itemCount) return
  try {
    await ElMessageBox.confirm(`确定删除字典类型“${row.typeName}”吗？`, '删除字典类型', { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' })
    const index = filteredTypes.value.findIndex((type) => type.id === row.id)
    const fallback = filteredTypes.value[index + 1] || filteredTypes.value[index - 1]
    await deleteType(row.id)
    if (selectedTypeId.value === row.id) selectedTypeId.value = undefined
    ElMessage.success('字典类型已删除')
    await loadTypes({ id: fallback?.id })
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') ElMessage.error(error.message || '删除字典类型失败')
  }
}

async function confirmItemDelete(row) {
  try {
    await ElMessageBox.confirm(`确定删除字典项“${row.itemLabel}”吗？`, '删除字典项', { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' })
    await deleteItem(row.id)
    ElMessage.success('字典项已删除')
    await Promise.all([loadTypes(), loadItems()])
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') ElMessage.error(error.message || '删除字典项失败')
  }
}

function createTypeForm(source = {}) {
  return { id: source.id ?? null, typeCode: source.typeCode ?? '', typeName: source.typeName ?? '', status: source.status ?? 'ENABLED', description: source.description ?? '' }
}

function createItemForm(source = {}) {
  return { id: source.id ?? null, typeId: source.typeId ?? '', itemValue: source.itemValue ?? '', itemLabel: source.itemLabel ?? '', itemSort: source.itemSort ?? 100, itemStatus: source.itemStatus ?? 'ENABLED', description: source.description ?? '' }
}
</script>

<style scoped>
.console-page { min-width: 0; }
.workspace-card { border: 1px solid var(--shell-tool-border-strong); border-radius: 8px; background: var(--shell-tool-surface); box-shadow: none; }
.workspace-card :deep(.el-card__body) { padding: 12px; background: var(--shell-tool-surface); }
.dict-workspace { display: grid; grid-template-columns: 300px minmax(0, 1fr); min-height: max(560px, calc(100vh - 112px)); }
.type-panel { display: flex; min-width: 0; flex-direction: column; padding-right: 12px; border-right: 1px solid var(--shell-tool-divider); }
.panel-header, .panel-title, .type-row-main, .type-row-footer, .items-panel-header, .selected-type-title, .selected-type-meta, .header-actions, .row-actions, .drawer-footer { display: flex; align-items: center; }
.panel-header, .items-panel-header { justify-content: space-between; gap: 12px; }
.panel-header { min-height: 36px; }
.panel-title { gap: 8px; color: var(--shell-tool-header-text); font-size: 14px; font-weight: 600; }
.panel-title :deep(.el-tag) { --el-tag-bg-color: var(--shell-tool-tag-bg); --el-tag-border-color: var(--shell-tool-tag-border); --el-tag-text-color: var(--shell-tool-tag-text); }
.type-filters { display: grid; grid-template-columns: minmax(0, 1fr) 92px; gap: 8px; margin: 12px 0 8px; }
.type-filters :deep(.el-input__wrapper), .type-filters :deep(.el-select__wrapper), .items-toolbar :deep(.el-input__wrapper) { background: var(--shell-tool-toolbar-bg); box-shadow: 0 0 0 1px var(--shell-tool-border-strong) inset; border-radius: 6px; }
.type-list { min-height: 0; flex: 1; overflow: auto; padding: 2px 2px 2px 0; }
.type-row { margin-bottom: 6px; padding: 10px; border: 1px solid transparent; border-radius: 7px; color: var(--shell-text-primary); cursor: pointer; outline: none; }
.type-row:hover, .type-row:focus-visible { background: var(--shell-tool-hover); }
.type-row.active { border-color: var(--shell-tool-selected-border); background: var(--shell-tool-selected-bg); }
.type-row-main { justify-content: space-between; gap: 8px; }
.type-name { min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-size: 13px; font-weight: 600; }
.type-row :deep(.el-tag), .items-table :deep(.el-tag), .selected-type-title :deep(.el-tag) { --el-tag-bg-color: var(--shell-tool-tag-bg); --el-tag-border-color: var(--shell-tool-tag-border); --el-tag-text-color: var(--shell-tool-tag-text); border-radius: 4px; font-size: 11px; }
.type-code, .selected-type-meta code, .code-cell :deep(.cell) { font-family: Consolas, "JetBrains Mono", monospace; font-size: 11.5px; }
.type-code { margin-top: 4px; color: var(--shell-tool-subtle-text); }
.type-row-footer { justify-content: space-between; gap: 8px; margin-top: 8px; color: var(--shell-text-muted); font-size: 11px; }
.type-row-actions, .row-actions, .drawer-footer, .header-actions { display: flex; gap: 6px; align-items: center; }
.type-row-actions :deep(.el-button), .row-actions :deep(.el-button) { padding: 0; font-size: 12px; }
.items-panel { min-width: 0; padding-left: 16px; }
.items-panel-header { min-height: 60px; padding-bottom: 12px; border-bottom: 1px solid var(--shell-tool-divider); }
.selected-type-summary { min-width: 0; }
.selected-type-title { gap: 8px; }
.selected-type-title h2 { margin: 0; color: var(--shell-tool-header-text); font-size: 18px; font-weight: 600; }
.selected-type-meta { flex-wrap: wrap; gap: 8px; margin-top: 6px; color: var(--shell-text-secondary); font-size: 12px; }
.selected-type-meta code { color: var(--shell-tool-subtle-text); }
.header-actions { flex-shrink: 0; justify-content: flex-end; }
.header-actions :deep(.el-button), .panel-header :deep(.el-button) { min-height: 30px; border-radius: 6px; font-size: 12px; }
.items-toolbar { display: flex; align-items: center; justify-content: space-between; gap: 12px; padding: 12px 0; }
.items-toolbar :deep(.el-input) { width: min(360px, 100%); }
.items-toolbar > span { flex-shrink: 0; color: var(--shell-text-muted); font-size: 12px; }
.items-table { --el-table-border-color: var(--shell-tool-divider); --el-table-header-bg-color: var(--shell-tool-toolbar-bg); --el-table-row-hover-bg-color: var(--shell-tool-hover); font-size: 12px; }
.items-table :deep(th.el-table__cell) { padding: 10px 0; color: var(--shell-tool-subtle-text); font-size: 12px; font-weight: 600; }
.items-table :deep(.el-table__cell) { padding: 10px 0; }
.items-table :deep(.el-table__inner-wrapper::before) { background-color: var(--shell-tool-divider); }
.empty-panel { display: grid; place-items: center; }
.form-split { display: grid; grid-template-columns: minmax(0, 1fr) minmax(0, 1.4fr); gap: 12px; }
@media (max-width: 960px) { .dict-workspace { grid-template-columns: 1fr; } .type-panel { max-height: 360px; padding: 0 0 12px; border-right: 0; border-bottom: 1px solid var(--shell-tool-divider); } .items-panel { padding: 16px 0 0; } }
@media (max-width: 720px) { .workspace-card :deep(.el-card__body) { padding: 10px; } .items-panel-header { align-items: flex-start; flex-direction: column; } .header-actions { flex-wrap: wrap; justify-content: flex-start; } .items-toolbar { align-items: stretch; flex-direction: column; } .items-toolbar :deep(.el-input) { width: 100%; } .form-split { grid-template-columns: 1fr; } }
</style>
