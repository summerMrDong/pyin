import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import federation from '@originjs/vite-plugin-federation'

export default defineConfig({
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
