<template>
  <main class="designer">
    <header class="toolbar">
      <div class="title"><span class="dot"></span><strong>{{ definition.machineName || '订单状态机' }}</strong><small v-if="saved">已保存</small></div>
      <div class="toolbar-actions">
        <el-button size="small" @click="addState('NORMAL')">＋ 状态</el-button>
        <el-button size="small" :disabled="!selected" @click="removeSelected">删除</el-button>
        <el-button size="small" @click="fitGraph">适配画布</el-button>
        <el-button size="small" :loading="saving" @click="save">保存</el-button>
        <el-button type="primary" size="small" :loading="publishing" @click="publish">发布 v{{ publishedVersion + 1 }}</el-button>
      </div>
    </header>

    <section class="workspace">
      <aside class="palette panel">
        <h3>组件库</h3>
        <p>拖入或点击添加状态，再从节点端点拖出连线。</p>
        <button class="palette-item initial" @click="addState('INITIAL')"><i></i>初始状态</button>
        <button class="palette-item normal" @click="addState('NORMAL')"><i></i>普通状态</button>
        <button class="palette-item final" @click="addState('FINAL')"><i></i>最终状态</button>
        <div class="legend"><b>连线</b><span>选中连线后可编辑事件、条件和动作。</span></div>
      </aside>

      <section class="canvas-shell panel">
        <div class="canvas-heading"><span>订单状态机</span><small>草稿 · 已发布 v{{ publishedVersion }}</small></div>
        <div ref="graphContainer" class="graph"></div>
        <div class="canvas-tip">拖动节点调整布局；从节点边缘拖至另一节点创建迁移。</div>
      </section>

      <aside class="inspector panel">
        <h3>属性</h3>
        <template v-if="selected?.kind === 'node'">
          <el-form label-position="top" size="small">
            <el-form-item label="ID"><el-input v-model="selected.data.id" disabled /></el-form-item>
            <el-form-item label="名称"><el-input v-model="selected.data.name" @change="syncSelection" /></el-form-item>
            <el-form-item label="类型"><el-select v-model="selected.data.type" @change="syncSelection"><el-option label="初始状态" value="INITIAL" /><el-option label="普通状态" value="NORMAL" /><el-option label="最终状态" value="FINAL" /></el-select></el-form-item>
            <el-form-item label="描述"><el-input v-model="selected.data.description" type="textarea" :rows="3" @change="syncSelection" /></el-form-item>
          </el-form>
        </template>
        <template v-else-if="selected?.kind === 'edge'">
          <el-form label-position="top" size="small">
            <el-form-item label="事件名称"><el-input v-model="selected.data.eventName" placeholder="例如：支付成功" @change="syncSelection" /></el-form-item>
            <el-form-item label="事件编码"><el-input v-model="selected.data.eventCode" placeholder="例如：PAY_SUCCESS" @change="syncSelection" /></el-form-item>
            <el-form-item label="条件"><el-input v-model="selected.data.condition" placeholder="例如：未确认收货" @change="syncSelection" /></el-form-item>
            <el-form-item label="动作"><el-input v-model="selected.data.actions" type="textarea" :rows="3" placeholder="例如：发送支付成功通知" @change="syncSelection" /></el-form-item>
          </el-form>
        </template>
        <el-empty v-else description="选择状态或连线编辑属性" :image-size="70" />
      </aside>
    </section>

    <section class="debug panel">
      <div class="debug-heading"><div><strong>调试面板</strong><small>当前状态：<b>{{ debug.currentStateName || '—' }}</b></small></div><div><el-button size="small" @click="resetDebug">重置会话</el-button><el-button size="small" type="success" :disabled="!debug.availableEvents?.length" @click="runFirstEvent">执行事件</el-button></div></div>
      <div class="debug-content">
        <div class="event-controls"><span>可用事件</span><el-select v-model="selectedEvent" size="small" placeholder="选择事件"><el-option v-for="event in debug.availableEvents" :key="event.eventCode" :label="event.eventName" :value="event.eventCode" /></el-select><el-button size="small" type="primary" :disabled="!selectedEvent" @click="dispatchEvent">执行</el-button><p v-if="selectedEventDetail?.condition !== '—'">条件：{{ selectedEventDetail?.condition }}</p></div>
        <el-table :data="debug.logs || []" size="small" height="184" class="trace-table"><el-table-column prop="eventName" label="事件" min-width="125" /><el-table-column prop="sourceState" label="来源状态" min-width="105" /><el-table-column prop="targetState" label="目标状态" min-width="105" /><el-table-column prop="condition" label="条件" min-width="130" /><el-table-column prop="actions" label="动作" min-width="160" /><el-table-column prop="result" label="结果" width="78"><template #default="{ row }"><span class="success">{{ row.result === 'SUCCESS' ? '成功' : row.result }}</span></template></el-table-column><el-table-column prop="createdAt" label="时间" width="168"><template #default="{ row }">{{ formatTime(row.createdAt) }}</template></el-table-column></el-table>
      </div>
    </section>
  </main>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { Graph } from '@antv/x6'
