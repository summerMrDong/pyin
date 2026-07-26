<template>
  <aside class="tree-panel">
    <header>
      <div><strong>模板资源</strong><small>{{ nodes.length }} 项</small></div>
      <button title="新建空白模板" @click="$emit('blank')">＋</button>
    </header>
    <div class="tree-actions">
      <button @click="$emit('blank')">新建模板</button>
      <button @click="fileInput?.click()">导入 XLSX</button>
      <button title="新建目录" @click="$emit('folder')">新建目录</button>
    </div>
    <input ref="fileInput" class="hidden" type="file" accept=".xlsx,.json" @change="upload" />

    <div class="tree-content">
      <div v-if="!nodes.length" class="empty">
        <strong>从第一个模板开始</strong>
        <span>新建空白模板，或导入已有 XLSX 文件。</span>
        <button @click="$emit('blank')">新建模板</button>
      </div>
      <div
        v-for="node in nodes"
        :key="node.id"
        class="tree-node"
        :class="{ active: activeId === node.id, directory: node.nodeType === 'DIRECTORY' }"
        @click="open(node)"
      >
        <span class="node-icon">{{ node.nodeType === 'DIRECTORY' ? '▸' : '▦' }}</span>
        <span class="node-name">{{ node.name }}</span>
        <small v-if="node.readOnly">只读</small>
        <small v-else-if="node.sourceType">{{ sourceLabel(node.sourceType) }}</small>
        <span class="node-actions">
          <button title="重命名" @click.stop="rename(node)">✎</button>
          <button title="删除" @click.stop="$emit('delete', node.id)">×</button>
        </span>
      </div>
    </div>

    <footer>
      <span>更多来源</span>
      <button @click="$emit('mount-network')">网络模板</button>
      <button @click="$emit('mount-local')">本地目录</button>
    </footer>
  </aside>
</template>

<script setup lang="ts">
import { ref } from 'vue'
defineProps<{ nodes: any[]; activeId?: string }>()
const emit = defineEmits(['open', 'blank', 'folder', 'upload', 'rename', 'delete', 'mount-network', 'mount-local'])
const fileInput = ref<HTMLInputElement>()
const sourceLabel = (source: string) => ({ ONLINE: '在线', UPLOAD: '导入', LOCAL_DIRECTORY: '本地', NETWORK: '网络' }[source] || source)
function open(node: any) { if (node.nodeType === 'TEMPLATE') emit('open', Number(node.id.replace('template-', ''))) }
function upload(event: Event) { const input = event.target as HTMLInputElement; const file = input.files?.[0]; if (file) emit('upload', file); input.value = '' }
function rename(node: any) { const name = window.prompt('新名称', node.name)?.trim(); if (name && name !== node.name) emit('rename', node.id, name) }
</script>

<style scoped>
.tree-panel{display:flex;min-width:0;min-height:0;flex-direction:column;border-right:1px solid var(--divider);background:var(--surface-raised);color:var(--text)}header,footer{display:flex;align-items:center;justify-content:space-between;padding:10px;border-bottom:1px solid var(--divider);font-size:12px}header>div{display:flex;align-items:center;gap:7px}header small{color:var(--muted);font-weight:400}.tree-panel button{border:0;border-radius:4px;background:transparent;color:inherit;cursor:pointer;font:inherit}.tree-panel button:hover{background:var(--hover)}header>button{width:26px;height:26px;font-size:18px}.tree-actions{display:grid;grid-template-columns:1fr 1fr;padding:7px;gap:5px;border-bottom:1px solid var(--divider)}.tree-actions button{padding:6px;border:1px solid var(--border);background:var(--surface);font-size:11px}.tree-actions button:last-child{grid-column:1/-1}.tree-content{min-height:0;flex:1;overflow:auto;padding:5px}.tree-node{display:flex;align-items:center;gap:6px;min-height:34px;padding:5px 6px;border:1px solid transparent;border-radius:5px;cursor:pointer;font-size:12px}.tree-node:hover{background:var(--hover)}.tree-node.active{border-color:var(--shell-tool-selected-border,var(--accent));background:var(--selection)}.tree-node.directory{cursor:default}.node-icon{width:18px;color:var(--muted);text-align:center}.node-name{min-width:0;flex:1;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.tree-node>small{padding:2px 4px;border-radius:3px;background:var(--shell-tool-tag-bg,var(--button));color:var(--shell-tool-tag-text,var(--muted));font-size:9px}.node-actions{display:none;gap:1px}.tree-node:hover .node-actions{display:flex}.node-actions button{width:23px;height:23px;color:var(--muted)}.empty{display:grid;justify-items:center;gap:7px;margin:18px 8px;padding:18px 12px;border:1px dashed var(--border);border-radius:6px;color:var(--muted);text-align:center;font-size:11px}.empty strong{color:var(--text);font-size:12px}.empty button{margin-top:3px;padding:6px 10px;background:var(--accent);color:#fff}footer{justify-content:flex-start;gap:3px;margin-top:auto;border-top:1px solid var(--divider);border-bottom:0;color:var(--muted)}footer span{margin-right:auto}footer button{padding:5px;font-size:10px}.hidden{display:none}
</style>
