<template>
  <main class="workbench">
    <header class="topbar">
      <div class="title-block">
        <strong>导出工坊</strong>
        <span>{{ store.active?.name || '未打开模板' }}</span>
      </div>
      <div class="top-actions">
        <div class="top-split">
          <button class="new-command" @click="newBlank">＋ 新建</button>
          <button class="new-more" title="新建选项" @click="topNewOpen = !topNewOpen">⌄</button>
          <div v-if="topNewOpen" class="top-menu">
            <button @click="runTopAction(newBlank)">▦ 新建空白模板</button>
            <button @click="runTopAction(newFolder)">▸ 新建目录</button>
            <button @click="runTopAction(triggerImport)">⇧ 导入 XLSX / JSON</button>
            <button @click="runTopAction(mountLocal)">▣ 挂载本地目录</button>
            <button @click="runTopAction(mountNetwork)">↗ 挂载网络模板</button>
          </div>
        </div>
        <button class="command-button" :disabled="!store.active || busy" @click="save">▣ 保存</button>
        <button class="command-button run-command" :disabled="!snapshot || busy" @click="runDebug">
          {{ busy ? '◌ 处理中…' : '▶ 运行预览' }}
        </button>
      </div>
    </header>

    <TemplateTree
      :nodes="store.nodes"
      :active-id="store.active ? `template-${store.active.id}` : ''"
      @open="openTemplate"
      @blank="newBlank"
      @folder="newFolder"
      @upload="upload"
      @rename="rename"
      @delete="remove"
      @mount-network="mountNetwork"
      @mount-local="mountLocal"
      @fork="fork"
    />

    <section class="editor-area">
      <EditorTabs :tabs="store.tabs" :active-id="store.active?.id" @select="openTemplate" @close="store.closeTab" @rename="rename" />
      <div v-if="store.loading || editorLoading" class="editor-loading" role="status"><span></span>{{ store.loading ? '正在读取模板…' : '正在加载表格编辑器…' }}</div>
      <UniverWrapper :snapshot="snapshot" :changed-cells="store.changedCells" @update:snapshot="snapshot = $event" @create="newBlank" @import="triggerImport" />
    </section>

    <DebugPanel
      v-model="mockJson"
      :collapsed="panelCollapsed"
      :status="status"
      :disabled="!snapshot || busy"
      @run="runDebug"
      @export="exportFile"
      @toggle="togglePanel"
    />

    <StatusBar :template="store.active" :message="status" :variable-count="automaticVariableCount" :changed-count="store.changedCells.length" />
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
import StatusBar from '../components/StatusBar.vue'
import PrintPreviewDialog from '../components/PrintPreviewDialog.vue'
import { useWorkshopStore } from '../stores/workshop'
import { workshopApi } from '../api/workshop'
import { cloneWorkbook, materializeWorkbook, templateVariables } from '../composables/workbook'
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
const topNewOpen = ref(false)
const activeId = computed(() => store.active?.id)
const automaticVariableCount = computed(() => templateVariables(snapshot.value).length)

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

function ask(label: string, initial = '') { return window.prompt(label, initial)?.trim() }
async function openTemplate(id: number) {
  if (store.loading || store.active?.id === id) return
  status.value = '正在打开模板…'
  try {
    await store.openTemplate(id)
  } catch (error: any) {
    status.value = error.message || '打开模板失败'
  }
}
async function newBlank() { const name = ask('模板名称', '新建导出模板'); if (!name) return; const item = await workshopApi.createBlank({ name }); await store.refreshTree(); await openTemplate(item.id) }
async function newFolder() { const name = ask('目录名称'); if (!name) return; await workshopApi.createFolder({ name }); await store.refreshTree() }
function runTopAction(action: () => void | Promise<void>) { topNewOpen.value = false; void action() }
function triggerImport() { document.querySelector<HTMLInputElement>('.tree-panel input[type="file"]')?.click() }
async function upload(file: File) { status.value = '正在上传模板…'; try { const item = await workshopApi.upload(null, file); await store.refreshTree(); await openTemplate(item.id); status.value = '模板已导入' } catch (error: any) { status.value = error.message } }
async function mountNetwork() { const url = ask('网络模板 URL'); if (!url) return; status.value = '正在挂载网络模板…'; try { const item = await workshopApi.mountNetwork({ url }); await store.refreshTree(); await store.openTemplate(item.id); status.value = '网络模板已挂载' } catch (error: any) { status.value = error.message } }
async function mountLocal() { const roots = await workshopApi.roots(); const root = ask(`本地来源目录（可选：${roots.join('；') || '未配置'}）`, roots[0] || ''); if (!root) return; try { await workshopApi.mountDirectory({ root }); await store.refreshTree(); status.value = '本地目录已挂载' } catch (error: any) { status.value = error.message } }
async function rename(nodeId: string, name: string) { await workshopApi.rename(nodeId, name); await store.refreshTree(); if (store.active && nodeId === `template-${store.active.id}`) store.active.name = name }
async function remove(nodeId: string) { if (!window.confirm('确定删除此资源？')) return; await workshopApi.remove(nodeId); store.closeTab(Number(nodeId.replace('template-', ''))); await store.refreshTree() }
async function fork(id: number) { try { const item = await workshopApi.fork(id); await store.refreshTree(); await openTemplate(item.id); status.value = '已创建可编辑副本' } catch (error: any) { status.value = error.message } }
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