import { ElMessage } from 'element-plus'
import { dispatchDebugEvent, fetchWorkspace, publishDefinition, resetDebug as resetDebugApi, saveDefinition } from '../api/stateMachineApi'

const graphContainer = ref()
const graph = ref()
const definition = reactive({ machineKey: 'order', machineName: '订单状态机', nodes: [], transitions: [] })
const debug = reactive({ currentStateId: '', currentStateName: '', availableEvents: [], logs: [] })
const publishedVersion = ref(0), selected = ref(), selectedEvent = ref(''), saving = ref(false), publishing = ref(false), saved = ref(false)
const selectedEventDetail = computed(() => debug.availableEvents?.find(item => item.eventCode === selectedEvent.value))

onMounted(async () => { createGraph(); await loadWorkspace() })
onBeforeUnmount(() => graph.value?.dispose())

function createGraph() {
  graph.value = new Graph({
    container: graphContainer.value,
    background: { color: '#101827' },
    grid: { visible: true, size: 16, type: 'mesh', args: { color: '#26364e', thickness: 1 } },
    panning: true,
    mousewheel: { enabled: true, modifiers: ['ctrl', 'meta'] },
    connecting: { allowBlank: false, allowLoop: false, allowNode: false, snap: true, highlight: true, createEdge: () => ({ attrs: edgeAttrs(), data: newTransition() }) }
  })
  graph.value.on('node:click', ({ node }) => selectNode(node))
  graph.value.on('edge:click', ({ edge }) => selectEdge(edge))
  graph.value.on('blank:click', () => { selected.value = undefined })
  graph.value.on('edge:connected', ({ edge }) => { if (!edge.getData()?.id) { edge.setData({ ...newTransition(), id: edge.id }); edge.setLabels(edgeLabels(edge.getData())) }; selectEdge(edge); saved.value = false })
}

async function loadWorkspace() {
  try { applyWorkspace(await fetchWorkspace()) } catch (error) { ElMessage.error(error.message || '加载状态机失败') }
}

function applyWorkspace(workspace) {
  Object.assign(definition, workspace.definition)
  publishedVersion.value = workspace.publishedVersion || 0
  Object.assign(debug, workspace.debug || {})
  selectedEvent.value = debug.availableEvents?.[0]?.eventCode || ''
  renderGraph()
}

function renderGraph() {
  if (!graph.value) return
  graph.value.clearCells()
  definition.nodes.forEach(item => graph.value.addNode({ id: item.id, shape: item.type === 'INITIAL' ? 'circle' : 'rect', x: item.x ?? 120, y: item.y ?? 100, width: item.type === 'INITIAL' ? 42 : 146, height: item.type === 'INITIAL' ? 42 : 66, attrs: nodeAttrs(item), data: { ...item } }))
  definition.transitions.forEach(item => graph.value.addEdge({ id: item.id, source: item.source, target: item.target, attrs: edgeAttrs(), labels: edgeLabels(item), data: { ...item }, connector: { name: 'rounded' } }))
  refreshStateHighlight()
  nextTick(fitGraph)
}

function nodeAttrs(node) {
  const terminal = node.type === 'FINAL'
  const initial = node.type === 'INITIAL'
  return { body: { rx: initial ? 22 : 8, ry: initial ? 22 : 8, fill: initial ? '#31b56a' : terminal ? '#923d37' : '#1d4f9b', stroke: initial ? '#95f3bd' : terminal ? '#f0a39c' : '#79aaff', strokeWidth: 1.5 }, label: { text: initial ? '' : `${node.name}\n${node.id}`, fill: '#f8fbff', fontSize: 13, fontWeight: 600, textWrap: { width: -18, height: -16 } } }
}

function edgeAttrs() { return { line: { stroke: '#d2d9e5', strokeWidth: 1.6, targetMarker: { name: 'classic', size: 7 } } } }
function edgeLabels(edge) { const text = edge.eventName || edge.eventCode || ''; return text ? [{ attrs: { label: { text, fill: '#f1f5fb', fontSize: 12 }, body: { fill: '#101827', stroke: 'none' } }, position: 0.5 }] : [] }
function newTransition() { return { id: '', eventCode: '', eventName: '', condition: '', actions: '' } }

