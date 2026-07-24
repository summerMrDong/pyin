<template><footer class="status"><span>导出工坊</span><span v-if="template">{{ template.name }} · Sheet1</span><span class="spacer"></span><span>{{ message || '就绪' }}</span><button @click="$emit('theme')">{{ dark ? '☀ 浅色' : '◐ 深色' }}</button></footer></template>
<script setup lang="ts">
defineProps<{ template?: any; message?: string; dark: boolean }>()
defineEmits(['theme'])
</script>
<style scoped>.status{display:flex;gap:14px;align-items:center;background:var(--status);color:var(--status-text);padding:4px 9px;font:12px var(--font)}.spacer{flex:1}.status button{border:0;background:transparent;color:inherit;cursor:pointer;font:inherit}</style>
