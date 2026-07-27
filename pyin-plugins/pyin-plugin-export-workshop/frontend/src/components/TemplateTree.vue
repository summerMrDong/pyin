<template>
  <aside class="tree-panel" @click="closeMenus">
    <header class="resource-header">
      <div><strong>模板</strong><small>{{ templates.length }} 个</small></div>
      <div class="command-group" @click.stop>
        <button class="split-main" title="新建空白模板" @click="$emit('blank')"><span>＋</span>新建</button>
        <button class="split-more" title="新建选项" @click="createOpen = !createOpen">⌄</button>
        <div v-if="createOpen" class="command-menu">
          <button @click="choose('blank')">▦ 新建空白模板</button>
          <button @click="choose('folder')">▸ 新建目录</button>
        </div>
      </div>
      <div class="menu-anchor" @click.stop>
        <button class="icon-button" title="更多来源" @click="moreOpen = !moreOpen">···</button>
        <div v-if="moreOpen" class="command-menu menu-right">
          <button @click="fileInput?.click(); closeMenus()">⇧ 导入 XLSX / JSON</button>
          <button @click="choose('local')">▣ 挂载本地目录</button>
          <button @click="choose('network')">↗ 挂载网络模板</button>
        </div>
      </div>
    </header>
    <input ref="fileInput" class="hidden" type="file" accept=".xlsx,.json" @change="upload" />

    <div class="tree-content">
      <div v-if="!nodes.length" class="empty">
        <strong>还没有模板</strong>
        <span>新建一个空白模板，或通过右上角菜单导入已有文件。</span>
        <button @click="$emit('blank')">新建模板</button>
      </div>
      <div
        v-for="entry in visibleNodes"
        :key="entry.node.id"
        class="tree-node"
        :class="{ active: activeId === entry.node.id, directory: entry.node.nodeType === 'DIRECTORY' }"
        :style="{ paddingLeft: `${8 + entry.depth * 14}px` }"
        @click.stop="open(entry.node)"
        @contextmenu.prevent.stop="openContext(entry.node, $event)"
      >
        <span class="node-icon">{{ iconFor(entry.node) }}</span>
        <span class="node-name">{{ entry.node.name }}</span>
        <small v-if="entry.node.nodeType === 'TEMPLATE'" :title="sourceLabel(entry.node.sourceType)">{{ sourceLabel(entry.node.sourceType) }}</small>
        <span class="node-actions">
          <button title="重命名" @click.stop="rename(entry.node)">✎</button>
          <button title="删除" @click.stop="$emit('delete', entry.node.id)">×</button>
        </span>
      </div>
    </div>

    <div v-if="contextNode" class="context-menu" :style="{ left: `${contextPosition.x}px`, top: `${contextPosition.y}px` }" @click.stop>
      <button @click="rename(contextNode)">重命名</button>
      <button v-if="contextNode.readOnly" @click="fork(contextNode)">创建可编辑副本</button>
      <button class="danger" @click="$emit('delete', contextNode.id); closeMenus()">删除</button>
    </div>
  </aside>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'

