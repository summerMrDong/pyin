<template>
  <section class="debug-panel" :class="{ collapsed }">
    <header>
      <div class="panel-title">
        <strong>数据绑定与运行</strong>
        <span>先选择表格单元格，再添加 JSONPath；运行后会立即打开结果预览。</span>
      </div>
      <div class="panel-actions">
        <button class="text-button" @click="$emit('toggle')">{{ collapsed ? '展开配置' : '收起' }}</button>
        <button :disabled="disabled" @click="$emit('export')">导出 XLSX</button>
        <button class="run-button" :disabled="disabled" @click="$emit('run')">运行并预览</button>
      </div>
    </header>

    <div v-show="!collapsed" class="debug-content">
      <div class="data-column">
        <label><b>1. 输入模拟数据</b><span>使用标准 JSON，用于验证模板填充结果。</span></label>
        <textarea class="json-editor" :value="modelValue" spellcheck="false" aria-label="Mock JSON" @input="$emit('update:modelValue', ($event.target as HTMLTextAreaElement).value)"></textarea>
      </div>
      <div class="mapping-panel">
        <div class="mapping-head">
          <label><b>2. 绑定模板单元格</b><span>当前选中：{{ selectedCell?.cellAddress || '请先点击上方表格单元格' }}</span></label>
          <button :disabled="!selectedCell" title="将当前单元格添加到变量映射" @click="addMapping">添加映射</button>
        </div>
        <div v-if="!mappings.length" class="mapping-empty">
          <strong>{{ automaticVariableCount ? `已自动识别 ${automaticVariableCount} 个模板变量` : '还没有变量映射' }}</strong>
          <span v-if="automaticVariableCount">模板中的 <code>{name}</code>、<code>{customer.name}</code> 会自动读取同名 JSON 字段，无需手工映射。</span>
          <span v-else>点击上方表格中的目标单元格，再点“添加映射”，填写例如 <code>$.customer.name</code>。</span>
        </div>
        <div v-else class="mapping-list">
          <label v-for="(mapping,index) in mappings" :key="`${mapping.sheetId}-${mapping.cellAddress}-${index}`" class="mapping-row">
            <span class="cell-tag">{{ mapping.cellAddress || '未选择' }}</span>
            <input v-model.trim="mapping.jsonPath" placeholder="例如 $.customer.name" />
            <button title="删除映射" @click="mappings.splice(index,1)">×</button>
          </label>
        </div>
        <small class="panel-status">{{ status }}</small>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import type { Mapping } from '../stores/workshop'
const props = defineProps<{ modelValue: string; mappings: Mapping[]; selectedCell?: { sheetId: string; cellAddress: string }; collapsed: boolean; status: string; disabled?: boolean; automaticVariableCount: number }>()
defineEmits(['update:modelValue', 'run', 'export', 'toggle'])
function addMapping() {
  if (!props.selectedCell) return
  const existing = props.mappings.find(mapping => mapping.sheetId === props.selectedCell?.sheetId && mapping.cellAddress === props.selectedCell?.cellAddress)
  if (!existing) props.mappings.push({ sheetId: props.selectedCell.sheetId, cellAddress: props.selectedCell.cellAddress, jsonPath: '' })
}
</script>

<style scoped>
.debug-panel{min-height:238px;display:grid;grid-template-rows:auto minmax(0,1fr);border-top:1px solid var(--divider);background:var(--surface)}.debug-panel.collapsed{min-height:48px;grid-template-rows:auto}.debug-panel header{display:flex;align-items:center;justify-content:space-between;gap:14px;padding:8px 12px;border-bottom:1px solid var(--divider);background:var(--surface-raised)}.panel-title{display:grid;gap:2px}.panel-title strong{font-size:13px}.panel-title span{font-size:11px;color:var(--muted)}.panel-actions{display:flex;gap:6px}.panel-actions button,.mapping-head button,.mapping-row button{padding:6px 9px;border:1px solid var(--border);border-radius:5px;background:var(--button);color:var(--text);cursor:pointer;font:12px var(--font)}button:disabled{cursor:not-allowed;opacity:.45}.panel-actions .text-button{border-color:transparent;background:transparent;color:var(--muted)}.panel-actions .run-button{border-color:var(--accent);background:var(--accent);color:#fff;font-weight:600}
.debug-content{display:grid;grid-template-columns:minmax(320px,1fr) minmax(360px,1.15fr);min-height:190px}.data-column,.mapping-panel{display:grid;grid-template-rows:auto minmax(0,1fr);gap:8px;padding:10px 12px;min-width:0}.data-column{border-right:1px solid var(--divider)}.data-column>label,.mapping-head label{display:grid;gap:2px;font-size:12px}.data-column label span,.mapping-head label span{font-size:10px;color:var(--muted)}.json-editor{min-height:136px;resize:vertical;border:1px solid var(--border);border-radius:5px;background:var(--input);color:var(--text);padding:9px;font:12px/1.5 var(--mono);outline:none}.json-editor:focus,.mapping-row input:focus{border-color:var(--accent);box-shadow:0 0 0 2px var(--selection)}.mapping-panel{grid-template-rows:auto minmax(0,1fr) auto}.mapping-head{display:flex;align-items:center;justify-content:space-between;gap:10px}.mapping-empty{display:grid;align-content:center;justify-items:center;gap:5px;padding:16px;border:1px dashed var(--border);border-radius:6px;color:var(--muted);text-align:center;font-size:11px}.mapping-empty strong{color:var(--text);font-size:12px}.mapping-empty code{color:var(--shell-tool-code-text,var(--text));font-family:var(--mono)}.mapping-list{overflow:auto}.mapping-row{display:grid;grid-template-columns:64px minmax(0,1fr) 30px;gap:6px;align-items:center;margin-bottom:7px}.cell-tag{padding:5px;border:1px solid var(--shell-tool-tag-border,var(--border));border-radius:4px;background:var(--shell-tool-tag-bg,var(--selection));color:var(--shell-tool-tag-text,var(--text));text-align:center;font:11px var(--mono)}.mapping-row input{min-width:0;padding:6px 8px;border:1px solid var(--border);border-radius:5px;background:var(--input);color:var(--text);font:12px var(--mono);outline:none}.mapping-row button{padding:4px;font-size:17px;line-height:1}.panel-status{overflow:hidden;color:var(--muted);text-overflow:ellipsis;white-space:nowrap}
@media(max-width:850px){.debug-content{grid-template-columns:1fr}.data-column{border-right:0;border-bottom:1px solid var(--divider)}}
</style>
