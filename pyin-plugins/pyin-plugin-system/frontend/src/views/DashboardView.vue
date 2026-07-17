<template>
  <section class="dashboard">
    <el-card class="hero" shadow="never">
      <p class="eyebrow">Pyin 配置中心 P1</p>
      <h2>系统模块已就绪，扩展模块可按插件状态动态挂载</h2>
      <p>顶部模块条用于切换系统模块与扩展模块，左侧菜单会随当前模块自动切换。</p>
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
