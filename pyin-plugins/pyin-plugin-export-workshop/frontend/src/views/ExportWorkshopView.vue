<template>
  <main class="workbench" :class="{ 'sidebar-collapsed': sidebarCollapsed }">
    <header class="topbar">
      <div class="title-block">
        <span>{{ store.active?.name || '未打开模板' }}</span>
      </div>
      <div class="top-actions">
        <button class="command-button" :disabled="!store.active || busy" @click="save">▣ 保存</button>
        <button class="command-button run-command" :disabled="!snapshot || busy" @click="runDebug">
          {{ busy ? '◌ 处理中…' : '▶ 运行预览' }}
        </button>
      </div>
    </header>

    <div class="workbench-body">
      <el-splitter layout="horizontal" class="workspace-splitter">
        <el-splitter-panel :size="sidebarPanelSize" :min="28" :max="508" @update:size="onSidebarSizeChange">
          <div class="left-dock">
            <nav class="tool-rail" aria-label="工作台工具栏">
              <button :class="{ active: !sidebarCollapsed }" :title="sidebarCollapsed ? '打开资源区' : '隐藏资源区'" :aria-label="sidebarCollapsed ? '打开资源区' : '隐藏资源区'" @click="toggleSidebar">资源</button>
              <button :class="{ active: !panelCollapsed }" :title="panelCollapsed ? '打开调试台' : '隐藏调试台'" :aria-label="panelCollapsed ? '打开调试台' : '隐藏调试台'" @click="togglePanel">调试台</button>
            </nav>
            <TemplateTree
              v-if="!sidebarCollapsed"
              :nodes="store.nodes"
              :active-id="store.active ? `template-${store.active.id}` : ''"
              @open="openTemplate"
              @create-online="newBlank"
              @create-import="openImportDialog"
              @create-network="openNetworkDialog"
              @create-directory="openDirectoryDialog"
              @folder="newFolder"
              @rename="rename"
              @delete="remove"
              @fork="fork"
              @download="downloadTemplate"
              @copy-download-link="copyDownloadLink"
            />
          </div>
        </el-splitter-panel>

        <el-splitter-panel>
          <el-splitter layout="vertical" class="editor-splitter">
            <el-splitter-panel>
              <section class="editor-area">
                <EditorTabs :tabs="store.tabs" :active-id="store.active?.id" @select="openTemplate" @close="store.closeTab" @rename="rename" />
                <div v-if="store.loading || editorLoading" class="editor-loading" role="status"><span></span>{{ store.loading ? '正在读取模板…' : '正在加载表格编辑器…' }}</div>
                <UniverWrapper :snapshot="snapshot" :changed-cells="store.changedCells" @update:snapshot="snapshot = $event" @create="newBlank" @import="triggerImport" />
              </section>
            </el-splitter-panel>

            <el-splitter-panel v-if="!panelCollapsed" :size="debugHeight" :min="130" @update:size="onDebugSizeChange">
              <DebugPanel
                v-model="mockJson"
                :collapsed="panelCollapsed"
                :status="status"
                :disabled="!snapshot || busy"
                @run="runDebug"
                @export="exportFile"
              />
            </el-splitter-panel>
          </el-splitter>
        </el-splitter-panel>
      </el-splitter>
    </div>

    <el-dialog v-model="createDialogOpen" class="template-create-dialog" title="新建导出模板" width="370px" :close-on-click-modal="false" @closed="resetCreateDraft">
      <el-form label-position="top" @submit.prevent="createBlank">
        <el-form-item label="模板 ID">
          <el-input v-model.trim="createDraft.id" maxlength="80" placeholder="留空后自动生成唯一 ID" />
          <div class="form-hint">仅支持字母、数字、下划线和短横线。</div>
        </el-form-item>
        <el-form-item label="模板名称" required>
          <el-input v-model.trim="createDraft.name" maxlength="128" placeholder="例如：订单导出模板" @keyup.enter="createBlank" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogOpen = false">取消</el-button>
        <el-button type="primary" :loading="creatingTemplate" @click="createBlank">创建模板</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="importDialogOpen" class="template-create-dialog" title="导入模板文件" width="370px" :close-on-click-modal="false">
      <el-form label-position="top"><el-form-item label="模板 ID"><el-input v-model.trim="importDraft.id" maxlength="80" placeholder="留空后自动生成唯一 ID" /><div class="form-hint">仅支持字母、数字、下划线和短横线。</div></el-form-item></el-form>
      <el-upload :auto-upload="false" :limit="1" accept=".xlsx,.json" @change="selectImportFile">
        <el-button>选择 XLSX / JSON 文件</el-button>
        <template #tip><div class="form-hint">支持 XLSX 与 JSON 模板文件。</div></template>
      </el-upload>
      <template #footer><el-button @click="importDialogOpen = false">取消</el-button><el-button type="primary" :disabled="!importDraft.file" :loading="sourceCreating" @click="confirmImport">导入</el-button></template>
    </el-dialog>

    <el-dialog v-model="networkDialogOpen" class="template-create-dialog" title="添加网络模板" width="390px" :close-on-click-modal="false">
      <el-form label-position="top"><el-form-item label="模板 ID"><el-input v-model.trim="networkDraft.id" maxlength="80" placeholder="留空后自动生成唯一 ID" /><div class="form-hint">仅支持字母、数字、下划线和短横线。</div></el-form-item><el-form-item label="模板名称" required><el-input v-model.trim="networkDraft.name" placeholder="例如：共享订单模板" /></el-form-item><el-form-item label="模板地址" required><el-input v-model.trim="networkDraft.url" placeholder="https://example.com/template.xlsx" /></el-form-item></el-form>
      <template #footer><el-button @click="networkDialogOpen = false">取消</el-button><el-button type="primary" :loading="sourceCreating" @click="confirmNetwork">添加</el-button></template>
    </el-dialog>

    <el-dialog v-model="directoryDialogOpen" class="template-create-dialog" title="挂载本地模板目录" width="390px" :close-on-click-modal="false">
      <el-form label-position="top"><el-form-item label="模板 ID 前缀"><el-input v-model.trim="directoryDraft.idPrefix" maxlength="76" placeholder="留空后每个模板自动生成唯一 ID" /><div class="form-hint">填写 report 后会按文件顺序生成 report_1、report_2 等 ID。</div></el-form-item><el-form-item label="本地目录" required><el-select v-model="directoryDraft.root" placeholder="选择已配置的目录" style="width:100%"><el-option v-for="root in localRoots" :key="root" :label="root" :value="root" /></el-select></el-form-item></el-form>
      <template #footer><el-button @click="directoryDialogOpen = false">取消</el-button><el-button type="primary" :disabled="!directoryDraft.root" :loading="sourceCreating" @click="confirmDirectory">挂载</el-button></template>
    </el-dialog>

    <el-dialog v-model="folderDialogOpen" class="template-create-dialog compact-resource-dialog" title="新建目录" width="320px" :close-on-click-modal="false" @closed="resetFolderDraft">
      <el-form label-position="top" @submit.prevent="createFolder">
        <el-form-item><el-input v-model.trim="folderDraft.name" maxlength="128" placeholder="目录名称" autofocus @keyup.enter="createFolder" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="folderDialogOpen = false">取消</el-button><el-button type="primary" :disabled="!folderDraft.name.trim()" :loading="resourceSaving" @click="createFolder">创建</el-button></template>
    </el-dialog>

    <el-dialog v-model="renameDialogOpen" class="template-create-dialog compact-resource-dialog" title="重命名" width="320px" :close-on-click-modal="false" @closed="resetRenameDraft">
      <el-form label-position="top" @submit.prevent="confirmRename">
        <el-form-item><el-input v-model.trim="renameDraft.name" maxlength="128" placeholder="输入新名称" autofocus @keyup.enter="confirmRename" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="renameDialogOpen = false">取消</el-button><el-button type="primary" :disabled="!renameDraft.name.trim()" :loading="resourceSaving" @click="confirmRename">确定</el-button></template>
    </el-dialog>

    <PrintPreviewDialog
      :open="previewOpen"
      :template="store.active"
      :snapshot="previewSnapshot"
      :changed-cells="store.changedCells"
      :changed-count="store.changedCells.length"
      :download-url="downloadUrl"
      @close="previewOpen = false"
    />
  </main>
