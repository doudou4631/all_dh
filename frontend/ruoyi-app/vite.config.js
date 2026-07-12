import { defineConfig } from 'vite'
import uniModule from '@dcloudio/vite-plugin-uni'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)
const uni = uniModule.default || uniModule

export default defineConfig({
  plugins: [uni()],
  resolve: {
    alias: {
      '@': __dirname
    }
  }
})
