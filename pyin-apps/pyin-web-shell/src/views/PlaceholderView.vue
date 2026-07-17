<template>
  <section class="placeholder">
    <h2>{{ title }}</h2>
    <p>{{ description }}</p>
  </section>
</template>

<script setup>
defineProps({
  title: {
    type: String,
    required: true
  },
  description: {
    type: String,
    default: '该页面已预留，后续接入真实接口与权限控制。'
  }
})
</script>

<style scoped>
.placeholder {
  padding: 28px;
  border-radius: 20px;
  background: var(--shell-panel-bg);
  border: 1px solid var(--shell-panel-border);
  box-shadow: var(--shell-panel-shadow);
}
</style>
