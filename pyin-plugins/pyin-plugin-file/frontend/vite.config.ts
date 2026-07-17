import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import federation from '@originjs/vite-plugin-federation'

export default defineConfig({
  plugins: [
    vue(),
    federation({
      name: 'file',
      filename: 'remoteEntry.js',
      exposes: {
        './FileRemoteApp': './src/exposed/FileRemoteApp.vue',
        './routes': './src/exposed/routes.ts'
      },
      shared: ['vue', 'vue-router', 'pinia']
    })
  ],
  build: {
    target: 'esnext'
  },
  server: {
    port: 4176,
    strictPort: true
  }
})
