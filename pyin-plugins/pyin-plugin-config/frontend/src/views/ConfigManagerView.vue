<template>
  <section class="config-page">
    <header class="hero">
      <div>
        <p class="eyebrow">System Plugin</p>
        <h1>配置管理</h1>
        <p class="subtitle">管理命名空间、环境隔离和配置项，当前页面直接连接插件 admin 接口。</p>
      </div>
      <button class="ghost-button" @click="refreshAll">刷新数据</button>
    </header>

    <p v-if="errorMessage" class="feedback error">{{ errorMessage }}</p>
    <p v-if="successMessage" class="feedback success">{{ successMessage }}</p>

    <section class="overview-grid">
      <article class="stat-card accent">
        <span>命名空间数</span>
        <strong>{{ namespaces.length }}</strong>
      </article>
      <article class="stat-card">
        <span>配置项数</span>
        <strong>{{ items.length }}</strong>
      </article>
      <article class="stat-card">
        <span>当前筛选</span>
        <strong>{{ selectedNamespaceLabel }}</strong>
      </article>
    </section>

    <section class="workspace">
      <article class="panel">
        <div class="panel-header">
          <div>
            <h2>命名空间概览</h2>
            <p>按环境查看命名空间，并决定当前配置项管理范围。</p>
          </div>
          <button class="primary-button" @click="editNamespace(null)">新增命名空间</button>
        </div>

        <div class="namespace-grid">
          <button
            class="namespace-card"
            :class="{ active: selectedNamespaceId === null }"
            @click="selectNamespace(null)"
          >
            <strong>全部命名空间</strong>
            <span>查看全部配置项</span>
          </button>
          <button
            v-for="namespace in namespaces"
            :key="namespace.id"
            class="namespace-card"
            :class="{ active: selectedNamespaceId === namespace.id }"
            @click="selectNamespace(namespace.id)"
          >
            <strong>{{ namespace.displayName }}</strong>
            <span>{{ namespace.namespaceCode }} / {{ namespace.env }}</span>
            <small>{{ namespace.itemCount }} 个配置项</small>
          </button>
        </div>

        <table class="data-table">
          <thead>
            <tr>
              <th>命名空间</th>
              <th>环境</th>
              <th>展示名</th>
              <th>配置项数</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="namespace in namespaces" :key="namespace.id">
              <td>{{ namespace.namespaceCode }}</td>
              <td>{{ namespace.env }}</td>
              <td>{{ namespace.displayName }}</td>
              <td>{{ namespace.itemCount }}</td>
              <td class="actions">
                <button class="text-button" @click="editNamespace(namespace)">编辑</button>
                <button class="text-button danger" @click="removeNamespace(namespace.id)">删除</button>
              </td>
            </tr>
            <tr v-if="namespaces.length === 0">
              <td colspan="5" class="empty-state">还没有命名空间，先创建一个吧。</td>
            </tr>
          </tbody>
        </table>
      </article>

      <article class="panel">
        <div class="panel-header">
          <div>
            <h2>配置项列表</h2>
            <p>支持按命名空间筛选并按关键字搜索。</p>
          </div>
          <div class="toolbar">
            <input
              v-model.trim="keyword"
              class="search-input"
              type="search"
              placeholder="搜索配置键或配置值"
              @keyup.enter="loadItems"
            >
            <button class="ghost-button" @click="loadItems">搜索</button>
            <button class="primary-button" @click="editItem(null)">新增配置项</button>
          </div>
        </div>

        <table class="data-table">
          <thead>
            <tr>
              <th>配置键</th>
              <th>配置值</th>
              <th>值类型</th>
              <th>命名空间</th>
              <th>环境</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in items" :key="item.id">
              <td>{{ item.itemKey }}</td>
              <td class="value-cell">{{ item.itemValue }}</td>
              <td>{{ item.valueType }}</td>
              <td>{{ item.namespaceCode }}</td>
              <td>{{ item.env }}</td>
              <td class="actions">
                <button class="text-button" @click="editItem(item)">编辑</button>
                <button class="text-button danger" @click="removeItem(item.id)">删除</button>
              </td>
            </tr>
            <tr v-if="items.length === 0">
              <td colspan="6" class="empty-state">当前筛选条件下没有配置项。</td>
            </tr>
          </tbody>
        </table>
      </article>
    </section>

    <section class="form-grid">
      <article class="panel">
        <div class="panel-header">
          <div>
            <h2>{{ namespaceForm.id ? '编辑命名空间' : '新增命名空间' }}</h2>
            <p>先定义命名空间，再在右侧维护具体配置项。</p>
          </div>
        </div>
        <form class="editor-form" @submit.prevent="submitNamespace">
          <label>
            <span>命名空间编码</span>
            <input v-model.trim="namespaceForm.namespaceCode" required placeholder="例如 order-service">
          </label>
          <label>
            <span>环境</span>
            <input v-model.trim="namespaceForm.env" required placeholder="例如 prod">
          </label>
          <label>
            <span>展示名称</span>
            <input v-model.trim="namespaceForm.displayName" required placeholder="例如 订单服务生产环境">
          </label>
          <label>
            <span>描述</span>
            <textarea v-model.trim="namespaceForm.description" rows="3" placeholder="描述这个命名空间的用途"></textarea>
          </label>
          <div class="form-actions">
            <button type="button" class="ghost-button" @click="resetNamespaceForm">清空</button>
            <button type="submit" class="primary-button">保存命名空间</button>
          </div>
        </form>
      </article>

      <article class="panel">
        <div class="panel-header">
          <div>
            <h2>{{ itemForm.id ? '编辑配置项' : '新增配置项' }}</h2>
            <p>配置项保存后会立即出现在当前命名空间列表中。</p>
          </div>
        </div>
        <form class="editor-form" @submit.prevent="submitItem">
          <label>
            <span>所属命名空间</span>
            <select v-model.number="itemForm.namespaceId" required>
              <option disabled value="">请选择命名空间</option>
              <option v-for="namespace in namespaces" :key="namespace.id" :value="namespace.id">
                {{ namespace.displayName }} ({{ namespace.env }})
              </option>
            </select>
          </label>
          <label>
            <span>配置键</span>
            <input v-model.trim="itemForm.itemKey" required placeholder="例如 order.timeout.seconds">
          </label>
          <label>
            <span>配置值</span>
            <textarea v-model.trim="itemForm.itemValue" rows="3" required placeholder="请输入配置值"></textarea>
          </label>
          <div class="inline-fields">
            <label>
              <span>值类型</span>
              <select v-model="itemForm.valueType">
                <option>STRING</option>
                <option>INTEGER</option>
                <option>BOOLEAN</option>
                <option>JSON</option>
              </select>
            </label>
            <label>
              <span>描述</span>
              <input v-model.trim="itemForm.description" placeholder="用于说明配置项含义">
            </label>
          </div>
          <div class="form-actions">
            <button type="button" class="ghost-button" @click="resetItemForm">清空</button>
            <button type="submit" class="primary-button">保存配置项</button>
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
  deleteNamespace,
  fetchItemDetail,
  fetchItems,
  fetchNamespaces,
  saveItem,
  saveNamespace
} from '../api/configAdminApi'

