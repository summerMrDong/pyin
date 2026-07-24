<template>
  <main class="plugin-workbench">
    <FileManagerView />
  </main>
</template>

<script setup>
import FileManagerView from '../views/FileManagerView.vue'
</script>

<style scoped>
.plugin-workbench {
  min-height: calc(100vh - 40px);
  padding: 20px 24px 28px;
}
</style>
