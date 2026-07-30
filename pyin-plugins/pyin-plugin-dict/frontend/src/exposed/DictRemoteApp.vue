<template>
  <main class="plugin-workbench">
    <DictManagerView />
  </main>
</template>

<script setup>
import DictManagerView from '../views/DictManagerView.vue'
</script>

<style scoped>
.plugin-workbench {
  min-height: calc(100vh - 40px);
  padding: 0;
}
</style>
