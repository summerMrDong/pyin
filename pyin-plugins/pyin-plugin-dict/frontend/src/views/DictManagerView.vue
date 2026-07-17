<template>
  <section class="dict-page">
    <header class="hero">
      <div>
        <p class="eyebrow">System Plugin</p>
        <h1>字典管理</h1>
        <p class="subtitle">维护字典类型与字典项，适合承载系统级标签、状态枚举和展示文案。</p>
      </div>
      <button class="ghost-button" @click="refreshAll">刷新数据</button>
    </header>

    <p v-if="errorMessage" class="feedback error">{{ errorMessage }}</p>
    <p v-if="successMessage" class="feedback success">{{ successMessage }}</p>

    <section class="overview-grid">
      <article class="stat-card accent">
        <span>字典类型数</span>
        <strong>{{ types.length }}</strong>
      </article>
      <article class="stat-card">
        <span>字典项数</span>
        <strong>{{ items.length }}</strong>
      </article>
      <article class="stat-card">
        <span>当前类型</span>
        <strong>{{ selectedTypeLabel }}</strong>
      </article>
    </section>

    <section class="workspace">
      <article class="panel">
        <div class="panel-header">
          <div>
            <h2>字典类型</h2>
            <p>定义每个字典的编码、名称和启用状态。</p>
          </div>
          <button class="primary-button" @click="editType(null)">新增类型</button>
        </div>

        <div class="type-grid">
          <button
            class="type-card"
            :class="{ active: selectedTypeId === null }"
            @click="selectType(null)"
          >
            <strong>全部类型</strong>
            <span>查看所有字典项</span>
          </button>
          <button
            v-for="type in types"
            :key="type.id"
            class="type-card"
            :class="{ active: selectedTypeId === type.id }"
            @click="selectType(type.id)"
          >
            <strong>{{ type.typeName }}</strong>
            <span>{{ type.typeCode }}</span>
            <small>{{ type.itemCount }} 个字典项</small>
          </button>
        </div>

        <table class="data-table">
          <thead>
            <tr>
              <th>类型编码</th>
              <th>类型名称</th>
              <th>状态</th>
              <th>字典项数</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="type in types" :key="type.id">
              <td>{{ type.typeCode }}</td>
              <td>{{ type.typeName }}</td>
              <td>{{ type.status }}</td>
              <td>{{ type.itemCount }}</td>
              <td class="actions">
                <button class="text-button" @click="editType(type)">编辑</button>
                <button class="text-button danger" @click="removeType(type.id)">删除</button>
              </td>
            </tr>
            <tr v-if="types.length === 0">
              <td colspan="5" class="empty-state">还没有字典类型，先创建一个吧。</td>
            </tr>
          </tbody>
        </table>
      </article>

      <article class="panel">
        <div class="panel-header">
          <div>
            <h2>字典项</h2>
            <p>维护当前字典类型下的值、标签与排序。</p>
          </div>
          <button class="primary-button" @click="editItem(null)">新增字典项</button>
        </div>

        <table class="data-table">
          <thead>
            <tr>
              <th>字典值</th>
              <th>字典标签</th>
              <th>类型</th>
              <th>排序</th>
              <th>状态</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in items" :key="item.id">
              <td>{{ item.itemValue }}</td>
              <td>{{ item.itemLabel }}</td>
              <td>{{ item.typeCode }}</td>
              <td>{{ item.itemSort }}</td>
              <td>{{ item.itemStatus }}</td>
              <td class="actions">
                <button class="text-button" @click="editItem(item)">编辑</button>
                <button class="text-button danger" @click="removeItem(item.id)">删除</button>
              </td>
            </tr>
            <tr v-if="items.length === 0">
              <td colspan="6" class="empty-state">当前筛选条件下没有字典项。</td>
            </tr>
          </tbody>
        </table>
      </article>
    </section>

    <section class="form-grid">
      <article class="panel">
        <div class="panel-header">
          <div>
            <h2>{{ typeForm.id ? '编辑字典类型' : '新增字典类型' }}</h2>
            <p>字典类型定义好之后，右侧即可补充字典项。</p>
          </div>
        </div>
        <form class="editor-form" @submit.prevent="submitType">
          <label>
            <span>类型编码</span>
            <input v-model.trim="typeForm.typeCode" required placeholder="例如 order_status">
          </label>
          <label>
            <span>类型名称</span>
            <input v-model.trim="typeForm.typeName" required placeholder="例如 订单状态">
          </label>
          <div class="inline-fields">
            <label>
              <span>状态</span>
              <select v-model="typeForm.status">
                <option>ENABLED</option>
                <option>DISABLED</option>
              </select>
            </label>
            <label>
              <span>描述</span>
              <input v-model.trim="typeForm.description" placeholder="说明这个字典的用途">
            </label>
          </div>
          <div class="form-actions">
            <button type="button" class="ghost-button" @click="resetTypeForm">清空</button>
            <button type="submit" class="primary-button">保存字典类型</button>
          </div>
        </form>
      </article>

      <article class="panel">
        <div class="panel-header">
          <div>
            <h2>{{ itemForm.id ? '编辑字典项' : '新增字典项' }}</h2>
            <p>字典项决定前端/业务系统最终展示给用户的标签内容。</p>
          </div>
        </div>
        <form class="editor-form" @submit.prevent="submitItem">
          <label>
            <span>所属类型</span>
            <select v-model.number="itemForm.typeId" required>
              <option disabled value="">请选择字典类型</option>
              <option v-for="type in types" :key="type.id" :value="type.id">
                {{ type.typeName }} ({{ type.typeCode }})
              </option>
            </select>
          </label>
          <div class="inline-fields">
            <label>
              <span>字典值</span>
              <input v-model.trim="itemForm.itemValue" required placeholder="例如 PAID">
            </label>
            <label>
              <span>字典标签</span>
              <input v-model.trim="itemForm.itemLabel" required placeholder="例如 已支付">
            </label>
          </div>
          <div class="inline-fields">
            <label>
              <span>排序</span>
              <input v-model.number="itemForm.itemSort" type="number" min="0">
            </label>
            <label>
              <span>状态</span>
              <select v-model="itemForm.itemStatus">
                <option>ENABLED</option>
                <option>DISABLED</option>
              </select>
            </label>
          </div>
          <label>
            <span>描述</span>
            <textarea v-model.trim="itemForm.description" rows="3" placeholder="描述这个字典项的业务含义"></textarea>
          </label>
          <div class="form-actions">
            <button type="button" class="ghost-button" @click="resetItemForm">清空</button>
            <button type="submit" class="primary-button">保存字典项</button>
          </div>
        </form>
      </article>
    </section>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import {
  deleteItem,
  deleteType,
  fetchItemDetail,
  fetchItems,
  fetchTypes,
  saveItem,
  saveType
} from '../api/dictAdminApi'

