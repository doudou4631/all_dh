import { cpSync, existsSync, mkdirSync, readdirSync, rmSync } from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)
const projectRoot = path.resolve(__dirname, '..')
const docsSourceDir = path.join(projectRoot, 'docs')
const docsTargetDir = path.join(projectRoot, 'frontend', 'public', 'docs')

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
