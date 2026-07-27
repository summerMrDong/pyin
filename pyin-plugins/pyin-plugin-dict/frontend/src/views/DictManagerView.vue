<template>
  <section class="dict-console">
    <aside class="resource-explorer" :style="{ width: `${sidebarWidth}px` }">
      <div class="explorer-heading">
        <span>字典资源管理器</span>
        <div class="heading-actions">
          <el-tooltip content="刷新目录"><el-button link @click="refreshWorkspace">↻</el-button></el-tooltip>
          <el-tooltip content="收起全部"><el-button link @click="collapseAll">«</el-button></el-tooltip>
          <el-dropdown><el-button link>···</el-button><template #dropdown><el-dropdown-menu><el-dropdown-item @click="openCategoryCreate">新建分类</el-dropdown-item><el-dropdown-item @click="openCategorySettings">目录设置</el-dropdown-item></el-dropdown-menu></template></el-dropdown>
        </div>
      </div>

      <el-input v-model="treeKeyword" class="explorer-search" placeholder="搜索字典目录" clearable>
        <template #suffix>⌕</template>
      </el-input>

      <div v-loading="categoriesLoading" class="directory-tree">
        <el-empty v-if="!visibleCategories.length && !categoriesLoading" :image-size="64" description="暂无字典分类" />
        <div v-for="category in visibleCategories" :key="category.id" class="directory-group">
          <button type="button" class="directory-category" :class="{ selected: categoryFilter === category.id }" @click="selectCategory(category)">
            <span class="disclosure">{{ expandedCategoryIds.includes(category.id) ? '⌄' : '›' }}</span>
            <span class="folder-icon">▰</span>
            <span class="directory-name">{{ category.categoryName }}</span>
            <span class="directory-count">{{ category.dictionaryCount }}</span>
          </button>
          <div v-show="expandedCategoryIds.includes(category.id)" class="directory-children">
            <button
              v-for="type in categoryTypes(category.id)"
              :key="type.id"
              type="button"
              class="directory-type"
              :class="{ active: type.id === selectedTypeId }"
              @click="selectType(type)"
            >
              <span class="dictionary-icon">▦</span>
              <span class="directory-name">{{ type.typeName }}</span>
              <span class="directory-count">{{ type.itemCount }}</span>
            </button>
          </div>
        </div>
      </div>

      <div class="explorer-footer">
        <el-button link @click="openCategoryCreate">＋ 新建分类</el-button>
        <i />
        <el-button link @click="openCategorySettings">⚙ 目录设置</el-button>
      </div>
    </aside>

    <div class="splitter vertical" @mousedown="startResize('sidebar', $event)" />

    <main class="main-workspace">
      <section class="dictionary-pane" :style="{ height: `${listHeight}px` }">
        <div class="compact-toolbar">
          <div class="toolbar-actions">
            <el-button type="primary" size="small" @click="openTypeCreate">＋ 新建字典</el-button>
            <el-button size="small" :disabled="!singleSelectedType" @click="openTypeEdit(singleSelectedType)">编辑</el-button>
            <el-button size="small" :disabled="!selectedTypes.length" @click="confirmTypeDelete">删除</el-button>
            <el-button size="small" :disabled="!selectedTypes.length" @click="changeTypeStatus('ENABLED')">启用</el-button>
            <el-button size="small" :disabled="!selectedTypes.length" @click="changeTypeStatus('DISABLED')">停用</el-button>
            <el-button size="small" @click="showUnavailable('导入')">导入</el-button>
            <el-button size="small" @click="showUnavailable('导出')">导出</el-button>
            <el-dropdown><el-button size="small">更多⌄</el-button><template #dropdown><el-dropdown-menu><el-dropdown-item @click="clearTypeSelection">取消选择</el-dropdown-item><el-dropdown-item @click="openCategorySettings">分类管理</el-dropdown-item></el-dropdown-menu></template></el-dropdown>
          </div>
          <div class="toolbar-search">
            <el-input v-model="typeKeyword" size="small" placeholder="搜索字典名称/编码/说明" clearable><template #suffix>⌕</template></el-input>
            <el-button size="small" title="过滤" @click="filterVisible = !filterVisible">⌑</el-button>
            <el-button size="small" title="刷新" @click="loadWorkspace">↻</el-button>
          </div>
        </div>
        <div v-show="filterVisible" class="filter-strip">
          <span>分类</span>
          <el-select v-model="categoryFilter" size="small" clearable placeholder="全部分类"><el-option v-for="category in categories" :key="category.id" :label="category.categoryName" :value="category.id" /></el-select>
          <span>状态</span>
          <el-select v-model="typeStatusFilter" size="small" clearable placeholder="全部状态"><el-option label="启用" value="ENABLED" /><el-option label="停用" value="DISABLED" /></el-select>
        </div>
        <el-table ref="typeTableRef" v-loading="typesLoading" :data="pagedTypes" class="dense-table" highlight-current-row @current-change="handleCurrentTypeChange" @selection-change="selectedTypes = $event">
          <el-table-column type="selection" width="42" />
          <el-table-column prop="typeCode" label="编码" min-width="145" class-name="code-cell" />
          <el-table-column prop="typeName" label="字典名称" min-width="130" />
          <el-table-column prop="categoryName" label="分类" min-width="112" />
          <el-table-column label="状态" width="94"><template #default="{ row }"><StatusBadge :enabled="row.status === 'ENABLED'" /></template></el-table-column>
          <el-table-column label="更新时间" min-width="160"><template #default="{ row }">{{ formatTime(row.updatedAt) }}</template></el-table-column>
          <el-table-column label="说明" min-width="220"><template #default="{ row }"><span class="ellipsis">{{ row.description || '—' }}</span></template></el-table-column>
        </el-table>
        <div class="table-footer">
          <span>共 {{ filteredTypes.length }} 条</span>
          <el-pagination v-model:current-page="typePage" :page-size="pageSize" size="small" layout="sizes, prev, pager, next" :page-sizes="[20, 50, 100]" :total="filteredTypes.length" @size-change="handlePageSizeChange" />
        </div>
      </section>

      <div class="splitter horizontal" @mousedown="startResize('list', $event)" />

      <section class="items-pane">
        <div class="items-title">
          字典项管理 <template v-if="selectedType">- {{ selectedType.typeName }} ({{ selectedType.typeCode }})</template><template v-else>- 未选择字典</template>
        </div>
        <div class="compact-toolbar item-toolbar">
          <div class="toolbar-actions">
            <el-button type="primary" size="small" :disabled="!selectedType" @click="openItemCreate">＋ 新增字典项</el-button>
            <el-button size="small" :disabled="!singleSelectedItem" @click="openItemEdit(singleSelectedItem)">编辑</el-button>
            <el-button size="small" :disabled="!selectedItems.length" @click="confirmItemDelete">删除</el-button>
            <el-button size="small" :disabled="!selectedItems.length" @click="changeItemStatus('ENABLED')">启用</el-button>
            <el-button size="small" :disabled="!selectedItems.length" @click="changeItemStatus('DISABLED')">停用</el-button>
            <el-button size="small" @click="showUnavailable('导入')">导入</el-button>
            <el-button size="small" @click="showUnavailable('导出')">导出</el-button>
            <el-dropdown><el-button size="small">批量操作⌄</el-button><template #dropdown><el-dropdown-menu><el-dropdown-item :disabled="!selectedItems.length" @click="changeItemStatus('ENABLED')">批量启用</el-dropdown-item><el-dropdown-item :disabled="!selectedItems.length" @click="changeItemStatus('DISABLED')">批量停用</el-dropdown-item></el-dropdown-menu></template></el-dropdown>
          </div>
          <div class="toolbar-search"><el-input v-model="itemKeyword" size="small" placeholder="搜索标签/值/备注" clearable><template #suffix>⌕</template></el-input><el-button size="small" title="刷新" :disabled="!selectedType" @click="loadItems">↻</el-button></div>
        </div>
        <el-table ref="itemTableRef" v-loading="itemsLoading" :data="filteredItems" class="dense-table item-table" highlight-current-row @selection-change="selectedItems = $event">
          <el-table-column type="selection" width="42" />
          <el-table-column prop="itemLabel" label="标签" min-width="180" />
          <el-table-column prop="itemValue" label="值" min-width="170" class-name="code-cell" />
          <el-table-column prop="itemSort" label="排序" width="100" />
          <el-table-column label="状态" width="94"><template #default="{ row }"><StatusBadge :enabled="row.itemStatus === 'ENABLED'" /></template></el-table-column>
          <el-table-column label="备注" min-width="240"><template #default="{ row }"><span class="ellipsis">{{ row.description || '—' }}</span></template></el-table-column>
        </el-table>
        <div class="table-footer"><span>共 {{ filteredItems.length }} 条</span><span>20 条/页</span></div>
      </section>
    </main>

    <el-drawer v-model="typeEditorVisible" :title="typeEditorMode === 'create' ? '新建字典' : '编辑字典'" size="460px" destroy-on-close>
      <el-form ref="typeFormRef" :model="typeForm" :rules="typeRules" label-position="top">
        <el-form-item label="字典名称" prop="typeName"><el-input v-model.trim="typeForm.typeName" placeholder="例如：行政区划" /></el-form-item>
        <el-form-item label="字典编码" prop="typeCode"><el-input v-model.trim="typeForm.typeCode" placeholder="例如：AREA_CODE" :disabled="typeEditorMode === 'edit'" /><p class="field-hint">建议使用大写字母、数字和下划线；编码全局唯一。</p></el-form-item>
        <el-form-item label="所属分类" prop="categoryId"><el-select v-model="typeForm.categoryId" class="full-width"><el-option v-for="category in categories" :key="category.id" :label="category.categoryName" :value="category.id" /></el-select></el-form-item>
        <el-form-item label="说明"><el-input v-model.trim="typeForm.description" type="textarea" :rows="3" placeholder="说明该字典的业务用途" /></el-form-item>
        <el-form-item label="状态"><el-radio-group v-model="typeForm.status"><el-radio-button label="ENABLED">启用</el-radio-button><el-radio-button label="DISABLED">停用</el-radio-button></el-radio-group></el-form-item>
      </el-form>
      <template #footer><div class="drawer-footer"><el-button @click="typeEditorVisible = false">取消</el-button><el-button type="primary" :loading="typeSaving" @click="submitType">保存</el-button></div></template>
    </el-drawer>

    <el-drawer v-model="itemEditorVisible" :title="itemEditorMode === 'create' ? '新增字典项' : '编辑字典项'" size="460px" destroy-on-close>
      <el-form ref="itemFormRef" :model="itemForm" :rules="itemRules" label-position="top">
        <el-form-item label="所属字典"><el-input :model-value="selectedType ? `${selectedType.typeName} (${selectedType.typeCode})` : ''" disabled /></el-form-item>
        <el-form-item label="标签" prop="itemLabel"><el-input v-model.trim="itemForm.itemLabel" placeholder="用户看到的文字，例如：启用" /></el-form-item>
        <el-form-item label="值" prop="itemValue"><el-input v-model.trim="itemForm.itemValue" placeholder="程序使用的值，例如：1" /></el-form-item>
        <div class="form-grid"><el-form-item label="排序"><el-input-number v-model="itemForm.itemSort" :min="0" controls-position="right" /></el-form-item><el-form-item label="状态"><el-radio-group v-model="itemForm.itemStatus"><el-radio-button label="ENABLED">启用</el-radio-button><el-radio-button label="DISABLED">停用</el-radio-button></el-radio-group></el-form-item></div>
        <el-form-item label="备注"><el-input v-model.trim="itemForm.description" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer><div class="drawer-footer"><el-button @click="itemEditorVisible = false">取消</el-button><el-button type="primary" :loading="itemSaving" @click="submitItem">保存</el-button></div></template>
    </el-drawer>

    <el-dialog v-model="categoryEditorVisible" :title="categoryForm.id ? '编辑字典分类' : '新建字典分类'" width="420px">
      <el-form ref="categoryFormRef" :model="categoryForm" :rules="categoryRules" label-position="top"><el-form-item label="分类名称" prop="categoryName"><el-input v-model.trim="categoryForm.categoryName" /></el-form-item><el-form-item label="排序"><el-input-number v-model="categoryForm.sortOrder" :min="0" controls-position="right" /></el-form-item></el-form>
      <template #footer><el-button @click="categoryEditorVisible = false">取消</el-button><el-button type="primary" :loading="categorySaving" @click="submitCategory">保存</el-button></template>
    </el-dialog>
    <el-dialog v-model="categorySettingsVisible" title="目录设置" width="560px"><div class="category-setting-toolbar"><el-button type="primary" size="small" @click="openCategoryCreate">＋ 新建分类</el-button></div><el-table :data="categories" class="dense-table"><el-table-column prop="categoryName" label="分类名称" /><el-table-column prop="dictionaryCount" label="字典数" width="100" /><el-table-column prop="sortOrder" label="排序" width="80" /><el-table-column label="操作" width="130"><template #default="{ row }"><el-button link type="primary" @click="openCategoryEdit(row)">编辑</el-button><el-button link type="danger" @click="confirmCategoryDelete(row)">删除</el-button></template></el-table-column></el-table></el-dialog>
  </section>