function togglePanel() { panelCollapsed.value = !panelCollapsed.value; localStorage.setItem('export-workshop.panel', panelCollapsed.value ? 'collapsed' : 'open') }
</script>

<style>
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
  height:calc(100vh - 88px);min-height:650px;display:grid;grid-template-columns:260px minmax(0,1fr);grid-template-rows:46px minmax(0,1fr) auto 28px;overflow:hidden;border:1px solid var(--border);border-radius:8px;background:var(--surface);color:var(--text);font-family:var(--font)
}
.topbar{grid-column:1/-1;display:flex;align-items:center;justify-content:space-between;gap:18px;padding:6px 10px;border-bottom:1px solid var(--divider);background:var(--surface)}.title-block{display:flex;align-items:baseline;gap:10px;min-width:0}.title-block strong{font-size:15px;color:var(--shell-tool-header-text,var(--text))}.title-block span{overflow:hidden;color:var(--muted);text-overflow:ellipsis;white-space:nowrap;font-size:12px}.top-actions{display:flex;align-items:center;gap:5px}.topbar button{height:30px;padding:0 9px;border:1px solid var(--border);border-radius:4px;cursor:pointer;font:12px var(--font)}.topbar button:focus-visible{outline:2px solid var(--accent);outline-offset:1px}.topbar button:disabled{cursor:not-allowed;opacity:.48}.top-split{position:relative;display:flex}.new-command{border-radius:4px 0 0 4px!important;background:var(--button);color:var(--text)}.new-more{width:23px;padding:0!important;border-left:0!important;border-radius:0 4px 4px 0!important;background:var(--button);color:var(--text)}.command-button{background:transparent;color:var(--text)}.topbar button:not(:disabled):hover{background:var(--hover)}.topbar .run-command{border-color:var(--accent);background:var(--accent);color:#fff;font-weight:600}.topbar .run-command:not(:disabled):hover{filter:brightness(.94);background:var(--accent)}.top-menu{position:absolute;z-index:30;top:34px;right:0;min-width:185px;padding:4px;border:1px solid var(--border);border-radius:6px;background:var(--surface);box-shadow:0 8px 24px #0002}.top-menu button{display:block;width:100%;border:0!important;background:transparent!important;color:var(--text);text-align:left}.top-menu button:hover{background:var(--hover)!important}
.tree-panel{grid-column:1;grid-row:2/4}.editor-area{grid-column:2;grid-row:2;position:relative;display:grid;grid-template-rows:auto minmax(0,1fr);min-width:0;min-height:0}.editor-loading{position:absolute;z-index:10;top:44px;left:50%;display:flex;align-items:center;gap:8px;transform:translateX(-50%);padding:7px 11px;border:1px solid var(--border);border-radius:5px;background:var(--surface-raised);box-shadow:0 6px 18px #0002;color:var(--muted);font-size:12px}.editor-loading span{width:12px;height:12px;border:2px solid var(--border);border-top-color:var(--accent);border-radius:50%;animation:editor-spin .7s linear infinite}.debug-panel{grid-column:2;grid-row:3}.status{grid-column:1/-1;grid-row:4}@keyframes editor-spin{to{transform:rotate(360deg)}}
@media(max-width:900px){.workbench{grid-template-columns:220px minmax(0,1fr)}}
@media(max-width:700px){.workbench{height:auto;min-height:700px;grid-template-columns:1fr;grid-template-rows:46px minmax(360px,1fr) auto 28px}.topbar{grid-column:1}.title-block span{display:none}.top-actions{gap:3px}.topbar button{padding:0 7px}.tree-panel{display:none}.editor-area,.debug-panel{grid-column:1}.status{grid-column:1}}
</style>
