import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

export default defineConfig({
  root: path.resolve(__dirname, 'mobile-h5-src'),
  base: '/mobile-h5/',
  plugins: [vue()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'mobile-h5-src/src')
    }
  },
  server: {
    host: true,
    port: 5175,
    open: false,
    proxy: {
      '/prod-api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  },
  build: {
    outDir: path.resolve(__dirname, 'mobile-h5-src/dist/mobile-h5'),
    emptyOutDir: true,
    assetsDir: 'assets',
    sourcemap: true
  }
})
