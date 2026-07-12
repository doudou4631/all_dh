import { cpSync, existsSync, mkdirSync, readdirSync, rmSync } from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)
const projectRoot = path.resolve(__dirname, '..')
const docsSourceDir = path.join(projectRoot, 'docs')
const docsTargetDir = path.join(projectRoot, 'frontend', 'public', 'docs')
const ruoyiAppH5SourceCandidates = [
  path.join(projectRoot, 'frontend', 'ruoyi-app', 'dist', 'build', 'h5'),
  path.join(projectRoot, 'frontend', 'ruoyi-app', 'unpackage', 'dist', 'build', 'h5')
]
const ruoyiAppStaticSourceDir = path.join(projectRoot, 'frontend', 'ruoyi-app', 'static')
const ruoyiAppH5TargetDir = path.join(projectRoot, 'frontend', 'public', 'mark-app')

if (!existsSync(docsSourceDir)) {
  console.error(`[sync-public-docs] Source directory not found: ${docsSourceDir}`)
  process.exit(1)
}

mkdirSync(docsTargetDir, { recursive: true })

for (const entry of readdirSync(docsTargetDir)) {
  if (entry.toLowerCase().endsWith('.md')) {
    rmSync(path.join(docsTargetDir, entry), { force: true })
  }
}

const copiedFiles = []
for (const entry of readdirSync(docsSourceDir)) {
  if (!entry.toLowerCase().endsWith('.md')) continue
  cpSync(path.join(docsSourceDir, entry), path.join(docsTargetDir, entry))
  copiedFiles.push(entry)
}

console.log(`[sync-public-docs] Synced ${copiedFiles.length} docs to frontend/public/docs`)
for (const fileName of copiedFiles) {
  console.log(`- ${fileName}`)
}

const ruoyiAppH5SourceDir = ruoyiAppH5SourceCandidates.find((dir) => existsSync(path.join(dir, 'index.html')))
if (ruoyiAppH5SourceDir) {
  rmSync(ruoyiAppH5TargetDir, { recursive: true, force: true })
  mkdirSync(ruoyiAppH5TargetDir, { recursive: true })
  cpSync(ruoyiAppH5SourceDir, ruoyiAppH5TargetDir, { recursive: true })
  if (existsSync(ruoyiAppStaticSourceDir)) {
    cpSync(ruoyiAppStaticSourceDir, path.join(ruoyiAppH5TargetDir, 'static'), { recursive: true })
  }
  console.log(`[sync-public-docs] Synced RuoYi-App H5 ${ruoyiAppH5SourceDir} -> ${ruoyiAppH5TargetDir}`)
} else {
  console.log('[sync-public-docs] RuoYi-App H5 build not found, skip frontend/public/mark-app sync')
}
