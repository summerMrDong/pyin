<template>
  <main class="workbench">
    <header class="topbar">
      <div class="title-block">
        <strong>导出工坊</strong>
        <span>设计模板、绑定数据并预览导出结果</span>
      </div>
      <div class="top-actions">
        <button class="secondary-button" @click="newBlank">新建模板</button>
        <button class="secondary-button" :disabled="!store.active || busy" @click="save">保存模板</button>
        <button class="primary-button" :disabled="!snapshot || busy" @click="runDebug">
          {{ busy ? '处理中…' : '运行并预览' }}
        </button>
      </div>
    </header>

    <nav class="workflow" aria-label="导出流程">
      <div :class="{ done: !!store.active, current: !store.active }"><b>1</b><span>选择模板<small>{{ store.active?.name || '请从左侧选择' }}</small></span></div>
      <i></i>
      <div :class="{ done: bindingCount > 0, current: !!store.active && bindingCount === 0 }"><b>2</b><span>绑定数据<small>{{ automaticVariableCount ? `${automaticVariableCount} 个自动变量` : `${store.mappings.length} 个自定义映射` }}</small></span></div>
      <i></i>
      <div :class="{ done: !!previewSnapshot, current: bindingCount > 0 && !previewSnapshot }"><b>3</b><span>运行预览<small>{{ previewSnapshot ? '结果已生成' : '检查填充效果' }}</small></span></div>
      <i></i>
      <div :class="{ done: !!downloadUrl }"><b>4</b><span>导出 XLSX<small>{{ downloadUrl ? '文件已就绪' : '确认后下载' }}</small></span></div>
    </nav>

    <TemplateTree
      :nodes="store.nodes"
      :active-id="store.active ? `template-${store.active.id}` : ''"
      @open="store.openTemplate"
      @blank="newBlank"
      @folder="newFolder"
      @upload="upload"
      @rename="rename"
      @delete="remove"
      @mount-network="mountNetwork"
      @mount-local="mountLocal"
    />

    <section class="editor-area">
      <EditorTabs :tabs="store.tabs" :active-id="store.active?.id" @select="store.openTemplate" @close="store.closeTab" @rename="rename" />
      <UniverWrapper :snapshot="snapshot" :changed-cells="store.changedCells" @update:snapshot="snapshot = $event" @cell-select="selectedCell = $event" />
    </section>

    <DebugPanel
      v-model="mockJson"
      :mappings="store.mappings"
      :selected-cell="selectedCell"
      :collapsed="panelCollapsed"
      :status="status"
      :automatic-variable-count="automaticVariableCount"
      :disabled="!snapshot || busy"
      @run="runDebug"
      @export="exportFile"
      @toggle="togglePanel"
    />

    <StatusBar :template="store.active" :message="status" :mapping-count="bindingCount" :changed-count="store.changedCells.length" />
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
import { computed, onMounted, ref, watch } from 'vue'
import { createPinia, setActivePinia } from 'pinia'
import TemplateTree from '../components/TemplateTree.vue'
import EditorTabs from '../components/EditorTabs.vue'
import UniverWrapper from '../components/UniverWrapper.vue'
import DebugPanel from '../components/DebugPanel.vue'
import StatusBar from '../components/StatusBar.vue'
import PrintPreviewDialog from '../components/PrintPreviewDialog.vue'
import { useWorkshopStore } from '../stores/workshop'
import { workshopApi } from '../api/workshop'
import { cloneWorkbook, materializeWorkbook, templateVariables } from '../composables/workbook'
import { exportWorkbook } from '../composables/exporter'

setActivePinia(createPinia())
const store = useWorkshopStore()
const snapshot = ref<any>()
const previewSnapshot = ref<any>()
const mockJson = ref('{\n  "customer": { "name": "Pyin" },\n  "order": { "amount": 128 }\n}')
const selectedCell = ref<any>()
const status = ref('请选择或创建一个模板')
const panelCollapsed = ref(localStorage.getItem('export-workshop.panel') === 'collapsed')
const previewOpen = ref(false)
const downloadUrl = ref('')
const busy = ref(false)
const activeId = computed(() => store.active?.id)
const automaticVariableCount = computed(() => templateVariables(snapshot.value).length)
const bindingCount = computed(() => automaticVariableCount.value + store.mappings.length)

watch(activeId, () => {
  snapshot.value = store.active?.workbookSnapshot ? cloneWorkbook(store.active.workbookSnapshot) : undefined
  previewSnapshot.value = undefined
  store.changedCells = []
  downloadUrl.value = ''
  status.value = store.active ? '模板已打开，可以编辑单元格并绑定数据' : '请选择或创建一个模板'
}, { immediate: true })

onMounted(async () => { await store.refreshTree() })

