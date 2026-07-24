<template>
  <main class="workbench">
    <header class="topbar"><strong>导出工坊</strong><span>模板 / Fesod / XLSX</span><div><button @click="newBlank">新建</button><button @click="save">保存</button></div></header>
    <TemplateTree :nodes="store.nodes" :active-id="store.active ? `template-${store.active.id}` : ''" @open="store.openTemplate" @blank="newBlank" @folder="newFolder" @upload="upload" @rename="rename" @delete="remove" @mount-network="mountNetwork" @mount-local="mountLocal" />
    <section class="editor-area"><EditorTabs :tabs="store.tabs" :active-id="store.active?.id" @select="store.openTemplate" @close="store.closeTab" @rename="rename" /><UniverWrapper :snapshot="snapshot" :changed-cells="store.changedCells" @update:snapshot="snapshot = $event" @cell-select="selectedCell = $event" /></section>
    <DebugPanel v-model="mockJson" :mappings="store.mappings" :selected-cell="selectedCell" :collapsed="panelCollapsed" :pinned="panelPinned" :status="status" @run="runDebug" @export="exportFile" @toggle="togglePanel" />
    <StatusBar :template="store.active" :message="status" :dark="dark" @theme="toggleTheme" />
    <PrintPreviewDialog :open="previewOpen" :template="store.active" :changed-count="store.changedCells.length" :download-url="downloadUrl" @close="previewOpen = false" />
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
import { cloneWorkbook } from '../composables/workbook'
import { exportWorkbook } from '../composables/exporter'

setActivePinia(createPinia())
const store = useWorkshopStore(); const snapshot = ref<any>(); const mockJson = ref('{\n  "customer": { "name": "Pyin" },\n  "order": { "amount": 128 }\n}')
const selectedCell = ref<any>(); const status = ref('就绪'); const panelCollapsed = ref(localStorage.getItem('export-workshop.panel') === 'collapsed'); const panelPinned = ref(localStorage.getItem('export-workshop.panel') === 'pinned')
const dark = ref(localStorage.getItem('export-workshop.theme') === 'dark'); const previewOpen = ref(false); const downloadUrl = ref('')
const activeId = computed(() => store.active?.id)
watch(activeId, () => { snapshot.value = store.active?.workbookSnapshot ? cloneWorkbook(store.active.workbookSnapshot) : undefined }, { immediate: true })
onMounted(async () => { document.documentElement.classList.toggle('dark', dark.value); await store.refreshTree() })
function ask(label: string, initial = '') { return window.prompt(label, initial)?.trim() }
async function newBlank() { const name = ask('模板名称', '新建模板'); if (!name) return; const item = await workshopApi.createBlank({ name }); await store.refreshTree(); await store.openTemplate(item.id) }
async function newFolder() { const name = ask('目录名称'); if (!name) return; await workshopApi.createFolder({ name }); await store.refreshTree() }
async function upload(file: File) { status.value = '正在上传模板…'; try { const item = await workshopApi.upload(null, file); await store.refreshTree(); await store.openTemplate(item.id); status.value = '模板已导入' } catch (error: any) { status.value = error.message } }
async function mountNetwork() { const url = ask('网络模板 URL'); if (!url) return; status.value = '正在挂载网络模板…'; try { const item = await workshopApi.mountNetwork({ url }); await store.refreshTree(); await store.openTemplate(item.id); status.value = '网络模板已挂载' } catch (error: any) { status.value = error.message } }
async function mountLocal() { const roots = await workshopApi.roots(); const root = ask(`本地来源目录（可选：${roots.join('；') || '未配置'}）`, roots[0] || ''); if (!root) return; try { await workshopApi.mountDirectory({ root }); await store.refreshTree(); status.value = '本地目录已挂载' } catch (error: any) { status.value = error.message } }
async function rename(nodeId: string, name: string) { await workshopApi.rename(nodeId, name); await store.refreshTree(); if (store.active && nodeId === `template-${store.active.id}`) store.active.name = name }
async function remove(nodeId: string) { if (!window.confirm('确定删除此资源？')) return; await workshopApi.remove(nodeId); store.closeTab(Number(nodeId.replace('template-', ''))); await store.refreshTree() }
async function save() { if (!snapshot.value || !store.active) return; status.value = '正在保存…'; try { await store.save(snapshot.value); status.value = '已保存' } catch (error: any) { status.value = error.message } }
async function runDebug() { if (!snapshot.value) return; try { const mockData = JSON.parse(mockJson.value); status.value = 'Fesod 正在填充…'; const result = await workshopApi.debug({ workbookSnapshot: snapshot.value, mockData, mappings: store.mappings }); store.changedCells = result.changedCells || []; status.value = `已填充 ${store.changedCells.length} 个单元格` } catch (error: any) { status.value = error instanceof SyntaxError ? 'Mock JSON 格式错误' : error.message } }
async function exportFile() { if (!snapshot.value || !store.active) return; try { status.value = '正在生成 XLSX…'; const blob = await exportWorkbook(snapshot.value, store.changedCells); status.value = '正在保存导出任务…'; const task = await workshopApi.createExport(store.active.id, { fileName: `${store.active.name}.xlsx` }); const completed = await workshopApi.uploadExport(task.taskId, blob); downloadUrl.value = completed.downloadUrl; previewOpen.value = true; status.value = '导出完成' } catch (error: any) { status.value = error.message || '导出失败' } }
function togglePanel() { panelCollapsed.value = !panelCollapsed.value; localStorage.setItem('export-workshop.panel', panelCollapsed.value ? 'collapsed' : panelPinned.value ? 'pinned' : 'open') }
function toggleTheme() { dark.value = !dark.value; document.documentElement.classList.toggle('dark', dark.value); localStorage.setItem('export-workshop.theme', dark.value ? 'dark' : 'light') }
</script>

