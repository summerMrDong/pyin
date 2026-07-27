import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import federation from '@originjs/vite-plugin-federation'

export default defineConfig({
  // 远程模块由主壳加载时，懒加载的 Univer 分包也必须回到插件静态目录，
  // 不能使用 Vite 默认的 /assets 路径（它会落到主壳的开发端口）。
  base: '/plugin-static/export-workshop/',
  plugins: [
    vue(),
    federation({
      name: 'export-workshop',
      filename: 'remoteEntry.js',
      exposes: {
        './ExportWorkshopRemoteApp': './src/exposed/ExportWorkshopRemoteApp.vue',
        './routes': './src/exposed/routes.ts'
      },
      shared: ['vue', 'vue-router', 'pinia']
    })
  ],
  build: { target: 'esnext' },
  server: {
    port: 4178,
    strictPort: true,
    proxy: {
      '/api': 'http://127.0.0.1:8080',
      '/plugins': 'http://127.0.0.1:8080',
      '/plugin-static': 'http://127.0.0.1:8080'
    }
  }
})
