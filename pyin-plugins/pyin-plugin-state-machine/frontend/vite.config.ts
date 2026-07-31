import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import federation from '@originjs/vite-plugin-federation'

export default defineConfig(({ mode }) => {
  const embeddedWatch = mode === 'embedded-watch'
  return {
    base: '/plugin-static/state-machine/',
    plugins: [
      vue(),
      federation({
        name: 'state-machine',
        filename: 'remoteEntry.js',
        exposes: {
          './StateMachineRemoteApp': './src/exposed/StateMachineRemoteApp.vue',
          './routes': './src/exposed/routes.ts'
        },
        shared: ['vue', 'vue-router', 'pinia', 'element-plus']
      })
    ],
    build: {
      target: 'esnext',
      outDir: embeddedWatch ? '../backend/target/classes/plugin-static/state-machine' : 'dist',
      emptyOutDir: embeddedWatch || undefined
    },
    server: {
      port: 4178,
      strictPort: true,
      proxy: {
        '/plugins': 'http://127.0.0.1:8080',
        '/plugin-static': 'http://127.0.0.1:8080'
      }
    }
  }
})
