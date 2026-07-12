import { cpSync, existsSync, mkdirSync, rmSync } from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)
const projectRoot = path.resolve(__dirname, '..')
const ruoyiAppDir = path.join(projectRoot, 'frontend', 'ruoyi-app')
const targetDir = path.join(projectRoot, 'frontend', 'public', 'mark-app')

const sourceCandidates = [
  // uni-app CLI：frontend/ruoyi-app/dist/build/h5
  path.join(ruoyiAppDir, 'dist', 'build', 'h5'),
  // HBuilderX：frontend/ruoyi-app/unpackage/dist/build/h5
  path.join(ruoyiAppDir, 'unpackage', 'dist', 'build', 'h5')
]

const sourceDir = sourceCandidates.find((dir) => existsSync(path.join(dir, 'index.html')))
const staticSourceDir = path.join(ruoyiAppDir, 'static')

if (!sourceDir) {
  console.error('[sync-ruoyi-app-h5] 未找到若依移动端 H5 构建产物。')
  console.error('请先执行以下任一方式生成 H5：')
  console.error('1) CLI：cd frontend/ruoyi-app && npm install && npm run build:h5')
  console.error('2) HBuilderX：发行 -> 网站-H5手机版')
  console.error('期望目录之一：')
  for (const dir of sourceCandidates) {
    console.error(`- ${dir}`)
  }
  process.exit(1)
}

rmSync(targetDir, { recursive: true, force: true })
mkdirSync(targetDir, { recursive: true })
cpSync(sourceDir, targetDir, { recursive: true })

// uni-app H5 的 tabBar 图标等资源仍会按 /static/... 引用；部署到 /mark-app/
// 子路径时需要一并放到 /mark-app/static/，否则底部图标会显示成破图。
if (existsSync(staticSourceDir)) {
  cpSync(staticSourceDir, path.join(targetDir, 'static'), { recursive: true })
}

console.log(`[sync-ruoyi-app-h5] Synced ${sourceDir} -> ${targetDir}`)