</template>

<script setup lang="ts">
import { computed, defineAsyncComponent, onMounted, ref, watch } from 'vue'
import TemplateTree from '../components/TemplateTree.vue'
import EditorTabs from '../components/EditorTabs.vue'
import DebugPanel from '../components/DebugPanel.vue'
import PrintPreviewDialog from '../components/PrintPreviewDialog.vue'
import { useWorkshopStore } from '../stores/workshop'
import { workshopApi } from '../api/workshop'
import { cloneWorkbook, materializeWorkbook } from '../composables/workbook'
import { exportWorkbook } from '../composables/exporter'

const editorLoading = ref(false)
const UniverWrapper = defineAsyncComponent({
  loader: async () => {
    editorLoading.value = true
    try { return await import('../components/UniverWrapper.vue') }
    finally { editorLoading.value = false }
  },
  delay: 80,
  suspensible: false,
})

const store = useWorkshopStore()
const snapshot = ref<any>()
const previewSnapshot = ref<any>()
const mockJson = ref('{\n  "customer": { "name": "Pyin" },\n  "order": { "amount": 128 }\n}')
const status = ref('请选择或创建一个模板')
const panelCollapsed = ref(localStorage.getItem('export-workshop.panel') === 'collapsed')
const previewOpen = ref(false)
const downloadUrl = ref('')
const busy = ref(false)
const createDialogOpen = ref(false)
const creatingTemplate = ref(false)
const createDraft = ref<{ directoryId?: number; id: string; name: string }>({ id: '', name: '' })
const importDialogOpen = ref(false)
const networkDialogOpen = ref(false)
const directoryDialogOpen = ref(false)
const sourceCreating = ref(false)
const localRoots = ref<string[]>([])
const importDraft = ref<{ directoryId?: number; id: string; file?: File }>({ id: '' })
const networkDraft = ref<{ directoryId?: number; id: string; name: string; url: string }>({ id: '', name: '', url: '' })
const directoryDraft = ref<{ directoryId?: number; idPrefix: string; root: string }>({ idPrefix: '', root: '' })
const folderDialogOpen = ref(false)
const renameDialogOpen = ref(false)
const resourceSaving = ref(false)
const folderDraft = ref<{ parentId?: number; name: string }>({ name: '' })
const renameDraft = ref<{ nodeId: string; name: string }>({ nodeId: '', name: '' })
const sidebarWidth = ref(Number(localStorage.getItem('export-workshop.sidebar-width')) || 260)
const sidebarCollapsed = ref(localStorage.getItem('export-workshop.sidebar') === 'collapsed')
const debugHeight = ref(Number(localStorage.getItem('export-workshop.debug-height')) || 210)
const activeId = computed(() => store.active?.id)
const sidebarPanelSize = computed(() => sidebarCollapsed.value ? 28 : sidebarWidth.value + 28)

