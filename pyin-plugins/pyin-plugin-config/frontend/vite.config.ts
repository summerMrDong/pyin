import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import federation from '@originjs/vite-plugin-federation'

export default defineConfig(({ mode }) => {
  const embeddedWatch = mode === 'embedded-watch'
  return {
    // 联邦异步资源必须由插件静态目录加载，避免回落到主壳的 /assets。
    base: '/plugin-static/config/',
    plugins: [
      vue(),
      federation({
        name: 'config',
        filename: 'remoteEntry.js',
        exposes: {
          './ConfigRemoteApp': './src/exposed/ConfigRemoteApp.vue',
          './routes': './src/exposed/routes.ts'
        },
        shared: ['vue', 'vue-router', 'pinia', 'element-plus']
      })
    ],
    build: {
      target: 'esnext',
      // 调试时直接写入内嵌插件的 classpath，刷新主壳即可看到最新远端模块。
      outDir: embeddedWatch ? '../backend/target/classes/plugin-static/config' : 'dist',
      emptyOutDir: embeddedWatch || undefined
    },
    server: {
      port: 4174,
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
