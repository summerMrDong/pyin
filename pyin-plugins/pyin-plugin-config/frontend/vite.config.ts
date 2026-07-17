import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import federation from '@originjs/vite-plugin-federation'

export default defineConfig({
  plugins: [
    vue(),
    federation({
      name: 'config',
      filename: 'remoteEntry.js',
      exposes: {
        './ConfigRemoteApp': './src/exposed/ConfigRemoteApp.vue',
        './routes': './src/exposed/routes.ts'
      },
      shared: ['vue', 'vue-router', 'pinia']
    })
  ],
  build: {
    target: 'esnext'
  },
  server: {
    port: 4174,
    strictPort: true
  }
})