const namespaces = ref([])
const items = ref([])
const selectedNamespaceId = ref(null)
const keyword = ref('')
const errorMessage = ref('')
const successMessage = ref('')

const namespaceForm = reactive(createNamespaceForm())
const itemForm = reactive(createItemForm())

const selectedNamespaceLabel = computed(() => {
  if (selectedNamespaceId.value == null) {
    return '全部命名空间'
  }
  const namespace = namespaces.value.find((item) => item.id === selectedNamespaceId.value)
  return namespace ? `${namespace.namespaceCode} / ${namespace.env}` : '未选择'
})

onMounted(async () => {
  await refreshAll()
})

async function refreshAll() {
  clearMessages()
  await Promise.all([loadNamespaces(), loadItems()])
}

async function loadNamespaces() {
  namespaces.value = await fetchNamespaces()
  if (
    selectedNamespaceId.value != null &&
    !namespaces.value.some((item) => item.id === selectedNamespaceId.value)
  ) {
    selectedNamespaceId.value = null
  }
}

async function loadItems() {
  try {
    items.value = await fetchItems(selectedNamespaceId.value, keyword.value)
  } catch (error) {
    errorMessage.value = error.message
  }
}

function selectNamespace(id) {
  selectedNamespaceId.value = id
  loadItems()
}

