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
      shared: ['vue', 'vue-router', 'pinia', 'element-plus']
    })
  ],
  build: {
    target: 'esnext'
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
})
