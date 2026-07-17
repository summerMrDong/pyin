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
      '/api': 'http://127.0.0.1:8080',
      '/capi': 'http://127.0.0.1:8080',
      '/plugin-static': 'http://127.0.0.1:8080'
    }
  }
})