function editNamespace(namespace) {
  Object.assign(namespaceForm, createNamespaceForm(namespace))
}

function resetNamespaceForm() {
  Object.assign(namespaceForm, createNamespaceForm())
}

async function submitNamespace() {
  try {
    clearMessages()
    await saveNamespace(namespaceForm)
    successMessage.value = '命名空间已保存。'
    resetNamespaceForm()
    await loadNamespaces()
  } catch (error) {
    errorMessage.value = error.message
  }
}

async function removeNamespace(id) {
  try {
    clearMessages()
    await deleteNamespace(id)
    successMessage.value = '命名空间已删除。'
    if (selectedNamespaceId.value === id) {
      selectedNamespaceId.value = null
    }
    await Promise.all([loadNamespaces(), loadItems()])
  } catch (error) {
    errorMessage.value = error.message
  }
}

async function editItem(item) {
  if (!item?.id) {
    Object.assign(itemForm, createItemForm({ namespaceId: selectedNamespaceId.value ?? '' }))
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
  Object.assign(itemForm, createItemForm({ namespaceId: selectedNamespaceId.value ?? '' }))
}

async function submitItem() {
  try {
    clearMessages()
    await saveItem(itemForm)
    successMessage.value = '配置项已保存。'
    resetItemForm()
    await loadItems()
    await loadNamespaces()
  } catch (error) {
    errorMessage.value = error.message
  }
}

async function removeItem(id) {
  try {
    clearMessages()
    await deleteItem(id)
    successMessage.value = '配置项已删除。'
    await Promise.all([loadItems(), loadNamespaces()])
  } catch (error) {
    errorMessage.value = error.message
  }
}

function clearMessages() {
  errorMessage.value = ''
  successMessage.value = ''
}

function createNamespaceForm(source = {}) {
  return {
    id: source.id ?? null,
    namespaceCode: source.namespaceCode ?? '',
    env: source.env ?? '',
    displayName: source.displayName ?? '',
    description: source.description ?? ''
  }
}

function createItemForm(source = {}) {
  return {
    id: source.id ?? null,
    namespaceId: source.namespaceId ?? '',
    itemKey: source.itemKey ?? '',
    itemValue: source.itemValue ?? '',
    valueType: source.valueType ?? 'STRING',
    description: source.description ?? ''
  }
}
</script>

<style scoped>
.config-page {
  display: grid;
  gap: 24px;
  color: var(--text-primary);
}

.hero,
.panel,
.stat-card,
.namespace-card {
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
    radial-gradient(circle at top right, rgba(219, 84, 97, 0.16), transparent 35%),
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
.toolbar,
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
  background: linear-gradient(135deg, rgba(219, 84, 97, 0.14), var(--panel-bg));
}

.namespace-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 12px;
  margin: 18px 0 20px;
}

.namespace-card {
  padding: 16px;
  text-align: left;
  cursor: pointer;
}

.namespace-card strong,
.namespace-card span,
.namespace-card small {
  display: block;
}

.namespace-card span,
.namespace-card small {
  margin-top: 6px;
  color: var(--text-secondary);
}

.namespace-card.active {
  border-color: #db5461;
  background: rgba(219, 84, 97, 0.08);
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
  vertical-align: top;
}

.value-cell {
  max-width: 240px;
  word-break: break-word;
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
.editor-form textarea,
.search-input {
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
  background: #db5461;
  color: white;
}

.ghost-button {
  background: rgba(15, 23, 42, 0.06);
  color: var(--text-primary);
}

.text-button {
  background: transparent;
  color: #db5461;
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

  .toolbar {
    grid-template-columns: 1fr;
  }
}
</style>