watch(activeId, () => {
  snapshot.value = store.active?.workbookSnapshot ? cloneWorkbook(store.active.workbookSnapshot) : undefined
  previewSnapshot.value = undefined
  store.changedCells = []
  downloadUrl.value = ''
  status.value = store.active
    ? '在单元格中输入 {{customer.name}} 等模板语法，然后运行预览'
    : '请选择或创建一个模板'
}, { immediate: true })

onMounted(async () => {
  try {
    await store.refreshTree()
    const firstTemplate = store.nodes.find(node => node.nodeType === 'TEMPLATE')
    if (firstTemplate) await openTemplate(Number(String(firstTemplate.id).replace('template-', '')))
  }
  catch (error: any) { status.value = error.message || '模板资源加载失败' }
})

async function openTemplate(id: number) {
  if (store.loading || store.active?.id === id) return
  status.value = '正在打开模板…'
  try {
    await store.openTemplate(id)
  } catch (error: any) {
    status.value = error.message || '打开模板失败'
  }
}
function newBlank(directoryId?: number) {
  createDraft.value = { directoryId, id: '', name: '新建导出模板' }
  createDialogOpen.value = true
}
function resetCreateDraft() { createDraft.value = { id: '', name: '' } }
async function createBlank() {
  const name = createDraft.value.name.trim()
  if (!name || creatingTemplate.value) return
  creatingTemplate.value = true
  try {
    const item = await workshopApi.createBlank({ name, directoryId: createDraft.value.directoryId, id: createDraft.value.id || undefined })
    await store.refreshTree(); await openTemplate(item.id); createDialogOpen.value = false; status.value = '模板已创建'
  } catch (error: any) { status.value = error.message || '创建模板失败' }
  finally { creatingTemplate.value = false }
}
function newFolder(parentId?: number) { folderDraft.value = { parentId, name: '' }; folderDialogOpen.value = true }
function resetFolderDraft() { folderDraft.value = { name: '' } }
async function createFolder() {
  const name = folderDraft.value.name.trim()
  if (!name || resourceSaving.value) return
  resourceSaving.value = true
  try {
    await workshopApi.createFolder({ name, parentId: folderDraft.value.parentId })
    await store.refreshTree(); folderDialogOpen.value = false; status.value = '目录已创建'
  } catch (error: any) { status.value = error.message || '创建目录失败' }
  finally { resourceSaving.value = false }
}
function triggerImport() { openImportDialog() }
async function upload(file: File, directoryId?: number, id?: string) { status.value = '正在上传模板…'; try { const item = await workshopApi.upload(directoryId ?? null, id, file); await store.refreshTree(); await openTemplate(item.id); status.value = '模板已导入' } catch (error: any) { status.value = error.message } }
function openImportDialog(directoryId?: number) { importDraft.value = { directoryId, id: '' }; importDialogOpen.value = true }
function selectImportFile(file: any) { importDraft.value.file = file.raw as File }
function openNetworkDialog(directoryId?: number) { networkDraft.value = { directoryId, id: '', name: '', url: '' }; networkDialogOpen.value = true }
async function openDirectoryDialog(directoryId?: number) { directoryDraft.value = { directoryId, idPrefix: '', root: '' }; localRoots.value = await workshopApi.roots(); if (localRoots.value.length === 1) directoryDraft.value.root = localRoots.value[0]; directoryDialogOpen.value = true }
async function confirmImport() { if (!importDraft.value.file || sourceCreating.value) return; sourceCreating.value = true; try { await upload(importDraft.value.file, importDraft.value.directoryId, importDraft.value.id || undefined); importDialogOpen.value = false } finally { sourceCreating.value = false } }
async function confirmNetwork() { if (!networkDraft.value.name || !networkDraft.value.url || sourceCreating.value) return; sourceCreating.value = true; try { const item = await workshopApi.mountNetwork(networkDraft.value); await store.refreshTree(); await openTemplate(item.id); networkDialogOpen.value = false; status.value = '网络模板已添加' } catch (error: any) { status.value = error.message || '网络模板添加失败' } finally { sourceCreating.value = false } }
async function confirmDirectory() { if (!directoryDraft.value.root || sourceCreating.value) return; sourceCreating.value = true; try { await workshopApi.mountDirectory(directoryDraft.value); await store.refreshTree(); directoryDialogOpen.value = false; status.value = '本地目录已挂载' } catch (error: any) { status.value = error.message || '本地目录挂载失败' } finally { sourceCreating.value = false } }
function rename(nodeId: string, name: string) { renameDraft.value = { nodeId, name }; renameDialogOpen.value = true }
function resetRenameDraft() { renameDraft.value = { nodeId: '', name: '' } }
async function confirmRename() {
  const { nodeId } = renameDraft.value
  const name = renameDraft.value.name.trim()
  if (!nodeId || !name || resourceSaving.value) return
  resourceSaving.value = true
  try {
    await workshopApi.rename(nodeId, name)
    await store.refreshTree()
    if (store.active && nodeId === `template-${store.active.id}`) store.active.name = name
    const tab = store.tabs.find(item => nodeId === `template-${item.id}`)
    if (tab) tab.name = name
    renameDialogOpen.value = false; status.value = '已重命名'
  } catch (error: any) { status.value = error.message || '重命名失败' }
  finally { resourceSaving.value = false }
}
async function remove(nodeId: string) { if (!window.confirm('确定删除此资源？')) return; await workshopApi.remove(nodeId); store.closeTab(Number(nodeId.replace('template-', ''))); await store.refreshTree() }
async function fork(id: number) { try { const item = await workshopApi.fork(id); await store.refreshTree(); await openTemplate(item.id); status.value = '已创建可编辑副本' } catch (error: any) { status.value = error.message } }
function downloadTemplate(node: any) { window.open(workshopApi.templateDownloadUrl(node.templateId), '_blank', 'noopener') }
async function copyDownloadLink(node: any) {
  const url = new URL(workshopApi.templateDownloadUrl(node.templateId), window.location.origin).toString()
  try { await navigator.clipboard.writeText(url); status.value = '下载链接已复制' }
  catch { status.value = `下载链接：${url}` }
}
async function save() { if (!snapshot.value || !store.active) return; busy.value = true; status.value = '正在保存模板…'; try { await store.save(snapshot.value); status.value = '模板已保存' } catch (error: any) { status.value = error.message } finally { busy.value = false } }

