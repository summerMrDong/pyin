<template>
  <section class="univer-wrapper">
    <div v-if="!snapshot" class="welcome"><h2>导出工坊</h2><p>从左侧创建、上传或挂载一个 XLSX 模板。</p></div>
    <template v-else>
      <div class="formula-bar"><span>fx</span><input :value="selectedValue" @input="updateSelected(($event.target as HTMLInputElement).value)" /><small>{{ selectedAddress }}</small></div>
      <div class="grid-scroll"><table class="sheet"><thead><tr><th></th><th v-for="column in columns" :key="column">{{ columnName(column) }}</th></tr></thead><tbody><tr v-for="row in rows" :key="row"><th>{{ row + 1 }}</th><td v-for="column in columns" :key="column" :class="{ selected: selection.row === row && selection.column === column, changed: isChanged(row, column) }" @click="select(row, column)"><input :value="value(row, column)" @input="setValue(row, column, ($event.target as HTMLInputElement).value)" /></td></tr></tbody></table></div>
      <div class="sheet-footer"><span>Sheet1</span><span>Univer 工作簿实例：{{ instanceState }}</span></div>
    </template>
  </section>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { cloneWorkbook, columnName, cellValue, setCellValue } from '../composables/workbook'

const props = defineProps<{ snapshot?: any; changedCells: any[] }>()
const emit = defineEmits(['update:snapshot', 'cell-select'])
const workbook = ref<any>(); const selection = ref({ row: 0, column: 0 }); const instanceState = ref('未加载')
let univer: { dispose?: () => void } | undefined
const rows = Array.from({ length: 40 }, (_, index) => index); const columns = Array.from({ length: 16 }, (_, index) => index)
const selectedAddress = computed(() => `${columnName(selection.value.column)}${selection.value.row + 1}`)
const selectedValue = computed(() => workbook.value ? cellValue(workbook.value, selection.value.row, selection.value.column) : '')

function dispose() { univer?.dispose?.(); univer = undefined; instanceState.value = '已释放' }
function create(snapshot: any) {
  dispose(); workbook.value = cloneWorkbook(snapshot)
  // The Vue shell remains independent from Univer's React UI peer dependency.  When the
  // platform preloads the locked Univer runtime, it can provide this narrow adapter.
  const runtime = (globalThis as any).__PYIN_UNIVER_RUNTIME__
  const createWorkbook = runtime?.createWorkbook as undefined | ((data: any) => { dispose?: () => void })
  if (createWorkbook) {
    univer = createWorkbook(workbook.value)
    instanceState.value = '已加载'
  } else {
    instanceState.value = '数据模型模式'
  }
}
function select(row: number, column: number) { selection.value = { row, column }; emit('cell-select', { sheetId: 'sheet-1', cellAddress: `${columnName(column)}${row + 1}` }) }
function value(row: number, column: number) { return workbook.value ? cellValue(workbook.value, row, column) : '' }
function setValue(row: number, column: number, value: string) { if (!workbook.value) return; setCellValue(workbook.value, row, column, value); emit('update:snapshot', cloneWorkbook(workbook.value)) }
function updateSelected(value: string) { setValue(selection.value.row, selection.value.column, value) }
function isChanged(row: number, column: number) { const address = `${columnName(column)}${row + 1}`; return props.changedCells.some(cell => cell.cellAddress === address) }
watch(() => props.snapshot, snapshot => { if (snapshot) create(snapshot); else { dispose(); workbook.value = undefined } }, { immediate: true, deep: false })
onBeforeUnmount(dispose)
</script>

<style scoped>
.univer-wrapper{min-width:0;min-height:0;display:grid;grid-template-rows:auto 1fr auto;background:var(--surface);color:var(--text)}.welcome{display:grid;place-content:center;text-align:center;color:var(--muted)}.welcome h2{color:var(--text);font:600 22px var(--font)}.formula-bar{display:flex;align-items:center;gap:9px;padding:6px 10px;border-bottom:1px solid var(--border);background:var(--surface-raised);font:12px var(--font)}.formula-bar input{flex:1;border:1px solid var(--border);background:var(--input);color:var(--text);padding:5px 8px;outline:none}.formula-bar small{width:50px;color:var(--muted)}.grid-scroll{overflow:auto}.sheet{border-collapse:collapse;font:12px var(--font);min-width:100%}.sheet th{position:sticky;top:0;background:var(--surface-raised);color:var(--muted);font-weight:400;z-index:1}.sheet th:first-child{left:0;z-index:2}.sheet th,.sheet td{border:1px solid var(--border);height:24px;min-width:90px;padding:0}.sheet th{min-width:40px}.sheet tbody th{position:sticky;left:0;z-index:1}.sheet td{background:var(--surface)}.sheet td.selected{outline:2px solid var(--accent);outline-offset:-2px}.sheet td.changed{background:var(--changed)}.sheet input{border:0;background:transparent;color:var(--text);width:100%;height:100%;padding:3px 5px;font:inherit;outline:none}.sheet-footer{display:flex;justify-content:space-between;border-top:1px solid var(--border);padding:5px 10px;background:var(--surface-raised);color:var(--muted);font:11px var(--font)}
</style>