<style>
:root { --font:"JetBrains Mono","Cascadia Mono",Consolas,monospace; --surface:#fff;--surface-raised:#f5f5f5;--input:#fff;--button:#f5f5f5;--text:#242424;--muted:#6f6f6f;--border:#d0d0d0;--hover:#e9e9e9;--selection:#dcecff;--accent:#3b82f6;--changed:#fff6c5;--status:#4c88d8;--status-text:#fff; }
:root.dark { --surface:#1e1e1e;--surface-raised:#2b2b2b;--input:#202020;--button:#3c3f41;--text:#d7d7d7;--muted:#9ea1a5;--border:#4d5052;--hover:#3c3f41;--selection:#264f78;--accent:#4f9dff;--changed:#5c5026;--status:#2b5b84;--status-text:#f4f4f4; }
*{box-sizing:border-box}.workbench{height:calc(100vh - 40px);min-height:630px;display:grid;grid-template-columns:220px minmax(0,1fr);grid-template-rows:40px minmax(0,1fr) auto 25px;background:var(--surface);font-family:var(--font)}.topbar{grid-column:1/-1;display:flex;gap:14px;align-items:center;padding:0 12px;color:var(--text);background:var(--surface-raised);border-bottom:1px solid var(--border);font-size:12px}.topbar span{color:var(--muted)}.topbar div{margin-left:auto}.topbar button{border:1px solid var(--border);background:var(--button);color:var(--text);cursor:pointer;padding:4px 8px;margin-left:5px;font:inherit}.tree-panel{grid-column:1;grid-row:2/4}.editor-area{grid-column:2;grid-row:2;display:grid;grid-template-rows:auto minmax(0,1fr);min-width:0}.debug-panel{grid-column:2;grid-row:3}.status{grid-column:1/-1;grid-row:4}@media(max-width:800px){.workbench{grid-template-columns:1fr}.tree-panel{display:none}.editor-area,.debug-panel{grid-column:1}.topbar{grid-column:1}.status{grid-column:1}}
</style>