function ask(label: string, initial = '') { return window.prompt(label, initial)?.trim() }
async function newBlank() { const name = ask('模板名称', '新建导出模板'); if (!name) return; const item = await workshopApi.createBlank({ name }); await store.refreshTree(); await store.openTemplate(item.id) }
async function newFolder() { const name = ask('目录名称'); if (!name) return; await workshopApi.createFolder({ name }); await store.refreshTree() }
async function upload(file: File) { status.value = '正在上传模板…'; try { const item = await workshopApi.upload(null, file); await store.refreshTree(); await store.openTemplate(item.id); status.value = '模板已导入' } catch (error: any) { status.value = error.message } }
async function mountNetwork() { const url = ask('网络模板 URL'); if (!url) return; status.value = '正在挂载网络模板…'; try { const item = await workshopApi.mountNetwork({ url }); await store.refreshTree(); await store.openTemplate(item.id); status.value = '网络模板已挂载' } catch (error: any) { status.value = error.message } }
async function mountLocal() { const roots = await workshopApi.roots(); const root = ask(`本地来源目录（可选：${roots.join('；') || '未配置'}）`, roots[0] || ''); if (!root) return; try { await workshopApi.mountDirectory({ root }); await store.refreshTree(); status.value = '本地目录已挂载' } catch (error: any) { status.value = error.message } }
async function rename(nodeId: string, name: string) { await workshopApi.rename(nodeId, name); await store.refreshTree(); if (store.active && nodeId === `template-${store.active.id}`) store.active.name = name }
async function remove(nodeId: string) { if (!window.confirm('确定删除此资源？')) return; await workshopApi.remove(nodeId); store.closeTab(Number(nodeId.replace('template-', ''))); await store.refreshTree() }
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
      : '运行完成；尚未配置变量映射，当前预览与原模板一致'
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
  height:calc(100vh - 88px);min-height:650px;display:grid;grid-template-columns:260px minmax(0,1fr);grid-template-rows:64px 54px minmax(0,1fr) auto 28px;overflow:hidden;border:1px solid var(--border);border-radius:8px;background:var(--surface);color:var(--text);font-family:var(--font)
}
.topbar{grid-column:1/-1;display:flex;align-items:center;justify-content:space-between;gap:18px;padding:10px 14px;border-bottom:1px solid var(--divider);background:var(--surface)}.title-block{display:flex;align-items:baseline;gap:12px}.title-block strong{font-size:18px;color:var(--shell-tool-header-text,var(--text))}.title-block span{font-size:12px;color:var(--muted)}.top-actions{display:flex;gap:7px}.topbar button{padding:7px 12px;border:1px solid var(--border);border-radius:6px;cursor:pointer;font:inherit}.topbar button:disabled{cursor:not-allowed;opacity:.48}.secondary-button{background:var(--button);color:var(--text)}.primary-button{border-color:var(--accent)!important;background:var(--accent);color:#fff;font-weight:600!important}
.workflow{grid-column:1/-1;display:flex;align-items:center;justify-content:center;gap:10px;padding:7px 16px;border-bottom:1px solid var(--divider);background:var(--surface-raised)}.workflow>div{display:flex;align-items:center;gap:7px;min-width:120px;color:var(--muted)}.workflow b{display:grid;width:24px;height:24px;place-content:center;border:1px solid var(--border);border-radius:50%;font-size:11px}.workflow span{display:grid;font-size:12px;font-weight:600}.workflow small{font-size:10px;font-weight:400;color:var(--muted)}.workflow i{width:34px;height:1px;background:var(--divider)}.workflow .done b{border-color:var(--accent);background:var(--accent);color:#fff}.workflow .done span,.workflow .current span{color:var(--text)}.workflow .current b{border-color:var(--accent);color:var(--accent);box-shadow:0 0 0 3px var(--selection)}
.tree-panel{grid-column:1;grid-row:3/5}.editor-area{grid-column:2;grid-row:3;display:grid;grid-template-rows:auto minmax(0,1fr);min-width:0;min-height:0}.debug-panel{grid-column:2;grid-row:4}.status{grid-column:1/-1;grid-row:5}
@media(max-width:900px){.workbench{grid-template-columns:220px minmax(0,1fr)}.workflow>div{min-width:auto}.workflow small{display:none}}
@media(max-width:700px){.workbench{height:auto;min-height:760px;grid-template-columns:1fr;grid-template-rows:auto auto minmax(360px,1fr) auto 28px}.topbar{grid-column:1;align-items:flex-start}.title-block{display:grid;gap:2px}.top-actions{flex-wrap:wrap;justify-content:flex-end}.workflow{grid-column:1;justify-content:flex-start;overflow:auto}.workflow i{width:14px;min-width:14px}.tree-panel{display:none}.editor-area,.debug-panel{grid-column:1}.status{grid-column:1}}
</style>