const types = ref([])
const items = ref([])
const selectedTypeId = ref(null)
const errorMessage = ref('')
const successMessage = ref('')

const typeForm = reactive(createTypeForm())
const itemForm = reactive(createItemForm())

const selectedTypeLabel = computed(() => {
  if (selectedTypeId.value == null) {
    return '全部类型'
  }
  const type = types.value.find((item) => item.id === selectedTypeId.value)
  return type ? type.typeName : '未选择'
})

onMounted(async () => {
  await refreshAll()
})

async function refreshAll() {
  clearMessages()
  await Promise.all([loadTypes(), loadItems()])
}

async function loadTypes() {
  types.value = await fetchTypes()
  if (selectedTypeId.value != null && !types.value.some((item) => item.id === selectedTypeId.value)) {
    selectedTypeId.value = null
  }
}

async function loadItems() {
  try {
    items.value = await fetchItems(selectedTypeId.value)
  } catch (error) {
    errorMessage.value = error.message
  }
}

function selectType(id) {
  selectedTypeId.value = id
  loadItems()
}

function editType(type) {
  Object.assign(typeForm, createTypeForm(type))
}

function resetTypeForm() {
  Object.assign(typeForm, createTypeForm())
}

async function submitType() {
  try {
    clearMessages()
    await saveType(typeForm)
    successMessage.value = '字典类型已保存。'
    resetTypeForm()
    await loadTypes()
  } catch (error) {
    errorMessage.value = error.message
  }
}

async function removeType(id) {
  try {
    clearMessages()
    await deleteType(id)
    successMessage.value = '字典类型已删除。'
    if (selectedTypeId.value === id) {
      selectedTypeId.value = null
    }
    await Promise.all([loadTypes(), loadItems()])
  } catch (error) {
    errorMessage.value = error.message
  }
}

async function editItem(item) {
  if (!item?.id) {
    Object.assign(itemForm, createItemForm({ typeId: selectedTypeId.value ?? '' }))
    return
  }
  try {
    clearMessages()
    const detail = await fetchItemDetail(item.id)
    Object.assign(itemForm, createItemForm(detail))
  } catch (error) {
    errorMessage.value = error.message
  }
}

function resetItemForm() {
  Object.assign(itemForm, createItemForm({ typeId: selectedTypeId.value ?? '' }))
}

async function submitItem() {
  try {
    clearMessages()
    await saveItem(itemForm)
    successMessage.value = '字典项已保存。'
    resetItemForm()
    await Promise.all([loadItems(), loadTypes()])
  } catch (error) {
    errorMessage.value = error.message
  }
}