async function renderPreview(openDialog = true) {
  if (!snapshot.value || !store.active) { status.value = '请先选择一个模板'; return false }
  try {
    const mockData = JSON.parse(mockJson.value)
    busy.value = true
    status.value = '正在填充数据并生成预览…'
    const result = await workshopApi.debug({ workbookSnapshot: snapshot.value, mockData, mappings: store.mappings })
    store.changedCells = result.changedCells || []
    previewSnapshot.value = materializeWorkbook(result.workbookSnapshot || snapshot.value, store.changedCells)
    downloadUrl.value = ''
    status.value = store.changedCells.length
      ? `运行完成，已填充 ${store.changedCells.length} 个单元格`
      : '运行完成；未识别到可填充的模板语法，当前预览与原模板一致'
    if (openDialog) previewOpen.value = true
    return true
  } catch (error: any) {
    status.value = error instanceof SyntaxError ? 'Mock JSON 格式错误，请修正后重试' : (error.message || '运行失败')
    return false
  } finally { busy.value = false }
}

async function runDebug() { await renderPreview(true) }
async function exportFile() {
  if (!await renderPreview(false) || !snapshot.value || !store.active) return
  try {
    busy.value = true
    status.value = '正在生成 XLSX…'
    const blob = await exportWorkbook(snapshot.value, store.changedCells)
    const task = await workshopApi.createExport(store.active.id, { fileName: `${store.active.name}.xlsx` })
    const completed = await workshopApi.uploadExport(task.taskId, blob)
    downloadUrl.value = completed.downloadUrl
    previewOpen.value = true
    status.value = '导出完成，可在预览窗口下载 XLSX'
  } catch (error: any) { status.value = error.message || '导出失败' } finally { busy.value = false }
}

