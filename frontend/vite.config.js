import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: { '@': '/src' }
  },
  server: {
/*    allowedHosts: true, // 放行任意外部域名
    host: "0.0.0.0",
    port: 5173,*/
    base: 'https://github.com/LeeBlitzcrank/inspire-ai-preview',
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      '/uploads': {
        target: 'http://localhost:8083',
        changeOrigin: true
      }
    }
  },
  build: {
    rollupOptions: {
      output: {
        manualChunks: {
          'element-plus': ['element-plus'],
          vendor: ['vue', 'vue-router']
        }
      }
    },
    chunkSizeWarningLimit: 500
  }
})