async function removeItem(id) {
  try {
    clearMessages()
    await deleteItem(id)
    successMessage.value = '字典项已删除。'
    await Promise.all([loadItems(), loadTypes()])
  } catch (error) {
    errorMessage.value = error.message
  }
}

function clearMessages() {
  errorMessage.value = ''
  successMessage.value = ''
}

function createTypeForm(source = {}) {
  return {
    id: source.id ?? null,
    typeCode: source.typeCode ?? '',
    typeName: source.typeName ?? '',
    status: source.status ?? 'ENABLED',
    description: source.description ?? ''
  }
}

function createItemForm(source = {}) {
  return {
    id: source.id ?? null,
    typeId: source.typeId ?? '',
    itemValue: source.itemValue ?? '',
    itemLabel: source.itemLabel ?? '',
    itemSort: source.itemSort ?? 100,
    itemStatus: source.itemStatus ?? 'ENABLED',
    description: source.description ?? ''
  }
}
</script>

<style scoped>
.dict-page {
  display: grid;
  gap: 24px;
  color: var(--text-primary);
}

.hero,
.panel,
.stat-card,
.type-card {
  border: 1px solid var(--panel-border);
  background: var(--panel-bg);
  box-shadow: var(--panel-shadow);
  border-radius: 24px;
}

.hero,
.panel {
  padding: 24px;
}

.hero {
  display: flex;
  justify-content: space-between;
  gap: 20px;
  align-items: flex-start;
  background:
    radial-gradient(circle at top right, rgba(34, 139, 84, 0.16), transparent 35%),
    var(--panel-bg);
}

.eyebrow {
  margin: 0 0 8px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  font-size: 12px;
  color: var(--text-secondary);
}

h1,
h2,
p {
  margin: 0;
}

.subtitle {
  margin-top: 10px;
  max-width: 720px;
  color: var(--text-secondary);
}

.overview-grid,
.workspace,
.form-grid,
.inline-fields,
.panel-header,
.form-actions {
  display: grid;
  gap: 16px;
}

.overview-grid {
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
}

.workspace,
.form-grid {
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
}

.stat-card {
  padding: 18px 20px;
}

.stat-card span {
  display: block;
  color: var(--text-secondary);
  font-size: 13px;
}

.stat-card strong {
  display: block;
  margin-top: 8px;
  font-size: 28px;
}

.stat-card.accent {
  background: linear-gradient(135deg, rgba(34, 139, 84, 0.14), var(--panel-bg));
}

.type-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 12px;
  margin: 18px 0 20px;
}

.type-card {
  padding: 16px;
  text-align: left;
  cursor: pointer;
}

.type-card strong,
.type-card span,
.type-card small {
  display: block;
}

.type-card span,
.type-card small {
  margin-top: 6px;
  color: var(--text-secondary);
}

.type-card.active {
  border-color: #228b54;
  background: rgba(34, 139, 84, 0.08);
}

.data-table {
  width: 100%;
  border-collapse: collapse;
}

.data-table th,
.data-table td {
  padding: 12px 10px;
  border-bottom: 1px solid var(--panel-border);
  text-align: left;
}

.actions {
  white-space: nowrap;
}

.editor-form {
  display: grid;
  gap: 14px;
}

.editor-form label {
  display: grid;
  gap: 8px;
  color: var(--text-secondary);
  font-size: 14px;
}

.editor-form input,
.editor-form select,
.editor-form textarea {
  border: 1px solid var(--panel-border);
  background: rgba(255, 255, 255, 0.72);
  border-radius: 14px;
  padding: 11px 12px;
  font: inherit;
  color: var(--text-primary);
}

.primary-button,
.ghost-button,
.text-button {
  border: 0;
  cursor: pointer;
  font: inherit;
}

.primary-button,
.ghost-button {
  padding: 10px 14px;
  border-radius: 999px;
}

.primary-button {
  background: #228b54;
  color: white;
}

.ghost-button {
  background: rgba(15, 23, 42, 0.06);
  color: var(--text-primary);
}

.text-button {
  background: transparent;
  color: #228b54;
  padding: 0 8px 0 0;
}

.text-button.danger {
  color: #9f1239;
}

.feedback {
  margin: 0;
  padding: 12px 14px;
  border-radius: 16px;
}

.feedback.error {
  background: rgba(159, 18, 57, 0.12);
  color: #9f1239;
}

.feedback.success {
  background: rgba(20, 83, 45, 0.12);
  color: #166534;
}

.empty-state {
  text-align: center;
  color: var(--text-secondary);
}

@media (max-width: 760px) {
  .hero {
    grid-template-columns: 1fr;
  }
}
</style>
