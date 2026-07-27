<template>
  <main class="plugin-workbench">
    <ConfigManagerView />
  </main>
</template>

<script setup>
import ConfigManagerView from '../views/ConfigManagerView.vue'
</script>

<style scoped>
.plugin-workbench {
  min-height: calc(100vh - 40px);
  padding: 0;
}
</style>
