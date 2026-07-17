import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import federation from '@originjs/vite-plugin-federation'

export default defineConfig({
  plugins: [
    vue(),
    federation({
      name: 'pyinSystem',
      filename: 'remoteEntry.js',
      exposes: {
        './PyinSystemRemoteApp': './src/exposed/PyinSystemRemoteApp.vue',
        './routes': './src/exposed/routes.ts'
      },
      shared: ['vue', 'vue-router', 'pinia', 'element-plus']
    })
  ],
  build: {
    target: 'esnext'
  },
  server: {
    port: 4173,
    strictPort: true
  }
})
