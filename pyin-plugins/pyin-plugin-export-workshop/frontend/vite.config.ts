import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import federation from '@originjs/vite-plugin-federation'

export default defineConfig(({ mode }) => {
  const embeddedWatch = mode === 'embedded-watch'
  return {
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
    build: {
      target: 'esnext',
      // 开发监听模式直接写入当前内嵌后端的 classpath，主壳刷新即可加载新远端。
      outDir: embeddedWatch ? '../backend/target/classes/plugin-static/export-workshop' : 'dist',
      emptyOutDir: embeddedWatch || undefined,
    },
    server: {
      port: 4178,
      strictPort: true,
      proxy: {
        '/api': 'http://127.0.0.1:8081',
        '/plugins': 'http://127.0.0.1:8081',
        '/plugin-static': 'http://127.0.0.1:8081'
      }
    }
  }
})
