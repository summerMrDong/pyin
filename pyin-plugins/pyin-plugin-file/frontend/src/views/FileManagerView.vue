<template>
  <section class="file-page">
    <header class="hero">
      <div>
        <p class="eyebrow">External Plugin</p>
        <h1>文件管理</h1>
        <p class="subtitle">按 bucket 管理本地 OSS 化存储，支持上传、预览、下载与业务对象关联查询。</p>
      </div>
      <button class="ghost-button" @click="refreshAll">刷新数据</button>
    </header>

    <p v-if="errorMessage" class="feedback error">{{ errorMessage }}</p>
    <p v-if="successMessage" class="feedback success">{{ successMessage }}</p>

    <section class="stats-grid">
      <article class="stat-card accent">
        <span>有效文件数</span>
        <strong>{{ summary.fileCount }}</strong>
      </article>
      <article class="stat-card">
        <span>存储总大小</span>
        <strong>{{ formatSize(summary.totalSize) }}</strong>
      </article>
      <article class="stat-card">
        <span>Bucket 数量</span>
        <strong>{{ buckets.length }}</strong>
      </article>
    </section>

    <section class="workspace">
      <article class="panel">
        <div class="panel-header">
          <div>
            <h2>上传文件</h2>
            <p>文件名仅作展示，真实存储路径由后端生成，前端不能传 objectKey。</p>
          </div>
        </div>
        <form class="editor-form" @submit.prevent="submitUpload">
          <label>
            <span>Bucket</span>
            <select v-model="uploadForm.bucketName">
              <option value="">默认 bucket (business)</option>
              <option v-for="bucket in buckets" :key="bucket.bucketName" :value="bucket.bucketName">
                {{ bucket.bucketName }} / {{ bucket.description }}
              </option>
            </select>
          </label>
          <div class="inline-fields">
            <label>
              <span>业务类型</span>
              <input v-model.trim="uploadForm.bizType" placeholder="例如 notice">
            </label>
            <label>
              <span>业务 ID</span>
              <input v-model.trim="uploadForm.bizId" placeholder="例如 10001">
            </label>
          </div>
          <label>
            <span>选择文件</span>
            <input type="file" @change="handleFileChange">
          </label>
          <div class="form-actions">
            <button type="button" class="ghost-button" @click="resetUploadForm">清空</button>
            <button type="submit" class="primary-button">上传文件</button>
          </div>
        </form>
      </article>

      <article class="panel">
        <div class="panel-header">
          <div>
            <h2>Bucket 配置</h2>
            <p>bucket 只能来自系统配置，首版预置 5 个业务桶。</p>
          </div>
        </div>
        <div class="bucket-grid">
          <article v-for="bucket in buckets" :key="bucket.bucketName" class="bucket-card">
            <strong>{{ bucket.bucketName }}</strong>
            <span>{{ bucket.description }}</span>
            <small>{{ bucket.publicRead ? 'PUBLIC_READ' : 'PRIVATE' }}</small>
            <p>{{ bucket.allowedTypes.join(', ') }}</p>
          </article>
        </div>
      </article>
    </section>

    <article class="panel">
      <div class="panel-header">
        <div>
          <h2>附件列表</h2>
          <p>支持按业务类型和业务 ID 查询未删除的附件。</p>
        </div>
        <div class="toolbar">
          <input v-model.trim="queryForm.bizType" class="search-input" placeholder="业务类型">
          <input v-model.trim="queryForm.bizId" class="search-input" placeholder="业务 ID">
          <button class="ghost-button" @click="loadFiles">筛选</button>
        </div>
      </div>

      <table class="data-table">
        <thead>
          <tr>
            <th>原始文件名</th>
            <th>Bucket</th>
            <th>对象 Key</th>
            <th>大小</th>
            <th>类型</th>
            <th>业务关联</th>
            <th>下载次数</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="file in files" :key="file.fileId">
            <td>{{ file.originalName }}</td>
            <td>{{ file.bucketName }}</td>
            <td class="value-cell">{{ file.objectKey }}</td>
            <td>{{ formatSize(file.fileSize) }}</td>
            <td>{{ file.realMimeType }}</td>
            <td>{{ file.bizType || '-' }} / {{ file.bizId || '-' }}</td>
            <td>{{ file.downloadCount }}</td>
            <td class="actions">
              <a class="text-button" :href="file.downloadUrl" target="_blank" rel="noreferrer">下载</a>
              <a
                v-if="file.image"
                class="text-button"
                :href="file.previewUrl"
                target="_blank"
                rel="noreferrer"
              >
                预览
              </a>
              <button class="text-button danger" @click="removeFile(file.fileId)">删除</button>
            </td>
          </tr>
          <tr v-if="files.length === 0">
            <td colspan="8" class="empty-state">当前筛选条件下没有附件。</td>
          </tr>
        </tbody>
      </table>
    </article>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { deleteFile, fetchBuckets, fetchFiles, fetchSummary, uploadFile } from '../api/fileAdminApi'

