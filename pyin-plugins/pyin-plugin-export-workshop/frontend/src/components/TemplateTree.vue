<template>
  <aside class="tree-panel" @contextmenu.prevent>
    <header><strong>资源管理器</strong><button title="新建模板" @click="$emit('blank')">＋</button></header>
    <div class="tree-actions"><button @click="$emit('folder')">新建目录</button><button @click="fileInput?.click()">上传</button></div>
    <input ref="fileInput" class="hidden" type="file" accept=".xlsx,.json" @change="upload" />
    <p v-if="!nodes.length" class="empty">尚无模板。创建、上传或挂载一个模板开始。</p>
    <div v-for="node in nodes" :key="node.id" class="tree-node" :class="{ active: activeId === node.id }" @dblclick="open(node)" @contextmenu.prevent="menu(node)">
      <span>{{ node.nodeType === 'DIRECTORY' ? '▸' : '▦' }}</span><span class="node-name">{{ node.name }}</span><small v-if="node.readOnly">只读</small><small v-else-if="node.sourceType">{{ sourceLabel(node.sourceType) }}</small>
    </div>
    <footer><button @click="$emit('mount-network')">网络挂载</button><button @click="$emit('mount-local')">本地目录</button></footer>
  </aside>
</template>

<script setup lang="ts">
import { ref } from 'vue'
const props = defineProps<{ nodes: any[]; activeId?: string }>()
const emit = defineEmits(['open', 'blank', 'folder', 'upload', 'rename', 'delete', 'mount-network', 'mount-local'])
const fileInput = ref<HTMLInputElement>()
const sourceLabel = (source: string) => ({ ONLINE: '在线', UPLOAD: '上传', LOCAL_DIRECTORY: '目录', NETWORK: '网络' }[source] || source)
function open(node: any) { if (node.nodeType === 'TEMPLATE') emit('open', Number(node.id.replace('template-', ''))) }
function upload(event: Event) { const file = (event.target as HTMLInputElement).files?.[0]; if (file) emit('upload', file); (event.target as HTMLInputElement).value = '' }
function menu(node: any) { const action = window.prompt(`资源“${node.name}”：输入 rename、delete 或 fork`); if (action === 'rename') { const name = window.prompt('新名称', node.name); if (name) emit('rename', node.id, name) } else if (action === 'delete') emit('delete', node.id); else if (action === 'fork' && node.nodeType === 'TEMPLATE') emit('open', Number(node.id.replace('template-', ''))) }
</script>

<style scoped>
.tree-panel { display:flex; flex-direction:column; min-width:0; background:var(--surface-raised); border-right:1px solid var(--border); color:var(--text); }
header, footer { display:flex; align-items:center; justify-content:space-between; padding:9px 10px; border-bottom:1px solid var(--border); font-size:12px; }
button { color:inherit; border:0; background:transparent; cursor:pointer; font:inherit; padding:4px 6px; border-radius:3px; } button:hover { background:var(--hover); }
.tree-actions { display:flex; gap:2px; padding:6px; border-bottom:1px solid var(--border); font-size:12px; }.tree-node { display:flex; align-items:center; gap:6px; padding:5px 8px; cursor:pointer; font-size:13px; }.tree-node:hover,.tree-node.active { background:var(--selection); }.node-name { overflow:hidden; text-overflow:ellipsis; white-space:nowrap; flex:1; }small { color:var(--muted); font-size:10px; }.empty { padding:12px; color:var(--muted); font-size:12px; line-height:1.6; }footer { margin-top:auto; border-top:1px solid var(--border); border-bottom:0; gap:2px; justify-content:flex-start; }.hidden { display:none; }
</style>