</template>

<script setup>
import { computed, defineComponent, h, nextTick, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { deleteCategory, deleteItem, deleteType, fetchCategories, fetchItemDetail, fetchItems, fetchTypes, saveCategory, saveItem, saveType } from '../api/dictAdminApi'

const StatusBadge = defineComponent({ props: { enabled: Boolean }, setup: (props) => () => h('span', { class: ['status-badge', { disabled: !props.enabled }] }, [h('i'), props.enabled ? '启用' : '停用']) })
const categories = ref([]), types = ref([]), items = ref([]), treeKeyword = ref(''), typeKeyword = ref(''), itemKeyword = ref(''), categoryFilter = ref(), typeStatusFilter = ref('')
const selectedTypeId = ref(), selectedTypes = ref([]), selectedItems = ref([]), expandedCategoryIds = ref([]), typesLoading = ref(false), itemsLoading = ref(false), categoriesLoading = ref(false), filterVisible = ref(false)
const sidebarWidth = ref(288), listHeight = ref(392), resizeMode = ref(), typePage = ref(1), pageSize = ref(20)
const typeTableRef = ref(), typeFormRef = ref(), itemFormRef = ref(), categoryFormRef = ref()
const typeEditorVisible = ref(false), itemEditorVisible = ref(false), categoryEditorVisible = ref(false), categorySettingsVisible = ref(false), typeEditorMode = ref('create'), itemEditorMode = ref('create'), typeSaving = ref(false), itemSaving = ref(false), categorySaving = ref(false)
const typeForm = reactive(newType()), itemForm = reactive(newItem()), categoryForm = reactive(newCategory())
const typeRules = { typeName: [{ required: true, message: '请输入字典名称', trigger: 'blur' }], typeCode: [{ required: true, validator: validateCode, trigger: 'blur' }], categoryId: [{ required: true, message: '请选择所属分类', trigger: 'change' }] }
const itemRules = { itemLabel: [{ required: true, message: '请输入标签', trigger: 'blur' }], itemValue: [{ required: true, message: '请输入值', trigger: 'blur' }] }
const categoryRules = { categoryName: [{ required: true, message: '请输入分类名称', trigger: 'blur' }] }
const visibleCategories = computed(() => { const keyword = treeKeyword.value.trim().toLowerCase(); return categories.value.filter(category => !keyword || category.categoryName.toLowerCase().includes(keyword) || categoryTypes(category.id).some(type => `${type.typeName} ${type.typeCode}`.toLowerCase().includes(keyword))) })
const filteredTypes = computed(() => { const keyword = typeKeyword.value.trim().toLowerCase(); return types.value.filter(type => (!categoryFilter.value || type.categoryId === categoryFilter.value) && (!typeStatusFilter.value || type.status === typeStatusFilter.value) && (!keyword || `${type.typeName} ${type.typeCode} ${type.description || ''}`.toLowerCase().includes(keyword))) })
const pagedTypes = computed(() => filteredTypes.value.slice((typePage.value - 1) * pageSize.value, typePage.value * pageSize.value))
const selectedType = computed(() => types.value.find(type => type.id === selectedTypeId.value))
const filteredItems = computed(() => { const keyword = itemKeyword.value.trim().toLowerCase(); return items.value.filter(item => !keyword || `${item.itemLabel} ${item.itemValue} ${item.description || ''}`.toLowerCase().includes(keyword)) })
const singleSelectedType = computed(() => selectedTypes.value.length === 1 ? selectedTypes.value[0] : selectedType.value)
const singleSelectedItem = computed(() => selectedItems.value.length === 1 ? selectedItems.value[0] : undefined)

watch([typeKeyword, categoryFilter, typeStatusFilter, pageSize], () => { typePage.value = 1 })
onMounted(loadWorkspace)

async function loadWorkspace() { categoriesLoading.value = typesLoading.value = true; try { const [categoryData, typeData] = await Promise.all([fetchCategories(), fetchTypes()]); categories.value = categoryData; types.value = typeData; expandedCategoryIds.value = categoryData.map(category => category.id); if (!selectedType.value && typeData.length) await selectType(typeData[0]); else if (selectedType.value) await loadItems() } catch (error) { ElMessage.error(error.message || '加载字典数据失败') } finally { categoriesLoading.value = typesLoading.value = false } }
async function refreshWorkspace() { await loadWorkspace(); ElMessage.success('字典工作区已刷新') }
function categoryTypes(categoryId) { return types.value.filter(type => type.categoryId === categoryId) }
async function selectCategory(category) { categoryFilter.value = category.id; expandedCategoryIds.value = expandedCategoryIds.value.includes(category.id) ? expandedCategoryIds.value.filter(item => item !== category.id) : [...expandedCategoryIds.value, category.id]; const firstType = categoryTypes(category.id)[0]; if (firstType && selectedType.value?.categoryId !== category.id) await selectType(firstType) }
function collapseAll() { expandedCategoryIds.value = [] }
async function selectType(type) { if (!type || selectedTypeId.value === type.id) return; selectedTypeId.value = type.id; selectedTypes.value = [type]; await nextTick(); typeTableRef.value?.setCurrentRow(type); await loadItems() }
async function handleCurrentTypeChange(row) { if (row && row.id !== selectedTypeId.value) await selectType(row) }
async function loadItems() { if (!selectedTypeId.value) { items.value = []; return }; itemsLoading.value = true; try { items.value = await fetchItems(selectedTypeId.value) } catch (error) { ElMessage.error(error.message || '加载字典项失败') } finally { itemsLoading.value = false } }
function clearTypeSelection() { selectedTypes.value = []; typeTableRef.value?.clearSelection() }
function handlePageSizeChange(size) { pageSize.value = size }
function startResize(mode, event) { resizeMode.value = mode; document.addEventListener('mousemove', resize); document.addEventListener('mouseup', stopResize); event.preventDefault() }
function resize(event) { if (resizeMode.value === 'sidebar') sidebarWidth.value = Math.min(380, Math.max(240, event.clientX)); if (resizeMode.value === 'list') { const top = document.querySelector('.dict-console')?.getBoundingClientRect().top || 0; listHeight.value = Math.min(Math.max(270, event.clientY - top), Math.max(320, window.innerHeight - top - 250)) } }
function stopResize() { resizeMode.value = undefined; document.removeEventListener('mousemove', resize); document.removeEventListener('mouseup', stopResize) }
function showUnavailable(action) { ElMessage.info(`${action}能力将在后续版本接入文件处理流程`) }
function validateCode(_rule, value, callback) { callback(/^[A-Z][A-Z0-9_]*$/.test(value || '') ? undefined : new Error('请输入大写字母、数字和下划线组成的编码')) }
async function openTypeCreate() { typeEditorMode.value = 'create'; Object.assign(typeForm, newType({ categoryId: categoryFilter.value || categories.value[0]?.id })); typeEditorVisible.value = true; await nextTick(); typeFormRef.value?.clearValidate() }
async function openTypeEdit(row) { if (!row) return; typeEditorMode.value = 'edit'; Object.assign(typeForm, newType(row)); typeEditorVisible.value = true; await nextTick(); typeFormRef.value?.clearValidate() }
async function submitType() { try { await typeFormRef.value?.validate(); typeSaving.value = true; await saveType({ ...typeForm }); const savedCode = typeForm.typeCode; typeEditorVisible.value = false; await loadWorkspace(); const saved = types.value.find(type => type.typeCode === savedCode); if (saved) await selectType(saved); ElMessage.success('字典已保存') } catch (error) { if (error?.message) ElMessage.error(error.message) } finally { typeSaving.value = false } }
async function confirmTypeDelete() { const rows = selectedTypes.value.length ? selectedTypes.value : (selectedType.value ? [selectedType.value] : []); if (!rows.length) return; try { await ElMessageBox.confirm(`确定删除选中的 ${rows.length} 个字典吗？未删除字典项的字典无法删除。`, '删除字典', { type: 'warning' }); await Promise.all(rows.map(row => deleteType(row.id))); selectedTypeId.value = undefined; items.value = []; clearTypeSelection(); await loadWorkspace(); ElMessage.success('字典已删除') } catch (error) { if (error !== 'cancel' && error !== 'close') ElMessage.error(error.message || '删除字典失败') } }
async function changeTypeStatus(status) { const rows = selectedTypes.value.length ? selectedTypes.value : (selectedType.value ? [selectedType.value] : []); if (!rows.length) return; try { await Promise.all(rows.map(row => saveType({ ...row, status }))); await loadWorkspace(); ElMessage.success(`已${status === 'ENABLED' ? '启用' : '停用'} ${rows.length} 个字典`) } catch (error) { ElMessage.error(error.message || '更新字典状态失败') } }
async function openItemCreate() { if (!selectedType.value) return; itemEditorMode.value = 'create'; Object.assign(itemForm, newItem({ typeId: selectedType.value.id })); itemEditorVisible.value = true; await nextTick(); itemFormRef.value?.clearValidate() }
async function openItemEdit(row) { try { itemEditorMode.value = 'edit'; Object.assign(itemForm, newItem(await fetchItemDetail(row.id))); itemEditorVisible.value = true; await nextTick(); itemFormRef.value?.clearValidate() } catch (error) { ElMessage.error(error.message || '加载字典项失败') } }
async function submitItem() { try { await itemFormRef.value?.validate(); itemSaving.value = true; await saveItem({ ...itemForm }); itemEditorVisible.value = false; await Promise.all([loadItems(), loadWorkspace()]); ElMessage.success('字典项已保存') } catch (error) { if (error?.message) ElMessage.error(error.message) } finally { itemSaving.value = false } }
async function confirmItemDelete() { if (!selectedItems.value.length) return; try { await ElMessageBox.confirm(`确定删除选中的 ${selectedItems.value.length} 个字典项吗？`, '删除字典项', { type: 'warning' }); await Promise.all(selectedItems.value.map(row => deleteItem(row.id))); selectedItems.value = []; await Promise.all([loadItems(), loadWorkspace()]); ElMessage.success('字典项已删除') } catch (error) { if (error !== 'cancel' && error !== 'close') ElMessage.error(error.message || '删除字典项失败') } }
async function changeItemStatus(itemStatus) { if (!selectedItems.value.length) return; try { await Promise.all(selectedItems.value.map(row => saveItem({ ...row, itemStatus }))); await loadItems(); ElMessage.success(`已${itemStatus === 'ENABLED' ? '启用' : '停用'} ${selectedItems.value.length} 个字典项`) } catch (error) { ElMessage.error(error.message || '更新字典项状态失败') } }
function openCategoryCreate() { Object.assign(categoryForm, newCategory()); categoryEditorVisible.value = true; nextTick(() => categoryFormRef.value?.clearValidate()) }
function openCategoryEdit(row) { Object.assign(categoryForm, newCategory(row)); categoryEditorVisible.value = true; nextTick(() => categoryFormRef.value?.clearValidate()) }
function openCategorySettings() { categorySettingsVisible.value = true }
async function submitCategory() { try { await categoryFormRef.value?.validate(); categorySaving.value = true; await saveCategory({ ...categoryForm }); categoryEditorVisible.value = false; await loadWorkspace(); ElMessage.success('分类已保存') } catch (error) { if (error?.message) ElMessage.error(error.message) } finally { categorySaving.value = false } }
async function confirmCategoryDelete(row) { try { await ElMessageBox.confirm(`确定删除分类“${row.categoryName}”吗？`, '删除分类', { type: 'warning' }); await deleteCategory(row.id); await loadWorkspace(); ElMessage.success('分类已删除') } catch (error) { if (error !== 'cancel' && error !== 'close') ElMessage.error(error.message || '删除分类失败') } }
function formatTime(value) { if (!value) return '—'; const date = new Date(value); return Number.isNaN(date.getTime()) ? value : date.toLocaleString('zh-CN', { hour12: false }).replaceAll('/', '-') }
function newType(source = {}) { return { id: source.id ?? null, categoryId: source.categoryId ?? null, typeCode: source.typeCode ?? '', typeName: source.typeName ?? '', status: source.status ?? 'ENABLED', description: source.description ?? '' } }
function newItem(source = {}) { return { id: source.id ?? null, typeId: source.typeId ?? selectedTypeId.value ?? null, itemValue: source.itemValue ?? '', itemLabel: source.itemLabel ?? '', itemSort: source.itemSort ?? 100, itemStatus: source.itemStatus ?? 'ENABLED', description: source.description ?? '' } }
function newCategory(source = {}) { return { id: source.id ?? null, categoryName: source.categoryName ?? '', sortOrder: source.sortOrder ?? 100 } }
</script>

<style scoped>
.dict-console { display: flex; min-height: calc(100vh - 40px); background: var(--shell-tool-surface); border-top: 1px solid var(--shell-tool-divider); color: var(--shell-text-primary); overflow: hidden; }
.resource-explorer { flex: 0 0 auto; display: flex; min-width: 240px; flex-direction: column; background: var(--shell-tool-surface-muted); }
.explorer-heading, .heading-actions, .compact-toolbar, .toolbar-actions, .toolbar-search, .explorer-footer, .table-footer, .form-grid { display: flex; align-items: center; }
.explorer-heading { min-height: 50px; padding: 0 14px 0 16px; justify-content: space-between; font-size: 14px; font-weight: 650; color: var(--shell-tool-header-text); }
.heading-actions { gap: 2px; }.heading-actions :deep(.el-button) { width: 24px; padding: 0; color: var(--shell-text-secondary); font-size: 16px; }
.explorer-search { width: auto; margin: 4px 12px 10px; }.explorer-search :deep(.el-input__wrapper), .toolbar-search :deep(.el-input__wrapper) { background: var(--shell-tool-toolbar-bg); box-shadow: 0 0 0 1px var(--shell-tool-border-strong) inset; border-radius: 4px; }
.directory-tree { min-height: 0; flex: 1; overflow: auto; padding: 3px 8px; }.directory-group { margin-bottom: 3px; }
.directory-category, .directory-type { display: flex; width: 100%; height: 31px; align-items: center; gap: 7px; padding: 0 7px; border: 0; border-radius: 4px; background: transparent; color: var(--shell-text-primary); cursor: pointer; font: inherit; font-size: 13px; text-align: left; }.directory-category:hover, .directory-type:hover { background: var(--shell-tool-hover); }.directory-category.selected { color: var(--shell-tool-header-text); }.directory-children { padding-left: 24px; }.directory-type.active { background: var(--shell-tool-selected-bg); color: var(--shell-tool-header-text); }.disclosure { width: 11px; color: var(--shell-tool-subtle-text); font-size: 17px; }.folder-icon { color: #d79a4b; font-size: 15px; }.dictionary-icon { color: var(--shell-text-secondary); font-size: 14px; }.directory-name { min-width: 0; flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }.directory-count { min-width: 18px; padding: 1px 5px; border-radius: 3px; background: var(--shell-tool-tag-bg); color: var(--shell-tool-subtle-text); font-size: 11px; text-align: center; }
.explorer-footer { min-height: 54px; gap: 11px; padding: 0 15px; border-top: 1px solid var(--shell-tool-divider); }.explorer-footer i { height: 16px; border-left: 1px solid var(--shell-tool-divider); }.explorer-footer :deep(.el-button) { padding: 0; color: var(--shell-text-secondary); font-size: 12px; }
.splitter { position: relative; z-index: 2; flex: 0 0 auto; background: var(--shell-tool-divider); }.splitter:hover, .splitter:active { background: var(--shell-accent); }.splitter.vertical { width: 1px; cursor: col-resize; }.splitter.horizontal { height: 1px; cursor: row-resize; }
.main-workspace { display: flex; min-width: 0; flex: 1; flex-direction: column; overflow: hidden; }.dictionary-pane { display: flex; min-height: 270px; flex: 0 0 auto; flex-direction: column; overflow: hidden; }.items-pane { display: flex; min-height: 250px; flex: 1; flex-direction: column; overflow: hidden; }
.compact-toolbar { min-height: 54px; justify-content: space-between; gap: 12px; padding: 0 16px; background: var(--shell-tool-surface); }.toolbar-actions, .toolbar-search { gap: 6px; min-width: 0; }.toolbar-actions { flex-wrap: wrap; }.toolbar-search { flex: 0 1 335px; }.toolbar-search :deep(.el-input) { min-width: 165px; }.compact-toolbar :deep(.el-button) { --el-button-border-color: var(--shell-tool-border-strong); --el-button-bg-color: var(--shell-tool-surface); --el-button-text-color: var(--shell-text-secondary); height: 29px; border-radius: 4px; }.compact-toolbar :deep(.el-button--primary) { --el-button-bg-color: var(--shell-accent); --el-button-border-color: var(--shell-accent); --el-button-text-color: #fff; }
.filter-strip { display: flex; align-items: center; gap: 8px; padding: 0 16px 10px; color: var(--shell-tool-subtle-text); font-size: 12px; }.filter-strip :deep(.el-select) { width: 120px; }
.dense-table { --el-table-border-color: var(--shell-tool-divider); --el-table-header-bg-color: var(--shell-tool-toolbar-bg); --el-table-tr-bg-color: var(--shell-tool-surface); --el-table-row-hover-bg-color: var(--shell-tool-hover); --el-table-current-row-bg-color: var(--shell-tool-selected-bg); flex: 1; font-size: 12px; }.dense-table :deep(th.el-table__cell) { height: 36px; padding: 0; color: var(--shell-tool-subtle-text); font-size: 12px; font-weight: 600; }.dense-table :deep(td.el-table__cell) { height: 36px; padding: 0; }.dense-table :deep(.el-table__inner-wrapper::before) { background-color: var(--shell-tool-divider); }.code-cell :deep(.cell) { font-family: Consolas, 'JetBrains Mono', monospace; }.ellipsis { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.status-badge { display: inline-flex; align-items: center; gap: 5px; padding: 2px 7px; border: 1px solid color-mix(in srgb, var(--shell-accent) 25%, transparent); border-radius: 3px; background: color-mix(in srgb, var(--shell-accent) 10%, transparent); color: var(--shell-accent); font-size: 12px; }.status-badge i { width: 6px; height: 6px; border-radius: 50%; background: currentColor; }.status-badge.disabled { border-color: var(--shell-tool-border-strong); background: var(--shell-tool-tag-bg); color: var(--shell-tool-subtle-text); }
.table-footer { min-height: 45px; justify-content: space-between; gap: 12px; padding: 0 16px; border-top: 1px solid var(--shell-tool-divider); color: var(--shell-text-secondary); font-size: 12px; }.items-title { height: 42px; padding: 0 16px; border-bottom: 1px solid var(--shell-tool-divider); color: var(--shell-accent); font-size: 14px; font-weight: 650; line-height: 42px; }.item-toolbar { min-height: 50px; }.item-table { min-height: 0; }.drawer-footer { justify-content: flex-end; gap: 8px; }.full-width { width: 100%; }.field-hint { margin: 5px 0 0; color: var(--shell-tool-subtle-text); font-size: 12px; line-height: 1.45; }.form-grid { align-items: flex-start; gap: 18px; }.form-grid :deep(.el-form-item) { flex: 1; }.category-setting-toolbar { display: flex; justify-content: flex-end; margin-bottom: 10px; }
@media (max-width: 900px) { .dict-console { display: block; overflow: auto; }.resource-explorer { width: 100% !important; min-height: 250px; }.splitter.vertical { display: none; }.main-workspace { min-width: 900px; }.dictionary-pane { height: 380px !important; } }
</style>
