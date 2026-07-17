<template>
  <section class="link-viewer">
    <iframe v-if="url" :src="url" class="link-viewer-frame" :title="title" />
    <div v-else class="link-viewer-empty">未提供可加载的链接地址。</div>
  </section>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()

const title = computed(() => route.query.title ?? route.meta?.title ?? '外部链接')
const url = computed(() => route.query.url ?? '')
</script>

<style scoped>
.link-viewer {
  height: 100%;
  min-height: calc(100vh - 170px);
}

.link-viewer-frame {
  width: 100%;
  height: 100%;
  border: 0;
  background: #fff;
}

.link-viewer-empty {
  padding: 32px;
  border-radius: 20px;
  background: var(--shell-panel-bg, #fff);
  border: 1px dashed var(--shell-panel-border);
  color: var(--shell-text-secondary);
}
</style>