const props = defineProps<{ nodes: any[]; activeId?: string }>()
const emit = defineEmits(['open', 'blank', 'folder', 'upload', 'rename', 'delete', 'mount-network', 'mount-local', 'fork'])
const fileInput = ref<HTMLInputElement>()
const createOpen = ref(false); const moreOpen = ref(false)
const contextNode = ref<any>(); const contextPosition = ref({ x: 0, y: 0 })
const expanded = ref(new Set<string>())
const templates = computed(() => props.nodes.filter(node => node.nodeType === 'TEMPLATE'))
const visibleNodes = computed(() => {
  const children = new Map<string | null, any[]>()
  for (const node of props.nodes) { const parent = node.parentId || null; children.set(parent, [...(children.get(parent) || []), node]) }
  const output: Array<{ node: any; depth: number }> = []
  const walk = (parentId: string | null, depth: number) => {
    for (const node of (children.get(parentId) || []).sort((a, b) => a.nodeType.localeCompare(b.nodeType) || a.name.localeCompare(b.name, 'zh-CN'))) {
      output.push({ node, depth })
      if (node.nodeType === 'DIRECTORY' && expanded.value.has(node.id)) walk(node.id, depth + 1)
    }
  }
  walk(null, 0); return output
})
const sourceLabel = (source?: string) => ({ ONLINE: '在线', UPLOAD: '导入', LOCAL_DIRECTORY: '本地', NETWORK: '网络' } as Record<string, string>)[source || ''] || ''
const iconFor = (node: any) => node.nodeType === 'DIRECTORY' ? (expanded.value.has(node.id) ? '⌄' : '›') : ({ ONLINE: '▦', UPLOAD: '⇧', LOCAL_DIRECTORY: '▣', NETWORK: '↗' } as Record<string, string>)[String(node.sourceType || '')] || '▦'
function closeMenus() { createOpen.value = false; moreOpen.value = false; contextNode.value = undefined }
function choose(action: string) { closeMenus(); if (action === 'blank') emit('blank'); if (action === 'folder') emit('folder'); if (action === 'local') emit('mount-local'); if (action === 'network') emit('mount-network') }
function open(node: any) { if (node.nodeType === 'DIRECTORY') { const next = new Set(expanded.value); next.has(node.id) ? next.delete(node.id) : next.add(node.id); expanded.value = next } else emit('open', Number(node.id.replace('template-', ''))) }
function upload(event: Event) { const input = event.target as HTMLInputElement; const file = input.files?.[0]; if (file) emit('upload', file); input.value = '' }
function rename(node: any) { closeMenus(); const name = window.prompt('新名称', node.name)?.trim(); if (name && name !== node.name) emit('rename', node.id, name) }
function fork(node: any) { closeMenus(); emit('fork', Number(node.id.replace('template-', ''))) }
function openContext(node: any, event: MouseEvent) { contextNode.value = node; contextPosition.value = { x: event.clientX, y: event.clientY } }
</script>

<style scoped>
.tree-panel{position:relative;display:flex;min-width:0;min-height:0;flex-direction:column;border-right:1px solid var(--divider);background:var(--surface-raised);color:var(--text)}.resource-header{display:flex;align-items:center;gap:5px;height:46px;padding:7px 8px;border-bottom:1px solid var(--divider);font-size:12px}.resource-header>div:first-child{display:flex;align-items:baseline;gap:6px;margin-right:auto}.resource-header small{color:var(--muted);font-weight:400}.tree-panel button{border:0;border-radius:4px;background:transparent;color:inherit;cursor:pointer;font:inherit}.tree-panel button:hover{background:var(--hover)}.command-group,.menu-anchor{position:relative;display:flex}.split-main,.split-more,.icon-button{height:28px;border:1px solid var(--border)!important;background:var(--surface)!important}.split-main{padding:0 7px;border-radius:4px 0 0 4px!important}.split-more{width:23px;border-left:0!important;border-radius:0 4px 4px 0!important}.icon-button{width:28px;font-weight:700}.command-menu,.context-menu{position:absolute;z-index:20;min-width:175px;padding:4px;background:var(--surface);border:1px solid var(--border);border-radius:6px;box-shadow:0 8px 24px #0002}.command-menu{top:33px;left:0}.menu-right{right:0;left:auto}.command-menu button,.context-menu button{display:block;width:100%;padding:7px 9px;text-align:left;font-size:12px}.hidden{display:none}.tree-content{min-height:0;flex:1;overflow:auto;padding:5px}.tree-node{display:flex;align-items:center;gap:6px;min-height:32px;padding-right:6px;border:1px solid transparent;border-radius:5px;cursor:pointer;font-size:12px}.tree-node:hover{background:var(--hover)}.tree-node.active{border-color:var(--shell-tool-selected-border,var(--accent));background:var(--selection)}.node-icon{width:16px;color:var(--muted);text-align:center}.node-name{min-width:0;flex:1;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.tree-node>small{padding:2px 4px;border-radius:3px;background:var(--shell-tool-tag-bg,var(--button));color:var(--shell-tool-tag-text,var(--muted));font-size:9px}.node-actions{display:none;gap:1px}.tree-node:hover .node-actions{display:flex}.node-actions button{width:22px;height:22px;color:var(--muted)}.empty{display:grid;justify-items:center;gap:7px;margin:18px 8px;padding:18px 12px;border:1px dashed var(--border);border-radius:6px;color:var(--muted);text-align:center;font-size:11px}.empty strong{color:var(--text);font-size:12px}.empty button{margin-top:3px;padding:6px 10px;background:var(--accent);color:#fff}.context-menu{position:fixed}.context-menu .danger{color:#c24141}
</style>
