<template>
  <aside class="tree-panel" @contextmenu="openRootContext">
    <header class="resource-header">
      <div><strong>资源</strong><small>{{ templates.length }}</small></div>
      <small class="context-hint">右键管理</small>
    </header>

    <div class="tree-content" @contextmenu="openRootContext">
      <div v-if="!nodes.length" class="empty">
        <strong>还没有模板</strong>
        <span>在此区域右键创建文件、目录或导入模板。</span>
      </div>
      <el-tree
        ref="treeRef"
        v-else
        class="resource-tree"
        :data="treeNodes"
        node-key="id"
        :props="treeProps"
        :current-node-key="activeId"
        default-expand-all
        highlight-current
        :expand-on-click-node="true"
        @node-click="open"
        @node-contextmenu="openNodeContext"
      >
        <template #default="{ data }">
          <span class="tree-node-content">
            <el-icon class="node-icon" :class="data.nodeType === 'DIRECTORY' ? 'directory-icon' : 'template-icon'">
              <Folder v-if="data.nodeType === 'DIRECTORY'" />
              <Document v-else />
            </el-icon>
            <span class="node-name">{{ data.name }}</span>
            <small v-if="data.nodeType === 'TEMPLATE'" :title="sourceLabel(data.sourceType)">{{ sourceLabel(data.sourceType) }}</small>
          </span>
        </template>
      </el-tree>
    </div>
  </aside>

</template>

<script setup lang="ts">
import { Document, Folder } from '@element-plus/icons-vue'
import ContextMenu, { type MenuItem } from '@imengyu/vue3-context-menu'
import '@imengyu/vue3-context-menu/lib/vue3-context-menu.css'
import { computed, ref } from 'vue'

type ResourceNode = {
  id: string
  name: string
  nodeType: 'DIRECTORY' | 'TEMPLATE'
  parentId?: string | null
  sourceType?: string
  readOnly?: boolean
  children?: ResourceNode[]
}

const props = defineProps<{ nodes: ResourceNode[]; activeId?: string }>()
const emit = defineEmits(['open', 'create-online', 'create-import', 'create-network', 'create-directory', 'folder', 'rename', 'delete', 'fork', 'download', 'copy-download-link'])
const treeRef = ref<{ setCurrentKey: (key?: string) => void }>()
const treeProps = { children: 'children', label: 'name' }
const templates = computed(() => props.nodes.filter(node => node.nodeType === 'TEMPLATE'))
const treeNodes = computed<ResourceNode[]>(() => {
  const byId = new Map<string, ResourceNode>()
  const roots: ResourceNode[] = []
  for (const source of props.nodes) byId.set(source.id, { ...source, children: [] })
  for (const node of byId.values()) {
    const parent = node.parentId ? byId.get(node.parentId) : undefined
    if (parent?.nodeType === 'DIRECTORY') parent.children?.push(node)
    else roots.push(node)
  }
  const sortNodes = (items: ResourceNode[]) => {
    items.sort((left, right) => left.nodeType.localeCompare(right.nodeType) || left.name.localeCompare(right.name, 'zh-CN'))
    items.forEach(node => sortNodes(node.children || []))
  }
  sortNodes(roots)
  return roots
})
const sourceLabel = (source?: string) => ({ ONLINE: '在线', UPLOAD: '导入', LOCAL_DIRECTORY: '本地', NETWORK: '网络' } as Record<string, string>)[source || ''] || ''

function open(node: ResourceNode) {
  if (node.nodeType === 'TEMPLATE') emit('open', Number(node.id.replace('template-', '')))
}
function parentDirectoryId(target?: ResourceNode) {
  if (!target) return undefined
  return Number(String(target.nodeType === 'DIRECTORY' ? target.id : target.parentId || '').replace('directory-', '')) || undefined
}
function createBlank(target?: ResourceNode) { emit('create-online', parentDirectoryId(target)) }
function createFolder(target?: ResourceNode) { emit('folder', parentDirectoryId(target)) }
function rename(node: ResourceNode) {
  emit('rename', node.id, node.name)
}
function menuItemsFor(node?: ResourceNode): MenuItem[] {
  const createItems = (target?: ResourceNode): MenuItem[] => [
    { label: '在线模板', onClick: () => emit('create-online', parentDirectoryId(target)) },
    { label: '导入文件', onClick: () => emit('create-import', parentDirectoryId(target)) },
    { label: '网络模板', onClick: () => emit('create-network', parentDirectoryId(target)) },
    { label: '本地目录', onClick: () => emit('create-directory', parentDirectoryId(target)) },
    { label: '新建目录', divided: 'up', onClick: () => createFolder(target) }
  ]
  if (!node) return [
    { label: '创建', children: createItems() }
  ]
  if (node.nodeType === 'DIRECTORY') return [
    { label: '创建', children: createItems(node) },
    { label: '重命名', divided: 'up', onClick: () => rename(node) },
    { label: '删除', divided: 'up', customClass: 'danger', onClick: () => emit('delete', node.id) }
  ]
  return [
    { label: '重命名', onClick: () => rename(node) },
    { label: '下载模板', onClick: () => emit('download', node) },
    { label: '复制链接', onClick: () => emit('copy-download-link', node) },
    ...(node.readOnly ? [{ label: '创建可编辑副本', onClick: () => emit('fork', Number(node.id.replace('template-', ''))) }] : []),
    { label: '删除', divided: 'up', customClass: 'danger', onClick: () => emit('delete', node.id) }
  ]
}
function showContext(node: ResourceNode | undefined, event: MouseEvent) {
  event.preventDefault()
  event.stopPropagation()
  ContextMenu.showContextMenu({
    x: event.clientX,
    y: event.clientY,
    items: menuItemsFor(node),
    theme: 'flat',
    customClass: 'export-workshop-context-menu',
    minWidth: 188,
    zIndex: 3000,
    adjustPosition: true
  })
}
function openNodeContext(event: Event, node: ResourceNode) {
  treeRef.value?.setCurrentKey(node.id)
  showContext(node, event as MouseEvent)
}
function openRootContext(event: MouseEvent) {
  if ((event.target as HTMLElement).closest('.el-tree-node__content')) return
  showContext(undefined, event)
}
</script>

