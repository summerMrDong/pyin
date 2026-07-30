<template>
  <section class="debug-panel" :class="{ collapsed }">
    <header>
      <strong>调试台</strong>
      <div class="panel-actions"><button :disabled="disabled" @click="$emit('export')">⇩ 导出</button><button class="run-button" :disabled="disabled" @click="$emit('run')">▶ 预览</button></div>
    </header>
    <div v-show="!collapsed" class="debug-content">
      <textarea class="json-editor" :value="modelValue" spellcheck="false" aria-label="Mock JSON" placeholder="输入 Mock JSON" @input="$emit('update:modelValue', ($event.target as HTMLTextAreaElement).value)"></textarea>
      <small class="panel-status">{{ status }}</small>
    </div>
  </section>
</template>
<script setup lang="ts">
defineProps<{ modelValue: string; collapsed: boolean; status: string; disabled?: boolean }>()
defineEmits(['update:modelValue', 'run', 'export'])
</script>
<style scoped>
.debug-panel{height:100%;display:grid;grid-template-rows:auto minmax(0,1fr);border-top:1px solid var(--divider);background:var(--surface)}.debug-panel.collapsed{grid-template-rows:auto}.debug-panel header{display:flex;align-items:center;justify-content:space-between;gap:14px;padding:7px 12px;border-bottom:1px solid var(--divider);background:var(--surface-raised);font-size:13px}.panel-actions{display:flex;gap:6px;white-space:nowrap}.panel-actions button{padding:5px 8px;border:1px solid var(--border);border-radius:5px;background:var(--button);color:var(--text);cursor:pointer;font:12px var(--font)}button:disabled{cursor:not-allowed;opacity:.45}.panel-actions .run-button{border-color:var(--accent);background:var(--accent);color:#fff;font-weight:600}.debug-content{display:grid;grid-template-rows:minmax(0,1fr) auto;gap:6px;padding:8px 12px}.json-editor{min-height:0;resize:none;border:1px solid var(--border);border-radius:5px;background:var(--input);color:var(--text);padding:8px;font:12px/1.5 var(--mono);outline:none}.json-editor:focus{border-color:var(--accent);box-shadow:0 0 0 2px var(--selection)}.panel-status{overflow:hidden;color:var(--muted);text-overflow:ellipsis;white-space:nowrap;font-size:11px}
</style>
