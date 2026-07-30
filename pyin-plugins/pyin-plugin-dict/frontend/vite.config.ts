import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import federation from '@originjs/vite-plugin-federation'

export default defineConfig(({ mode }) => {
  const embeddedWatch = mode === 'embedded-watch'
  return {
    // 模块联邦异步资源必须始终由插件静态目录加载，不能回落到主壳的 /assets。
    base: '/plugin-static/dict/',
    plugins: [
      vue(),
      federation({
        name: 'dict',
        filename: 'remoteEntry.js',
        exposes: {
          './DictRemoteApp': './src/exposed/DictRemoteApp.vue',
          './routes': './src/exposed/routes.ts'
        },
        shared: ['vue', 'vue-router', 'pinia', 'element-plus']
      })
    ],
    build: {
      target: 'esnext',
      // 内嵌监听模式直接写入运行中后端的 classpath；刷新主壳即可加载最新远端资源。
      outDir: embeddedWatch ? '../backend/target/classes/plugin-static/dict' : 'dist',
      emptyOutDir: embeddedWatch || undefined
    },
    server: {
      port: 4175,
      strictPort: true,
      proxy: {
        '/api': 'http://127.0.0.1:8080',
        '/open': 'http://127.0.0.1:8080',
        '/plugins': 'http://127.0.0.1:8080',
        '/plugin-static': 'http://127.0.0.1:8080'
      }
    }
  }
})
