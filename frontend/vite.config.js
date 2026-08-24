import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    port: 5173,
    proxy: {
      // 後端沒有設定 CORS，開發時一定要靠這個 proxy 轉發，不能讓瀏覽器直接跨埠打 :8080
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  },
  build: {
    // 相對 frontend/ 資料夾；打包產物跟後端包進同一個 jar（同源部署，不需要 CORS）
    outDir: '../src/main/resources/static',
    emptyOutDir: true
  }
})
