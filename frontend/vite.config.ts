import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

// 后端端口可通过环境变量 BACKEND_PORT 覆盖，默认 8082
const backendPort = process.env.BACKEND_PORT || '8082'
const backendTarget = `http://localhost:${backendPort}`

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
        target: backendTarget,
        changeOrigin: true
      },
      '/api/forum': {
        target: backendTarget,
        changeOrigin: true
      },
      '/api/overview': {
        target: backendTarget,
        changeOrigin: true
      },
      '/rag': {
        target: 'http://172.53.3.98:8000',
        changeOrigin: true,
        rewrite: (path: string) => path.replace(/^\/rag/, '')
      }
    }
  }
})
