<template>
  <div class="directory-tree-panel" @contextmenu="openRootContext">
    <div class="tree-search-row"><el-input v-model.trim="keyword" class="tree-search" placeholder="搜索目录或 Key" clearable>
      <template #prefix><el-icon class="search-icon"><Search /></el-icon></template>
    </el-input></div>

    <div class="tree-caption">
      <div class="tree-caption-title"><strong>配置键</strong><span>{{ props.items.length }} 项 · 右键管理</span></div>
      <div class="tree-caption-actions">
        <button type="button" title="新建配置" aria-label="新建配置" @click="emit('create-item')"><el-icon><Plus /></el-icon></button>
        <button type="button" title="刷新目录" aria-label="刷新目录" @click="emit('refresh')"><el-icon><Refresh /></el-icon></button>
        <button type="button" title="全部收起" aria-label="全部收起" @click="collapseAll"><el-icon><Fold /></el-icon></button>
      </div>
    </div>
    <el-tree
      ref="treeRef"
      class="key-tree"
      :data="filteredTreeData"
      node-key="nodeKey"
      :props="treeProps"
      :default-expanded-keys="rootKeys"
      :expand-on-click-node="true"
      highlight-current
      @node-click="emitNodeClick"
      @node-expand="onNodeExpand"
      @node-collapse="onNodeCollapse"
      @node-contextmenu="openNodeContext"
    >
      <template #default="{ data }">
        <span class="tree-node" :class="{ 'is-key': data.kind === 'item', 'is-disabled': data.kind === 'item' && data.status === 'DISABLED' }">
          <span class="tree-node-main">
            <el-icon class="tree-node-icon" :class="data.kind === 'item' ? 'key-icon' : 'folder-icon'"><Key v-if="data.kind === 'item'" /><Folder v-else /></el-icon>
            <span class="tree-node-label">{{ data.label }}</span>
          </span>
          <span v-if="data.kind === 'item'" class="tree-node-meta">
            <small class="item-status" :class="{ disabled: data.status === 'DISABLED', draft: data.status !== 'DISABLED' && (data.status === 'DRAFT' || data.itemValue == null || data.itemValue === '') }"><i v-if="data.status !== 'DISABLED' && (data.status === 'DRAFT' || data.itemValue == null || data.itemValue === '')" class="draft-status-dot" /><template v-else><el-icon><CircleCloseFilled v-if="data.status === 'DISABLED'" /><CircleCheckFilled v-else /></el-icon>{{ data.status === 'DISABLED' ? '停用' : '启用' }}</template></small>
            <small class="value-type" :class="`type-${data.valueType?.toLowerCase()}`">{{ typeText(data.valueType) }}</small>
          </span>
          <small v-else-if="data.depth === 0" class="directory-count">{{ data.itemCount }} 键</small>
        </span>
      </template>
    </el-tree>
  </div>
</template>

