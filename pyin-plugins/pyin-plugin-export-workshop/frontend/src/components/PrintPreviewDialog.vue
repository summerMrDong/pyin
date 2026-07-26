<template>
  <div v-if="open" class="preview-backdrop" @click.self="$emit('close')">
    <section class="preview-dialog" role="dialog" aria-modal="true" aria-labelledby="preview-title">
      <header>
        <div>
          <span class="preview-kicker">运行结果</span>
          <strong id="preview-title">{{ template?.name || '未命名模板' }}</strong>
          <small>{{ changedCount ? `已填充 ${changedCount} 个单元格` : '没有变量映射，显示原模板内容' }}</small>
        </div>
        <button class="icon-button" title="关闭预览" @click="$emit('close')">×</button>
      </header>

      <div class="preview-body">
        <div v-if="snapshot" class="sheet-frame">
          <table class="preview-sheet">
            <thead><tr><th></th><th v-for="column in columns" :key="column">{{ columnName(column) }}</th></tr></thead>
            <tbody>
              <tr v-for="row in rows" :key="row">
                <th>{{ row + 1 }}</th>
                <td v-for="column in columns" :key="column" :class="{ changed: isChanged(row, column) }">
                  {{ cellValue(snapshot, row, column) }}
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <div v-else class="empty-preview">暂时没有可预览的工作簿内容。</div>
      </div>

      <footer>
        <span>预览确认无误后，可继续导出 XLSX。</span>
        <div>
          <button class="secondary-button" @click="$emit('close')">返回编辑</button>
          <a v-if="downloadUrl" class="secondary-button" :href="downloadUrl">下载 XLSX</a>
          <button class="primary-button" @click="printPreview">打印预览</button>
        </div>
      </footer>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { cellValue, columnName } from '../composables/workbook'

const props = defineProps<{ open: boolean; downloadUrl?: string; template?: any; changedCount: number; snapshot?: any; changedCells?: any[] }>()
defineEmits(['close'])

const bounds = computed(() => {
  const sheetId = props.snapshot?.sheetOrder?.[0] || 'sheet-1'
  const cellData = props.snapshot?.sheets?.[sheetId]?.cellData || {}
  let maxRow = 9
  let maxColumn = 7
  for (const [row, cells] of Object.entries(cellData)) {
    maxRow = Math.max(maxRow, Number(row))
    for (const column of Object.keys(cells as Record<string, any>)) maxColumn = Math.max(maxColumn, Number(column))
  }
  return { rows: Math.min(maxRow + 1, 30), columns: Math.min(maxColumn + 1, 16) }
})
const rows = computed(() => Array.from({ length: bounds.value.rows }, (_, index) => index))
const columns = computed(() => Array.from({ length: bounds.value.columns }, (_, index) => index))

function isChanged(row: number, column: number) {
  const address = `${columnName(column)}${row + 1}`
  return props.changedCells?.some(cell => cell.cellAddress === address)
}

const printPreview = () => window.print()
</script>

<style scoped>
.preview-backdrop{position:fixed;inset:0;z-index:80;display:grid;place-items:center;padding:24px;background:rgba(15,23,42,.58);backdrop-filter:blur(3px)}
.preview-dialog{width:min(1120px,96vw);height:min(820px,92vh);display:grid;grid-template-rows:auto minmax(0,1fr) auto;overflow:hidden;border:1px solid var(--border);border-radius:12px;background:var(--surface);color:var(--text);box-shadow:0 24px 80px rgba(2,6,23,.34)}
.preview-dialog header,.preview-dialog footer{display:flex;align-items:center;justify-content:space-between;gap:18px;padding:14px 18px;background:var(--surface-raised);border-bottom:1px solid var(--border)}
.preview-dialog header>div{display:grid;grid-template-columns:auto auto;align-items:baseline;gap:2px 10px}.preview-dialog header small{grid-column:2;color:var(--muted)}
.preview-kicker{grid-row:1/3;padding:5px 8px;border-radius:6px;background:var(--selection);color:var(--accent);font-size:11px;font-weight:700}.preview-dialog strong{font-size:16px}.preview-body{min-height:0;padding:18px;overflow:auto;background:var(--surface-raised)}
.sheet-frame{min-width:max-content;padding:18px;background:#fff;box-shadow:0 2px 12px rgba(15,23,42,.12)}.preview-sheet{border-collapse:collapse;color:#1f2937;font:12px var(--font)}.preview-sheet th,.preview-sheet td{height:27px;min-width:94px;padding:4px 7px;border:1px solid #d8dee8;text-align:left;white-space:nowrap}.preview-sheet th{min-width:38px;background:#f3f5f8;color:#64748b;text-align:center;font-weight:500}.preview-sheet td.changed{background:#fff3b0;box-shadow:inset 0 0 0 1px #d99a00}.empty-preview{display:grid;min-height:300px;place-content:center;color:var(--muted)}
.preview-dialog footer{border-top:1px solid var(--border);border-bottom:0;color:var(--muted);font-size:12px}.preview-dialog footer>div{display:flex;gap:8px}.preview-dialog button,.preview-dialog a{font:inherit}.icon-button,.secondary-button,.primary-button{border:1px solid var(--border);border-radius:6px;cursor:pointer}.icon-button{width:30px;height:30px;background:transparent;color:var(--text);font-size:20px}.secondary-button,.primary-button{padding:7px 12px;text-decoration:none}.secondary-button{background:var(--button);color:var(--text)}.primary-button{border-color:var(--accent);background:var(--accent);color:#fff}
@media(max-width:700px){.preview-backdrop{padding:8px}.preview-dialog{width:100%;height:96vh}.preview-dialog footer>span{display:none}.preview-dialog header>div{display:flex;flex-wrap:wrap}.preview-kicker{display:none}}
@media print{.preview-backdrop{position:static;padding:0;background:none}.preview-dialog{width:100%;height:auto;border:0;box-shadow:none}.preview-dialog header,.preview-dialog footer{display:none}.preview-body{padding:0;background:#fff}.sheet-frame{padding:0;box-shadow:none}}
</style>
