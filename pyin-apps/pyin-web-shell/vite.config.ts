import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import federation from '@originjs/vite-plugin-federation'

export default defineConfig({
  plugins: [
    vue(),
    federation({
      name: 'pyin-web-shell',
      remotes: {},
      shared: ['vue', 'vue-router', 'pinia']
    })
  ],
  build: {
    target: 'esnext'
  },
  server: {
    port: 5173,
    proxy: {
      '/api': 'http://127.0.0.1:8081',
      '/open': 'http://127.0.0.1:8081',
      '^/plugins/[^/]+/(admin|open)(?:/.*)?$': 'http://127.0.0.1:8081',
      '/plugin-static': 'http://127.0.0.1:8081'
    }
  }
})
