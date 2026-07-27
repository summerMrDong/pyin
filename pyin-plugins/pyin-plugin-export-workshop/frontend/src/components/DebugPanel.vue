<template>
  <section class="debug-panel" :class="{ collapsed }">
    <header>
      <div class="panel-title"><strong>模拟数据</strong><span>在模板单元格直接输入 <code v-pre>{{customer.name}}</code>，运行时自动替换。</span></div>
      <div class="panel-actions"><button class="text-button" @click="$emit('toggle')">{{ collapsed ? '展开' : '收起' }}</button><button :disabled="disabled" @click="$emit('export')">⇩ 导出 XLSX</button><button class="run-button" :disabled="disabled" @click="$emit('run')">▶ 运行预览</button></div>
    </header>
    <div v-show="!collapsed" class="debug-content">
      <label><b>Mock JSON</b><span>用于填充模板变量并验证导出结果。</span></label>
      <textarea class="json-editor" :value="modelValue" spellcheck="false" aria-label="Mock JSON" @input="$emit('update:modelValue', ($event.target as HTMLTextAreaElement).value)"></textarea>
      <small class="panel-status">{{ status }}</small>
    </div>
  </section>
</template>
<script setup lang="ts">
defineProps<{ modelValue: string; collapsed: boolean; status: string; disabled?: boolean }>()
defineEmits(['update:modelValue', 'run', 'export', 'toggle'])
</script>
<style scoped>
.debug-panel{min-height:190px;display:grid;grid-template-rows:auto minmax(0,1fr);border-top:1px solid var(--divider);background:var(--surface)}.debug-panel.collapsed{min-height:46px;grid-template-rows:auto}.debug-panel header{display:flex;align-items:center;justify-content:space-between;gap:14px;padding:8px 12px;border-bottom:1px solid var(--divider);background:var(--surface-raised)}.panel-title{display:flex;align-items:baseline;gap:9px;min-width:0}.panel-title strong{font-size:13px}.panel-title span{overflow:hidden;color:var(--muted);text-overflow:ellipsis;white-space:nowrap;font-size:11px}.panel-title code{font-family:var(--mono);color:var(--text)}.panel-actions{display:flex;gap:6px;white-space:nowrap}.panel-actions button{padding:6px 9px;border:1px solid var(--border);border-radius:5px;background:var(--button);color:var(--text);cursor:pointer;font:12px var(--font)}button:disabled{cursor:not-allowed;opacity:.45}.panel-actions .text-button{border-color:transparent;background:transparent;color:var(--muted)}.panel-actions .run-button{border-color:var(--accent);background:var(--accent);color:#fff;font-weight:600}.debug-content{display:grid;grid-template-columns:150px minmax(0,1fr);grid-template-rows:minmax(110px,1fr) auto;gap:8px;padding:10px 12px}.debug-content>label{display:grid;align-content:start;gap:4px;padding-top:4px;font-size:12px}.debug-content label span{color:var(--muted);font-size:10px}.json-editor{min-height:115px;resize:vertical;border:1px solid var(--border);border-radius:5px;background:var(--input);color:var(--text);padding:9px;font:12px/1.5 var(--mono);outline:none}.json-editor:focus{border-color:var(--accent);box-shadow:0 0 0 2px var(--selection)}.panel-status{grid-column:2;overflow:hidden;color:var(--muted);text-overflow:ellipsis;white-space:nowrap;font-size:11px}@media(max-width:700px){.panel-title span{display:none}.debug-content{grid-template-columns:1fr}.debug-content>label{display:none}.panel-status{grid-column:1}}
</style>
