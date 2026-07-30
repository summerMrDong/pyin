<template>
  <aside class="tree-panel" @click="closeMenu" @contextmenu.prevent="openMenu(undefined, $event)">
    <header class="resource-header"><div><strong>字典资源</strong><small>{{ types.length }}</small></div><small>右键操作</small></header>
    <el-tree ref="treeRef" class="resource-tree" :data="treeData" node-key="nodeKey" :props="treeProps" :current-node-key="activeTypeId ? `type:${activeTypeId}` : undefined" highlight-current default-expand-all :expand-on-click-node="false" @node-click="selectNode" @node-contextmenu="handleNodeContextMenu">
      <template #default="{ data }"><span class="tree-node"><span class="node-icon" :class="data.kind">{{ data.kind === 'category' ? '▰' : '▦' }}</span><span class="node-name">{{ data.label }}</span><small>{{ data.kind === 'category' ? data.dictionaryCount : data.itemCount }}</small></span></template>
    </el-tree>
    <el-empty v-if="!categories.length" class="empty" description="在此区域右键新建分类或字典。" :image-size="58" />
    <div v-if="menuOpen" class="context-menu" :style="{ left: `${menuPosition.x}px`, top: `${menuPosition.y}px` }" @click.stop>
      <template v-if="menuNode?.kind === 'type'"><button @click="emit('new-item', menuNode); closeMenu()">新增字典项</button><button @click="emit('edit-type', menuNode); closeMenu()">编辑字典</button><button @click="emit('toggle-type', menuNode); closeMenu()">{{ menuNode.status === 'ENABLED' ? '停用字典' : '启用字典' }}</button><hr /><button class="danger" @click="emit('delete-type', menuNode); closeMenu()">删除字典</button></template>
      <template v-else-if="menuNode?.kind === 'category'"><button @click="emit('new-type', menuNode); closeMenu()">新建字典</button><button @click="emit('new-category'); closeMenu()">新建分类</button><hr /><button @click="emit('edit-category', menuNode); closeMenu()">编辑分类</button><button class="danger" @click="emit('delete-category', menuNode); closeMenu()">删除分类</button></template>
      <template v-else><button @click="emit('new-type'); closeMenu()">新建字典</button><button @click="emit('new-category'); closeMenu()">新建分类</button></template>
    </div>
  </aside>
</template>

<script setup>
import { computed, ref } from 'vue'

const props = defineProps({ categories: { type: Array, default: () => [] }, types: { type: Array, default: () => [] }, activeTypeId: Number })
const emit = defineEmits(['select-type', 'select-category', 'new-type', 'new-item', 'edit-type', 'toggle-type', 'delete-type', 'new-category', 'edit-category', 'delete-category'])
const treeRef = ref(), menuOpen = ref(false), menuNode = ref(), menuPosition = ref({ x: 0, y: 0 })
const treeProps = { label: 'label', children: 'children' }
const treeData = computed(() => props.categories.map(category => ({ ...category, kind: 'category', label: category.categoryName, nodeKey: `category:${category.id}`, children: props.types.filter(type => type.categoryId === category.id).map(type => ({ ...type, kind: 'type', label: type.typeName, nodeKey: `type:${type.id}` })) })))
function selectNode(data) {
  if (data.kind === 'category') {
    const node = treeRef.value?.getNode(data.nodeKey)
    if (node) node.expanded = !node.expanded
    emit('select-category', data)
  } else emit('select-type', data)
}
function closeMenu() { menuOpen.value = false; menuNode.value = undefined }
function openMenu(node, event) { menuNode.value = node; menuPosition.value = { x: event.clientX, y: event.clientY }; menuOpen.value = true }
function handleNodeContextMenu(event, _node, data) { openMenu(data, event) }
</script>

<style scoped>
.tree-panel{position:relative;display:flex;min-width:0;min-height:0;flex-direction:column;border-right:1px solid var(--divider);background:var(--surface-raised);color:var(--text)}.resource-header{display:flex;align-items:center;gap:5px;height:38px;padding:6px 9px;border-bottom:1px solid var(--divider);font-size:12px}.resource-header>div{display:flex;align-items:baseline;gap:6px;margin-right:auto}.resource-header small{color:var(--muted);font-size:10px;font-weight:400}.resource-tree{min-height:0;flex:1;overflow:auto;padding:5px;background:transparent;color:var(--text);font-size:12px}.resource-tree :deep(.el-tree-node__content){height:30px;border-radius:5px}.resource-tree :deep(.el-tree-node__content:hover),.resource-tree :deep(.is-current>.el-tree-node__content){background:var(--selection)}.tree-node{display:flex;width:100%;min-width:0;align-items:center;gap:6px;padding-right:6px}.node-icon{width:16px;color:var(--muted);text-align:center}.node-icon.category{color:#d79a4b}.node-name{min-width:0;flex:1;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.tree-node small{padding:2px 4px;border-radius:3px;background:var(--shell-tool-tag-bg,var(--button));color:var(--shell-tool-tag-text,var(--muted));font-size:9px}.empty{padding:14px 8px}.context-menu{position:fixed;z-index:30;min-width:170px;padding:4px;border:1px solid var(--border);border-radius:6px;background:var(--surface);box-shadow:0 8px 24px #0002}.context-menu button{display:block;width:100%;padding:7px 9px;border:0;border-radius:4px;background:transparent;color:inherit;cursor:pointer;font:12px var(--font);text-align:left}.context-menu button:hover{background:var(--hover)}.context-menu hr{height:1px;margin:4px 0;border:0;background:var(--divider)}.context-menu .danger{color:#c24141}
</style>
