<template><nav class="editor-tabs"><button v-for="tab in tabs" :key="tab.id" :class="{ active: tab.id === activeId }" @click="$emit('select', tab.id)" @dblclick="rename(tab)"><span>{{ tab.readOnly ? '◌' : '▦' }}</span>{{ tab.name }}<i @click.stop="$emit('close', tab.id)">×</i></button></nav></template>
<script setup lang="ts">
defineProps<{ tabs: any[]; activeId?: number }>(); const emit = defineEmits(['select', 'close', 'rename'])
function rename(tab: any) { const name = window.prompt('模板名称', tab.name); if (name) emit('rename', `template-${tab.id}`, name) }
</script>
<style scoped>.editor-tabs{display:flex;background:var(--surface-raised);border-bottom:1px solid var(--border);overflow:auto}.editor-tabs button{min-width:130px;max-width:230px;border:0;border-right:1px solid var(--border);background:transparent;color:var(--muted);padding:9px 10px;text-align:left;white-space:nowrap;text-overflow:ellipsis;overflow:hidden;cursor:pointer;font:12px var(--font)}.editor-tabs button.active{color:var(--text);background:var(--surface);border-top:2px solid var(--accent);padding-top:7px}.editor-tabs i{font-style:normal;float:right;color:var(--muted);font-size:15px}</style>