function selectNode(node) { selected.value = { kind: 'node', cell: node, data: { ...node.getData() } } }
function selectEdge(edge) { selected.value = { kind: 'edge', cell: edge, data: { ...newTransition(), ...edge.getData(), id: edge.id } } }

function syncSelection() {
  if (!selected.value) return
  const { cell, data, kind } = selected.value
  cell.setData({ ...data })
  if (kind === 'node') cell.attr(nodeAttrs(data))
  else cell.setLabels(edgeLabels(data))
  saved.value = false
}

function addState(type) {
  if (type === 'INITIAL' && graph.value.getNodes().some(node => node.getData().type === 'INITIAL')) return ElMessage.warning('一个状态机只能有一个初始状态')
  const number = graph.value.getNodes().length + 1
  const id = type === 'INITIAL' ? 'state.start' : `state.${Date.now()}`
  const data = { id, name: type === 'INITIAL' ? '初始状态' : type === 'FINAL' ? '结束状态' : `状态 ${number}`, type, description: '', x: 170 + (number % 4) * 165, y: 80 + Math.floor(number / 4) * 125 }
  const node = graph.value.addNode({ id: data.id, shape: type === 'INITIAL' ? 'circle' : 'rect', x: data.x, y: data.y, width: type === 'INITIAL' ? 42 : 146, height: type === 'INITIAL' ? 42 : 66, attrs: nodeAttrs(data), data })
  selectNode(node); saved.value = false
}

function removeSelected() {
  if (!selected.value) return
  graph.value.removeCell(selected.value.cell)
  selected.value = undefined; saved.value = false
}

function graphDefinition() {
  const nodes = graph.value.getNodes().map(node => ({ ...node.getData(), x: Math.round(node.position().x), y: Math.round(node.position().y) }))
  const transitions = graph.value.getEdges().map(edge => ({ ...newTransition(), ...edge.getData(), id: edge.id, source: edge.getSourceCellId(), target: edge.getTargetCellId() }))
  return { machineKey: 'order', machineName: definition.machineName, nodes, transitions }
}

async function save() {
  saving.value = true
  try { applyWorkspace(await saveDefinition(graphDefinition())); saved.value = true; ElMessage.success('状态机草稿已保存') } catch (error) { ElMessage.error(error.message) } finally { saving.value = false }
}

async function publish() {
  if (!saved.value) await save()
  publishing.value = true
  try { const response = await publishDefinition(); applyWorkspace(response); ElMessage.success(response.message || '已发布') } catch (error) { ElMessage.error(error.message) } finally { publishing.value = false }
}

async function resetDebug() { try { Object.assign(debug, await resetDebugApi()); selectedEvent.value = debug.availableEvents?.[0]?.eventCode || ''; refreshStateHighlight(); ElMessage.success('调试会话已重置') } catch (error) { ElMessage.error(error.message) } }
async function dispatchEvent() { if (!selectedEvent.value) return; try { Object.assign(debug, await dispatchDebugEvent(selectedEvent.value)); selectedEvent.value = debug.availableEvents?.[0]?.eventCode || ''; refreshStateHighlight(); ElMessage.success('事件执行成功') } catch (error) { ElMessage.error(error.message) } }
function runFirstEvent() { selectedEvent.value = selectedEvent.value || debug.availableEvents?.[0]?.eventCode; dispatchEvent() }
function refreshStateHighlight() { if (!graph.value) return; graph.value.getNodes().forEach(node => { const data = node.getData(); const attrs = nodeAttrs(data); if (data.id === debug.currentStateId) { attrs.body.stroke = '#39e18a'; attrs.body.strokeWidth = 3 }; node.attr(attrs) }) }
function fitGraph() { graph.value?.zoomToFit({ padding: 54, maxScale: 1 }) }
function formatTime(value) { if (!value) return '—'; const date = new Date(value); return Number.isNaN(date.getTime()) ? value : date.toLocaleString('zh-CN', { hour12: false }).replaceAll('/', '-') }
</script>