const summary = reactive({
  fileCount: 0,
  totalSize: 0,
  bucketCount: 0
})
const buckets = ref([])
const files = ref([])
const selectedFile = ref(null)
const errorMessage = ref('')
const successMessage = ref('')

const uploadForm = reactive({
  bucketName: '',
  bizType: '',
  bizId: ''
})

const queryForm = reactive({
  bizType: '',
  bizId: ''
})

onMounted(async () => {
  await refreshAll()
})

async function refreshAll() {
  clearMessages()
  await Promise.all([loadSummary(), loadBuckets(), loadFiles()])
}

async function loadSummary() {
  Object.assign(summary, await fetchSummary())
}

async function loadBuckets() {
  buckets.value = await fetchBuckets()
}

async function loadFiles() {
  try {
    files.value = await fetchFiles(queryForm.bizType, queryForm.bizId)
  } catch (error) {
    errorMessage.value = error.message
  }
}

function handleFileChange(event) {
  selectedFile.value = event.target.files?.[0] ?? null
}

async function submitUpload() {
  if (!selectedFile.value) {
    errorMessage.value = '请先选择文件。'
    return
  }
  try {
    clearMessages()
    const formData = new FormData()
    formData.append('file', selectedFile.value)
    if (uploadForm.bucketName) {
      formData.append('bucketName', uploadForm.bucketName)
    }
    if (uploadForm.bizType) {
      formData.append('bizType', uploadForm.bizType)
    }
    if (uploadForm.bizId) {
      formData.append('bizId', uploadForm.bizId)
    }
    await uploadFile(formData)
    successMessage.value = '文件已上传。'
    resetUploadForm()
    await refreshAll()
  } catch (error) {
    errorMessage.value = error.message
  }
}

function resetUploadForm() {
  uploadForm.bucketName = ''
  uploadForm.bizType = ''
  uploadForm.bizId = ''
  selectedFile.value = null
}

async function removeFile(fileId) {
  try {
    clearMessages()
    await deleteFile(fileId)
    successMessage.value = '文件已逻辑删除。'
    await refreshAll()
  } catch (error) {
    errorMessage.value = error.message
  }
}

function clearMessages() {
  errorMessage.value = ''
  successMessage.value = ''
}

function formatSize(size) {
  if (!size) {
    return '0 B'
  }
  if (size < 1024) {
    return `${size} B`
  }
  if (size < 1024 * 1024) {
    return `${(size / 1024).toFixed(1)} KB`
  }
  return `${(size / 1024 / 1024).toFixed(1)} MB`
}
</script>

<style scoped>
.file-page {
  display: grid;
  gap: 24px;
  color: var(--text-primary);
}

.hero,
.panel,
.stat-card,
.bucket-card {
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
    radial-gradient(circle at top right, rgba(191, 90, 36, 0.16), transparent 35%),
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

.stats-grid,
.workspace,
.panel-header,
.editor-form,
.inline-fields,
.toolbar,
.form-actions,
.bucket-grid {
  display: grid;
  gap: 16px;
}

.stats-grid {
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
}

.workspace {
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
}

.bucket-grid {
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
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
  background: linear-gradient(135deg, rgba(191, 90, 36, 0.16), var(--panel-bg));
}

.bucket-card {
  padding: 16px;
}

.bucket-card strong,
.bucket-card span,
.bucket-card small,
.bucket-card p {
  display: block;
}

.bucket-card span,
.bucket-card small,
.bucket-card p {
  margin-top: 6px;
  color: var(--text-secondary);
}

.editor-form label {
  display: grid;
  gap: 8px;
  color: var(--text-secondary);
  font-size: 14px;
}

.editor-form input,
.editor-form select,
.search-input {
  border: 1px solid var(--panel-border);
  background: rgba(255, 255, 255, 0.72);
  border-radius: 14px;
  padding: 11px 12px;
  font: inherit;
  color: var(--text-primary);
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
  max-width: 280px;
  word-break: break-word;
}

.actions {
  white-space: nowrap;
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
  background: #bf5a24;
  color: white;
}

.ghost-button {
  background: rgba(15, 23, 42, 0.06);
  color: var(--text-primary);
}

.text-button {
  background: transparent;
  color: #bf5a24;
  padding: 0 8px 0 0;
  text-decoration: none;
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
  .hero,
  .toolbar {
    grid-template-columns: 1fr;
  }
}
</style>
