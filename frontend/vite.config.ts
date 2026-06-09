import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src')
    }
  },
  server: {
    host: '0.0.0.0',
    port: 5173,
    proxy: {
      '/api/v1': {
        target: 'http://localhost:8081',
        changeOrigin: true
      },
      '/api/forum': {
        target: 'http://localhost:8081',
        changeOrigin: true
      },
      '/api/overview': {
        target: 'http://localhost:8081',
        changeOrigin: true
      }
    }
  }
})
