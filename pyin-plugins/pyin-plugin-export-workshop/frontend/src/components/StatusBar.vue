<template>
  <footer class="status">
    <span class="status-dot"></span>
    <span>{{ message || '就绪' }}</span>
    <span class="spacer"></span>
    <span v-if="template">{{ template.name }} · Sheet1</span>
    <span v-if="variableCount">{{ variableCount }} 个模板变量</span>
    <span v-if="changedCount">{{ changedCount }} 个已填充单元格</span>
  </footer>
</template>
<script setup lang="ts">
defineProps<{ template?: any; message?: string; variableCount: number; changedCount: number }>()
</script>
<style scoped>
.status{display:flex;align-items:center;gap:10px;padding:4px 9px;border-top:1px solid var(--divider);background:var(--surface-raised);color:var(--muted);font:11px var(--font)}.status-dot{width:7px;height:7px;border-radius:50%;background:var(--accent)}.spacer{flex:1}
</style>
