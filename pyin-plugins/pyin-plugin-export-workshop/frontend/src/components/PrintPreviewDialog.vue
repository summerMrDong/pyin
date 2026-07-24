<template>
  <div v-if="open" class="backdrop" @click.self="$emit('close')">
    <section class="dialog"><header><strong>导出打印预览</strong><button @click="$emit('close')">×</button></header>
      <div class="preview"><img v-if="image" :src="image" alt="导出预览" /><div v-else class="sheet-preview"><h3>{{ template?.name }}</h3><p>导出任务已完成。下载的 XLSX 与当前模拟预览使用同一份物化工作簿。</p><p>变化单元格：{{ changedCount }}</p></div></div>
      <footer><a :href="downloadUrl">下载 XLSX</a><button @click="printPreview">打印</button></footer>
    </section>
  </div>
</template>
<script setup lang="ts">
defineProps<{ open: boolean; image?: string; downloadUrl?: string; template?: any; changedCount: number }>()
defineEmits(['close'])
const printPreview = () => window.print()
</script>
<style scoped>.backdrop{position:fixed;inset:0;background:#0008;display:grid;place-items:center;z-index:30}.dialog{width:min(760px,90vw);max-height:86vh;background:var(--surface);color:var(--text);box-shadow:0 20px 70px #0008;border:1px solid var(--border);display:grid;grid-template-rows:auto 1fr auto}.dialog header,.dialog footer{padding:10px 13px;border-bottom:1px solid var(--border);display:flex;justify-content:space-between}.dialog footer{border-top:1px solid var(--border);border-bottom:0;gap:8px;justify-content:flex-end}.dialog button,.dialog a{border:1px solid var(--border);background:var(--button);color:var(--text);padding:5px 9px;text-decoration:none;cursor:pointer}.preview{padding:25px;overflow:auto;min-height:250px}.preview img{width:100%}.sheet-preview{background:white;color:#222;min-height:220px;padding:30px;font-family:Arial}</style>