<style scoped>
.tree-panel{position:relative;display:flex;width:100%;height:100%;min-width:0;min-height:0;flex-direction:column;border-right:1px solid var(--divider);background:var(--surface-raised);color:var(--text)}.resource-header{display:flex;align-items:center;gap:5px;height:38px;padding:6px 9px;border-bottom:1px solid var(--divider);font-size:12px}.resource-header>div:first-child{display:flex;align-items:baseline;gap:6px;margin-right:auto}.resource-header small{color:var(--muted);font-weight:400}.context-hint{font-size:10px}.tree-content{min-height:0;flex:1;overflow:auto;padding:5px}.resource-tree{--el-tree-node-hover-bg-color:var(--hover);--el-tree-text-color:var(--text);--el-tree-expand-icon-color:var(--muted);background:transparent;color:var(--text);font:12px var(--font)}.tree-node-content{display:flex;min-width:0;flex:1;align-items:center;gap:7px;padding-right:6px}.node-icon{width:16px;height:16px;color:var(--muted)}.directory-icon{color:#d19a35}.template-icon{color:#5c93ce}.node-name{min-width:0;flex:1;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.tree-node-content>small{padding:2px 4px;border-radius:3px;background:var(--shell-tool-tag-bg,var(--button));color:var(--shell-tool-tag-text,var(--muted));font-size:9px}.empty{display:grid;justify-items:center;gap:7px;margin:18px 8px;padding:18px 12px;border:1px dashed var(--border);border-radius:6px;color:var(--muted);text-align:center;font-size:11px}.empty strong{color:var(--text);font-size:12px}
:global(.resource-tree .el-tree-node__content){height:30px;border:1px solid transparent;border-radius:5px}:global(.resource-tree .el-tree-node__content:hover){background:var(--hover)}:global(.resource-tree .el-tree-node.is-current>.el-tree-node__content){border-color:var(--shell-tool-selected-border,var(--accent));background:var(--selection)}:global(.mx-context-menu.export-workshop-context-menu){--mx-menu-backgroud:var(--shell-tool-surface,#fff);--mx-menu-hover-backgroud:var(--shell-tool-hover,#edf2f7);--mx-menu-active-backgroud:var(--shell-tool-hover,#edf2f7);--mx-menu-open-backgroud:var(--shell-tool-hover,#edf2f7);--mx-menu-open-hover-backgroud:var(--shell-tool-hover,#edf2f7);--mx-menu-divider:rgba(148,163,184,.28);--mx-menu-text:var(--shell-text-primary,#172033);--mx-menu-hover-text:var(--shell-text-primary,#172033);--mx-menu-open-text:var(--shell-text-primary,#172033);--mx-menu-open-hover-text:var(--shell-text-primary,#172033);--mx-menu-shadow-color:rgba(15,23,42,.2);--mx-menu-backgroud-radius:6px;padding:3px 0;border:0;box-shadow:0 10px 28px rgba(15,23,42,.2)}:global(.mx-context-menu.export-workshop-context-menu .mx-context-menu-item){padding:4px 10px;font:11px var(--font,Arial,sans-serif)}:global(.mx-context-menu.export-workshop-context-menu .mx-context-menu-item .label){font-size:11px}:global(.mx-context-menu.export-workshop-context-menu .mx-context-menu-item-separator),:global(.mx-context-menu.export-workshop-context-menu .mx-context-menu-item-sperator){margin:0 10px;padding:3px 0;background:transparent}:global(.mx-context-menu.export-workshop-context-menu .mx-context-menu-item-separator:after),:global(.mx-context-menu.export-workshop-context-menu .mx-context-menu-item-sperator:after){background:rgba(148,163,184,.28)}:global(.mx-context-menu.export-workshop-context-menu .mx-context-menu-item.danger){color:#d14d4d}
</style>