<style scoped>
.designer{--bg:#0c1421;--panel:#121d2d;--line:#26364e;--muted:#93a4bc;--text:#edf4ff;--accent:#18c7bd;display:grid;grid-template-rows:50px minmax(520px,1fr) 244px;min-height:calc(100vh - 40px);background:var(--bg);color:var(--text);font-family:var(--shell-font-sans,"Segoe UI","PingFang SC",sans-serif)}.toolbar,.debug-heading{display:flex;align-items:center;justify-content:space-between;padding:0 16px;border-bottom:1px solid var(--line);background:#111b2a}.title{display:flex;align-items:center;gap:9px;font-size:14px}.title small{color:#35d49c;font-size:12px}.dot{width:9px;height:9px;border:2px solid var(--accent);border-radius:50%}.toolbar-actions{display:flex;gap:8px}.toolbar :deep(.el-button),.debug :deep(.el-button){--el-button-bg-color:#17253a;--el-button-border-color:#30445f;--el-button-text-color:#d8e5f5}.toolbar :deep(.el-button--primary),.debug :deep(.el-button--primary){--el-button-bg-color:#148f92;--el-button-border-color:#18c7bd;--el-button-text-color:#fff}.workspace{display:grid;grid-template-columns:205px minmax(530px,1fr) 275px;gap:10px;padding:10px;min-height:0}.panel{border:1px solid var(--line);border-radius:6px;background:var(--panel);box-shadow:0 8px 24px rgba(0,0,0,.15)}.palette,.inspector{padding:14px}.palette h3,.inspector h3{margin:0 0 16px;font-size:14px}.palette p{margin:0 0 18px;color:var(--muted);font-size:12px;line-height:1.6}.palette-item{display:flex;align-items:center;gap:10px;width:100%;margin-bottom:9px;padding:10px;border:1px solid #314561;border-radius:5px;background:#17253a;color:var(--text);text-align:left;cursor:pointer}.palette-item:hover{border-color:var(--accent);background:#1a3042}.palette-item i{display:block;width:17px;height:17px;border:2px solid #62a3ff;border-radius:4px}.palette-item.initial i{border-radius:50%;border-color:#59df8c}.palette-item.final i{border-color:#f4726b}.legend{display:grid;gap:6px;margin-top:23px;padding-top:15px;border-top:1px solid var(--line);color:var(--muted);font-size:12px;line-height:1.5}.legend b{color:var(--text)}.canvas-shell{position:relative;display:grid;grid-template-rows:38px minmax(0,1fr);overflow:hidden}.canvas-heading{display:flex;justify-content:space-between;align-items:center;padding:0 13px;border-bottom:1px solid var(--line);font-size:13px}.canvas-heading small{color:var(--muted)}.graph{min-height:0;overflow:hidden}.canvas-tip{position:absolute;bottom:11px;left:12px;padding:5px 8px;border-radius:4px;background:rgba(7,13,23,.72);color:#97a7bd;font-size:11px;pointer-events:none}.inspector :deep(.el-form-item){margin-bottom:13px}.inspector :deep(.el-form-item__label){padding-bottom:4px;color:#aebdd0;font-size:12px}.inspector :deep(.el-input__wrapper),.inspector :deep(.el-textarea__inner){background:#0e1827;box-shadow:0 0 0 1px #30445f inset;color:#eef6ff}.inspector :deep(.el-select){width:100%}.debug{margin:0 10px 10px;overflow:hidden}.debug-heading{height:47px;padding:0 14px}.debug-heading>div{display:flex;align-items:center;gap:12px}.debug-heading small{color:var(--muted);font-size:12px}.debug-heading b{color:#44d39b}.debug-content{display:grid;grid-template-columns:270px minmax(0,1fr);height:196px}.event-controls{padding:16px;border-right:1px solid var(--line)}.event-controls>span{display:block;margin-bottom:9px;color:var(--muted);font-size:12px}.event-controls :deep(.el-select){width:100%;margin-bottom:8px}.event-controls :deep(.el-input__wrapper){background:#0e1827;box-shadow:0 0 0 1px #30445f inset}.event-controls p{margin:10px 0;color:#f3c777;font-size:12px}.trace-table{--el-table-border-color:#26364e;--el-table-header-bg-color:#17253a;--el-table-tr-bg-color:#121d2d;--el-table-row-hover-bg-color:#172c3c;--el-table-text-color:#dce8f6;--el-table-header-text-color:#aebdd0;font-size:12px}.trace-table :deep(.el-table__inner-wrapper:before){background:#26364e}.success{color:#45d49a}@media(max-width:1100px){.workspace{grid-template-columns:185px minmax(430px,1fr)}.inspector{display:none}}@media(max-width:800px){.designer{min-width:800px}.workspace{grid-template-columns:185px minmax(600px,1fr)}.debug-content{grid-template-columns:235px minmax(0,1fr)}}
</style>
