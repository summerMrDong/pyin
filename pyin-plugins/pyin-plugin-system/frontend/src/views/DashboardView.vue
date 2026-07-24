<template>
  <section class="dashboard">
    <el-card class="hero" shadow="never">
      <p class="eyebrow">Pyin 配置中心 P1</p>
      <h2>系统模块已就绪，扩展模块可按插件状态动态挂载</h2>
      <p>通过顶部应用标签切换系统与扩展插件；系统管理目录由当前插件工作区自行提供。</p>
    </el-card>
  </section>
</template>

<style scoped>
.dashboard {
  display: grid;
  gap: 20px;
}

.hero {
  max-width: 720px;
  padding: 32px;
  border-radius: 24px;
}

.eyebrow {
  text-transform: uppercase;
  letter-spacing: 0.2em;
  color: var(--shell-accent-strong);
}
</style>