function toggleSidebar() { sidebarCollapsed.value = !sidebarCollapsed.value; localStorage.setItem('export-workshop.sidebar', sidebarCollapsed.value ? 'collapsed' : 'open') }
function togglePanel() { panelCollapsed.value = !panelCollapsed.value; localStorage.setItem('export-workshop.panel', panelCollapsed.value ? 'collapsed' : 'open') }
function onSidebarSizeChange(size: number | string) {
  const nextWidth = Math.round(Number(size) - 28)
  if (!Number.isFinite(nextWidth)) return
  sidebarWidth.value = Math.min(Math.max(nextWidth, 190), 480)
  localStorage.setItem('export-workshop.sidebar-width', String(sidebarWidth.value))
}
function onDebugSizeChange(size: number | string) {
  const nextHeight = Math.round(Number(size))
  if (!Number.isFinite(nextHeight)) return
  debugHeight.value = Math.max(nextHeight, 130)
  localStorage.setItem('export-workshop.debug-height', String(debugHeight.value))
}
</script>

<style>
/* IDE 风格停靠工作台。 */
.workbench{
  --font:var(--shell-font-sans,"Segoe UI","PingFang SC",sans-serif);
  --mono:"Cascadia Mono",Consolas,monospace;
  --surface:var(--shell-tool-surface,#fcfcfd);
  --surface-raised:var(--shell-tool-surface-muted,#f5f7fa);
  --input:var(--shell-tool-code-bg,#f3f5f8);
  --button:var(--shell-tool-toolbar-bg,#f8fafc);
  --text:var(--shell-text-primary,#172033);
  --muted:var(--shell-tool-subtle-text,#64748b);
  --border:var(--shell-tool-border-strong,rgba(148,163,184,.3));
  --divider:var(--shell-tool-divider,rgba(148,163,184,.2));
  --hover:var(--shell-tool-hover,rgba(15,23,42,.04));
  --selection:var(--shell-tool-selected-bg,rgba(15,118,110,.1));
  --accent:var(--shell-accent,#0f766e);
  --changed:color-mix(in srgb,var(--shell-accent-strong,#b45309) 20%,var(--surface));
  height:100%;min-height:650px;display:grid;grid-template-rows:46px minmax(0,1fr);overflow:hidden;border:1px solid var(--border);border-radius:8px;background:var(--surface);color:var(--text);font-family:var(--font)
}
.topbar{display:flex;align-items:center;justify-content:space-between;gap:18px;padding:6px 10px;border-bottom:1px solid var(--divider);background:var(--surface)}.title-block{display:flex;align-items:baseline;gap:10px;min-width:0}.title-block strong{font-size:15px;color:var(--shell-tool-header-text,var(--text))}.title-block span{overflow:hidden;color:var(--muted);text-overflow:ellipsis;white-space:nowrap;font-size:12px}.top-actions{display:flex;align-items:center;gap:5px}.topbar button{height:30px;padding:0 9px;border:1px solid var(--border);border-radius:4px;cursor:pointer;font:12px var(--font)}.topbar button:focus-visible{outline:2px solid var(--accent);outline-offset:1px}.topbar button:disabled{cursor:not-allowed;opacity:.48}.command-button{background:transparent;color:var(--text)}.topbar button:not(:disabled):hover{background:var(--hover)}.topbar .run-command{border-color:var(--accent);background:var(--accent);color:#fff;font-weight:600}.topbar .run-command:not(:disabled):hover{filter:brightness(.94);background:var(--accent)}
.template-create-dialog{border:1px solid var(--border);background:var(--surface)}.template-create-dialog .el-dialog__title,.template-create-dialog .el-form-item__label{color:var(--text)}.template-create-dialog .el-dialog__header,.template-create-dialog .el-dialog__footer{margin:0;padding:10px 14px;border-color:var(--divider)}.template-create-dialog .el-dialog__header{border-bottom:1px solid var(--divider)}.template-create-dialog .el-dialog__footer{border-top:1px solid var(--divider)}.template-create-dialog .el-dialog__body{padding:12px 14px}.template-create-dialog .el-form-item{margin-bottom:12px}.template-create-dialog .el-form-item__label{padding-bottom:4px;font-size:12px}.template-create-dialog .el-input__wrapper{box-shadow:0 0 0 1px var(--border) inset;background:var(--input)}.template-create-dialog .el-input__inner{color:var(--text)}.compact-resource-dialog .el-dialog__header,.compact-resource-dialog .el-dialog__footer{padding:7px 9px}.compact-resource-dialog .el-dialog__body{padding:8px 9px}.compact-resource-dialog .el-dialog__title{font-size:13px;line-height:18px}.compact-resource-dialog .el-dialog__headerbtn{top:7px;width:25px;height:25px}.compact-resource-dialog .el-input__wrapper{min-height:26px;padding:0 8px}.compact-resource-dialog .el-input__inner{font-size:11px}.compact-resource-dialog .el-button{height:25px;padding:0 9px;font-size:11px}.compact-resource-dialog .el-form-item{margin-bottom:0}.form-hint{margin-top:3px;color:var(--muted);font-size:11px}
.workbench-body{min-width:0;min-height:0}.workspace-splitter,.editor-splitter{min-width:0;min-height:0}.left-dock{display:grid;grid-template-columns:28px minmax(0,1fr);height:100%;min-width:0;min-height:0}.tool-rail{display:flex;flex-direction:column;align-items:center;border-right:1px solid var(--divider);background:var(--surface-raised)}.tool-rail button{display:grid;place-items:center;box-sizing:border-box;flex:0 0 auto;width:26px;height:56px;padding:11px 0;border:0;border-bottom:1px solid var(--divider);border-radius:0;background:transparent;color:var(--muted);cursor:pointer;font:11px/1 var(--font);letter-spacing:0;writing-mode:vertical-rl}.tool-rail button:hover{background:var(--hover);color:var(--text)}.tool-rail button.active{background:var(--surface);color:var(--accent);box-shadow:inset 2px 0 var(--accent)}.left-dock>.tree-panel{min-width:0;min-height:0}.editor-area{position:relative;display:grid;grid-template-rows:auto minmax(0,1fr);width:100%;height:100%;min-width:0;min-height:0}.editor-loading{position:absolute;z-index:10;top:44px;left:50%;display:flex;align-items:center;gap:8px;transform:translateX(-50%);padding:7px 11px;border:1px solid var(--border);border-radius:5px;background:var(--surface-raised);box-shadow:0 6px 18px #0002;color:var(--muted);font-size:12px}.editor-loading span{width:12px;height:12px;border:2px solid var(--border);border-top-color:var(--accent);border-radius:50%;animation:editor-spin .7s linear infinite}.debug-panel{height:100%;min-height:0}.workspace-splitter .el-splitter-bar__dragger-horizontal:before{width:1px;background:var(--divider)}.editor-splitter .el-splitter-bar__dragger-vertical:before{height:1px;background:var(--divider)}.workspace-splitter .el-splitter-bar__dragger:hover:not(.is-disabled):before,.editor-splitter .el-splitter-bar__dragger:hover:not(.is-disabled):before{background:var(--accent)}@keyframes editor-spin{to{transform:rotate(360deg)}}
@media(max-width:700px){.workbench{height:auto;min-height:700px}.title-block span{display:none}.top-actions{gap:3px}.topbar button{padding:0 7px}.workspace-splitter>.el-splitter-panel:first-child,.workspace-splitter>.el-splitter-bar{display:none}.editor-splitter{height:100%}}
</style>
