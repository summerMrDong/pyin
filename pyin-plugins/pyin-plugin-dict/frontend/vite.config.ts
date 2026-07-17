import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import federation from '@originjs/vite-plugin-federation'

export default defineConfig({
  plugins: [
    vue(),
    federation({
      name: 'dict',
      filename: 'remoteEntry.js',
      exposes: {
        './DictRemoteApp': './src/exposed/DictRemoteApp.vue',
        './routes': './src/exposed/routes.ts'
      },
      shared: ['vue', 'vue-router', 'pinia']
    })
  ],
  build: {
    target: 'esnext'
  },
  server: {
    port: 4175,
    strictPort: true
  }
})
