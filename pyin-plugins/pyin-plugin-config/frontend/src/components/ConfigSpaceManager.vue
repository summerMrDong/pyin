<template>
  <section class="space-manager">
    <header class="space-manager-header">
      <div><strong>空间</strong><small>{{ spaces.length }}</small></div>
      <div class="space-manager-actions">
        <button type="button" title="新建空间" aria-label="新建空间" @click="emit('create')">＋</button>
        <button type="button" title="刷新空间" aria-label="刷新空间" @click="emit('refresh')">↻</button>
      </div>
    </header>

    <div class="space-list">
      <div v-if="!spaces.length" class="space-empty">还没有空间，点击右上角＋创建。</div>
      <article v-for="space in spaces" :key="space.id" class="space-card" :class="{ active: space.id === activeSpaceId }" @click="emit('select', space)">
        <i class="space-indicator" aria-hidden="true" />
        <div class="space-card-main">
          <div class="space-card-title"><strong>{{ space.displayName }}</strong><span>{{ space.itemCount || 0 }} Key</span></div>
          <p>{{ space.namespaceCode }} <b>/</b> {{ space.env }}<em v-if="space.description"> · {{ space.description }}</em></p>
        </div>
        <div class="space-card-actions" @click.stop>
          <button type="button" title="编辑空间" aria-label="编辑空间" @click="emit('edit', space)">✎</button>
          <button type="button" class="danger" title="删除空间" aria-label="删除空间" @click="emit('delete', space)">×</button>
        </div>
      </article>
    </div>
  </section>
</template>

<script setup>
defineProps({
  spaces: { type: Array, default: () => [] },
  activeSpaceId: { type: Number, default: null }
})
const emit = defineEmits(['select', 'create', 'edit', 'delete', 'refresh'])
</script>

<style scoped>
.space-manager { display: flex; min-height: 0; flex: 1; flex-direction: column; background: var(--shell-tool-surface); }.space-manager-header { display: flex; align-items: center; height: 37px; padding: 0 8px; border-bottom: 1px solid var(--shell-tool-divider); }.space-manager-header > div:first-child { display: flex; align-items: baseline; gap: 5px; margin-right: auto; }.space-manager-header strong { color: var(--shell-tool-header-text); font-size: 11px; }.space-manager-header small { color: var(--shell-tool-subtle-text); font-size: 9px; }.space-manager-actions { display: flex; gap: 2px; }.space-manager-actions button { width: 22px; height: 22px; padding: 0; border: 1px solid transparent; border-radius: 2px; background: transparent; color: var(--shell-text-secondary); cursor: pointer; font-size: 15px; }.space-manager-actions button:hover { background: var(--shell-tool-hover); color: var(--shell-accent); }.space-list { min-height: 0; flex: 1; overflow: auto; padding: 3px 5px; }.space-empty { margin: 12px 4px; color: var(--shell-text-muted); font-size: 11px; line-height: 1.6; text-align: center; }.space-card { display: grid; grid-template-columns: 3px minmax(0, 1fr) auto; align-items: center; gap: 7px; min-height: 48px; margin: 1px 0; padding: 5px 6px; border: 1px solid transparent; border-radius: 2px; background: transparent; cursor: pointer; }.space-card:hover { background: var(--shell-tool-hover); }.space-card.active { border-color: var(--shell-tool-selected-border, var(--shell-accent)); background: var(--shell-tool-selected-bg); }.space-indicator { align-self: stretch; border-radius: 2px; background: var(--shell-tool-border-strong); }.space-card.active .space-indicator { background: var(--shell-accent); }.space-card-main { min-width: 0; }.space-card-title { display: flex; align-items: center; justify-content: space-between; gap: 6px; min-width: 0; }.space-card-title strong { overflow: hidden; color: var(--shell-text-primary); font-size: 11px; text-overflow: ellipsis; white-space: nowrap; }.space-card-title span { flex: 0 0 auto; padding: 1px 4px; border-radius: 2px; background: var(--shell-tool-tag-bg); color: var(--shell-text-muted); font: 9px Consolas, monospace; }.space-card p { margin: 3px 0 0; overflow: hidden; color: var(--shell-tool-subtle-text); font: 10px Consolas, monospace; text-overflow: ellipsis; white-space: nowrap; }.space-card p b { color: var(--shell-text-muted); font-weight: 400; }.space-card p em { color: var(--shell-text-secondary); font-family: var(--shell-font-sans, sans-serif); font-style: normal; }.space-card-actions { display: flex; align-items: center; gap: 1px; opacity: 0; transition: opacity .12s ease; }.space-card:hover .space-card-actions, .space-card.active .space-card-actions { opacity: 1; }.space-card-actions button { display: grid; width: 19px; height: 19px; padding: 0; place-items: center; border: 0; border-radius: 2px; background: transparent; color: var(--shell-text-secondary); cursor: pointer; font-size: 13px; }.space-card-actions button:hover { background: var(--shell-tool-surface); color: var(--shell-accent); }.space-card-actions .danger:hover { color: #d14d4d; }
</style>