<script setup>
import ContextMenu from '@imengyu/vue3-context-menu'
import '@imengyu/vue3-context-menu/lib/vue3-context-menu.css'
import { computed, nextTick, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { CircleCheckFilled, CircleCloseFilled, Fold, Folder, Key, Plus, Refresh, Search } from '@element-plus/icons-vue'

const props = defineProps({
  items: { type: Array, default: () => [] }
})
const emit = defineEmits(['node-click', 'create-item', 'edit-item', 'copy-key', 'set-status', 'delete-item', 'refresh'])
const keyword = ref('')
const treeRef = ref()
const treeProps = { label: 'label', children: 'children' }
const treeData = computed(() => buildKeyTree(props.items))
const filteredTreeData = computed(() => filterTree(treeData.value, keyword.value))
const rootKeys = computed(() => treeData.value.map((node) => node.nodeKey))
const expandedNodeKeys = ref(new Set())
const treeStateInitialized = ref(false)

function typeText(type) {
  return ({ STRING: 'String', INTEGER: 'Integer', BOOLEAN: 'Boolean', JSON: 'JSON' })[type] || 'String'
}

function emitNodeClick(node) {
  emit('node-click', node)
}

function onNodeExpand(node) {
  expandedNodeKeys.value = new Set([...expandedNodeKeys.value, node.nodeKey])
}

function onNodeCollapse(node) {
  const nextKeys = new Set(expandedNodeKeys.value)
  nextKeys.delete(node.nodeKey)
  expandedNodeKeys.value = nextKeys
}

function collapseAll() {
  Object.values(treeRef.value?.store?.nodesMap || {}).forEach((node) => { node.expanded = false })
  expandedNodeKeys.value = new Set()
}

async function restoreExpandedNodes() {
  await nextTick()
  if (!treeStateInitialized.value && treeData.value.length) {
    expandedNodeKeys.value = new Set(rootKeys.value)
    treeStateInitialized.value = true
  }
  const nodesMap = treeRef.value?.store?.nodesMap || {}
  if (keyword.value.trim()) {
    collectExpandableNodeKeys(filteredTreeData.value).forEach((key) => {
      if (nodesMap[key]) nodesMap[key].expanded = true
    })
    return
  }
  expandedNodeKeys.value.forEach((key) => {
    if (nodesMap[key]) nodesMap[key].expanded = true
  })
}

watch([treeData, keyword], restoreExpandedNodes, { flush: 'post', immediate: true })

function menuItemsFor(node) {
  if (!node) return [
    { label: '新建配置', onClick: () => emit('create-item') },
    { label: '刷新目录', divided: 'up', onClick: () => emit('refresh') }
  ]
  if (node.kind === 'directory') return [
    { label: '新建配置', onClick: () => emit('create-item', node.path) },
    { label: '复制目录路径', onClick: () => copyText(node.path, '目录路径已复制') },
    { label: '刷新目录', divided: 'up', onClick: () => emit('refresh') }
  ]
  return [
    { label: '编辑配置', onClick: () => emit('edit-item', node) },
    { label: '复制完整 Key', onClick: () => emit('copy-key', node) },
    { label: node.status === 'DISABLED' ? '启用配置' : '停用配置', onClick: () => emit('set-status', node, node.status === 'DISABLED' ? 'ENABLED' : 'DISABLED') },
    { label: '删除配置', divided: 'up', customClass: 'danger', onClick: () => emit('delete-item', node) }
  ]
}

function showContext(node, event) {
  event.preventDefault()
  event.stopPropagation()
  ContextMenu.showContextMenu({
    x: event.clientX,
    y: event.clientY,
    items: menuItemsFor(node),
    theme: 'flat',
    customClass: 'config-directory-context-menu',
    minWidth: 156,
    zIndex: 3000,
    adjustPosition: true
  })
}

function openNodeContext(event, node) {
  treeRef.value?.setCurrentKey(node.nodeKey)
  emitNodeClick(node)
  showContext(node, event)
}

function openRootContext(event) {
  if (event.target.closest('.el-tree-node__content')) return
  showContext(undefined, event)
}

async function copyText(value, message) {
  try {
    await navigator.clipboard.writeText(value)
    ElMessage.success(message)
  } catch {
    // A copy action is still available from the main workspace when the browser denies clipboard access.
  }
}

function clearSelection() {
  treeRef.value?.setCurrentKey(undefined)
}

function selectPath(path) {
  const node = findTreeNode(treeData.value, `directory:${path}`)
  if (!node) return
  treeRef.value?.setCurrentKey(node.nodeKey)
  emitNodeClick(node)
}

function selectItemKey(itemId) {
  const node = findTreeNode(treeData.value, `item:${itemId}`)
  if (node) treeRef.value?.setCurrentKey(node.nodeKey)
}

function buildKeyTree(source) {
  const roots = []
  const directories = new Map()
  for (const item of source) {
    const parts = item.itemKey.split(':')
    let children = roots
    let path = ''
    parts.forEach((part, index) => {
      path = path ? `${path}:${part}` : part
      if (index === parts.length - 1) {
        children.push({ nodeKey: `item:${item.id}`, label: part, path, kind: 'item', itemId: item.id, itemCount: 1, fullKey: item.itemKey, itemValue: item.itemValue, valueType: item.valueType, status: item.status, children: [] })
        return
      }
      let node = directories.get(path)
      if (!node) {
        node = { nodeKey: `directory:${path}`, label: part, path, depth: index, kind: 'directory', itemIds: [], itemCount: 0, children: [] }
        directories.set(path, node)
        children.push(node)
      }
      node.itemIds.push(item.id)
      node.itemCount++
      children = node.children
    })
  }
  return roots
}

function filterTree(nodes, rawKeyword) {
  const query = rawKeyword.trim().toLowerCase()
  if (!query) return nodes
  return nodes.reduce((result, node) => {
    const children = filterTree(node.children || [], query)
    if (fuzzyMatch(node.label, query) || fuzzyMatch(node.fullKey, query) || children.length) result.push({ ...node, children })
    return result
  }, [])
}

function fuzzyMatch(value, query) {
  const text = String(value || '').toLowerCase()
  if (text.includes(query)) return true
  let cursor = 0
  for (const character of query) {
    cursor = text.indexOf(character, cursor)
    if (cursor < 0) return false
    cursor++
  }
  return true
}

function collectExpandableNodeKeys(nodes, keys = new Set()) {
  nodes.forEach((node) => {
    if (node.children?.length) {
      keys.add(node.nodeKey)
      collectExpandableNodeKeys(node.children, keys)
    }
  })
  return keys
}

function findTreeNode(nodes, key) {
  for (const node of nodes) {
    if (node.nodeKey === key) return node
    const match = findTreeNode(node.children || [], key)
    if (match) return match
  }
}

defineExpose({ clearSelection, selectPath, selectItemKey })
</script>

<style scoped>
.directory-tree-panel { display: flex; min-height: 0; flex: 1; flex-direction: column; background: var(--shell-tool-surface); }
.tree-search-row { padding: 8px 9px 3px; }
.tree-search :deep(.el-input__wrapper) { min-height: 29px; padding: 0 9px; border-radius: 4px; background: var(--shell-tool-toolbar-bg); box-shadow: 0 0 0 1px var(--shell-tool-border-strong) inset; transition: box-shadow .15s ease; }
.tree-search :deep(.el-input__wrapper.is-focus) { box-shadow: 0 0 0 1px var(--shell-accent) inset; }
.tree-search :deep(.el-input__inner) { font-size: 11px; }
.search-icon { color: var(--shell-tool-subtle-text); font-size: 13px; }
.tree-caption { display: flex; align-items: center; justify-content: space-between; height: 30px; margin: 0 10px; border-bottom: 1px solid var(--shell-tool-divider); color: var(--shell-tool-subtle-text); font-size: 10px; }
.tree-caption-title, .tree-caption-actions { display: flex; align-items: center; }
.tree-caption-title { min-width: 0; gap: 6px; }
.tree-caption strong { color: var(--shell-tool-header-text); font-size: 11px; font-weight: 650; }
.tree-caption span { overflow: hidden; font-size: 9px; text-overflow: ellipsis; white-space: nowrap; }
.tree-caption-actions { flex: 0 0 auto; gap: 1px; }
.tree-caption-actions button { display: grid; width: 21px; height: 21px; padding: 0; place-items: center; border: 0; border-radius: 3px; background: transparent; color: var(--shell-tool-subtle-text); cursor: pointer; font-size: 13px; }
.tree-caption-actions button:hover { background: var(--shell-tool-hover); color: var(--shell-accent); }
.key-tree { flex: 1; min-height: 0; overflow: auto; padding: 6px 6px 10px; background: transparent; color: var(--shell-text-primary); --el-tree-node-hover-bg-color: transparent; }
.key-tree :deep(.el-tree-node__content) { height: 35px; margin: 1px 0; border: 1px solid transparent; border-radius: 4px; }
.key-tree :deep(.el-tree-node__content:hover) { background: var(--shell-tool-hover); }
.key-tree :deep(.el-tree-node__expand-icon) { width: 18px; padding: 0; color: var(--shell-tool-subtle-text); font-size: 12px; }
.key-tree :deep(.is-current > .el-tree-node__content) { border-color: color-mix(in srgb, var(--shell-accent) 28%, transparent); box-shadow: inset 2px 0 var(--shell-accent); background: var(--shell-tool-selected-bg); }
.tree-node, .tree-node-main, .tree-node-meta { display: flex; align-items: center; }
.tree-node { width: 100%; justify-content: space-between; gap: 6px; padding-right: 7px; }
.tree-node-main { min-width: 0; gap: 8px; overflow: hidden; }
.tree-node-label { overflow: hidden; color: var(--shell-text-primary); font-size: 12px; text-overflow: ellipsis; white-space: nowrap; }
.tree-node-icon { width: 15px; height: 15px; flex: 0 0 auto; font-size: 15px; }
.folder-icon { color: #dca94f; }
.key-icon { color: var(--shell-accent); }
.tree-node-meta { display: grid; width: 86px; height: 18px; flex: 0 0 86px; grid-template-columns: 40px 42px; align-items: center; gap: 4px; }
.tree-node small { display: inline-flex; height: 18px; min-height: 18px; box-sizing: border-box; align-items: center; justify-content: center; border-radius: 3px; font-size: 10px; line-height: 18px; }
.directory-count { min-width: 18px; padding: 0 4px; background: var(--shell-tool-tag-bg); color: var(--shell-text-muted); }
.value-type { width: 100%; padding: 0 3px; background: color-mix(in srgb, #7896b3 14%, transparent); color: var(--shell-text-secondary); }
.value-type.type-integer { background: color-mix(in srgb, #659ce1 16%, transparent); color: #75abea; }
.value-type.type-boolean { background: color-mix(in srgb, #76a64f 16%, transparent); color: #91c76a; }
.value-type.type-json { background: color-mix(in srgb, #8a6fd4 18%, transparent); color: #a58aef; }
.item-status { width: 100%; gap: 2px; padding: 0 3px; background: color-mix(in srgb, var(--shell-accent) 14%, transparent); color: var(--shell-accent); }
.item-status .el-icon { font-size: 9px; }
.item-status.disabled { background: color-mix(in srgb, #9ba3ad 14%, transparent); color: var(--shell-text-muted); }
.item-status.draft { min-width: 0; padding: 0; background: transparent; }.draft-status-dot { width: 6px; height: 6px; border-radius: 50%; background: #e6a23c; box-shadow: 0 0 0 2px color-mix(in srgb, #e6a23c 18%, transparent); }
.tree-node.is-disabled .tree-node-label, .tree-node.is-disabled .key-icon, .tree-node.is-disabled .value-type { color: var(--shell-text-muted); opacity: .6; }
.tree-node.is-disabled .value-type { background: color-mix(in srgb, #9ba3ad 12%, transparent); }
.key-tree :deep(.is-current > .el-tree-node__content) .tree-node-label { color: var(--shell-accent); font-weight: 600; }
.key-tree :deep(.is-current > .el-tree-node__content) .tree-node.is-disabled .tree-node-label { color: var(--shell-text-secondary); }
:global(.mx-context-menu.config-directory-context-menu){--mx-menu-backgroud:var(--shell-tool-surface,#fff);--mx-menu-hover-backgroud:var(--shell-tool-hover,#edf2f7);--mx-menu-active-backgroud:var(--shell-tool-hover,#edf2f7);--mx-menu-divider:rgba(148,163,184,.28);--mx-menu-text:var(--shell-text-primary,#172033);--mx-menu-hover-text:var(--shell-text-primary,#172033);--mx-menu-shadow-color:rgba(15,23,42,.2);--mx-menu-backgroud-radius:6px;padding:3px 0;border:0;box-shadow:0 10px 28px rgba(15,23,42,.2)}:global(.mx-context-menu.config-directory-context-menu .mx-context-menu-item){padding:4px 10px;font:11px var(--font,Arial,sans-serif)}:global(.mx-context-menu.config-directory-context-menu .mx-context-menu-item-separator),:global(.mx-context-menu.config-directory-context-menu .mx-context-menu-item-sperator){margin:0 10px;padding:3px 0;background:transparent}:global(.mx-context-menu.config-directory-context-menu .mx-context-menu-item-separator:after),:global(.mx-context-menu.config-directory-context-menu .mx-context-menu-item-sperator:after){background:rgba(148,163,184,.28)}:global(.mx-context-menu.config-directory-context-menu .mx-context-menu-item.danger){color:#d14d4d}
</style>
