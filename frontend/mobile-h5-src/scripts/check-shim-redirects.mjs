import fs from 'node:fs'
import path from 'node:path'
import vm from 'node:vm'
import { fileURLToPath } from 'node:url'

const scriptDir = path.dirname(fileURLToPath(import.meta.url))
const distRoot = path.resolve(
  process.argv[2] || path.join(scriptDir, '..', 'dist', 'mobile-h5')
)

const shimFiles = [
  'batch/index.html',
  'result/index.html',
  'profile/index.html',
  'profile/query-records.html',
  'profile/recharge-records.html',
  'profile/about.html',
  'profile/agreement.html',
  'profile/privacy.html',
  'captcha/tdx.html',
  'captcha/tdx/index.html',
  'pages/captcha/tdx/index.html'
]

const failures = []
const requiredShimSnippets = [
  "var base = '/mobile-h5';",
  "var target = path + (window.location.search || '') + (window.location.hash || '');",
  "window.location.replace(base + '/?page=' + encodeURIComponent(page) + '&to=' + encodeURIComponent(target));"
]

function expectedRedirect(pathname, search, hash) {
  const base = '/mobile-h5'
  let localPath = pathname || '/'
  if (localPath.indexOf(base) === 0) {
    localPath = localPath.slice(base.length) || '/'
  }
  const target = localPath + (search || '') + (hash || '')

  let page = 'mobile-h5'
  try {
    const params = new URLSearchParams(search || '')
    page = String(params.get('page') || 'mobile-h5').trim() || 'mobile-h5'
  } catch {}

  return `${base}/?page=${encodeURIComponent(page)}&to=${encodeURIComponent(target)}`
}

function extractInlineScript(html, filePath) {
  const match = html.match(/<script>([\s\S]*?)<\/script>/i)
  if (!match) {
    failures.push(`Missing inline script in: ${filePath}`)
    return ''
  }
  return match[1]
}

function runBehaviorCheck(scriptSource, relPath, pathname, search, hash) {
  let replaced = ''
  const sandbox = {
    window: {
      location: {
        pathname,
        search,
        hash,
        replace(value) {
          replaced = String(value)
        }
      }
    },
    URLSearchParams
  }

  try {
    vm.runInNewContext(scriptSource, sandbox, { timeout: 1000 })
  } catch (error) {
    failures.push(`Runtime error in ${relPath}: ${error.message}`)
    return
  }

  if (!replaced) {
    failures.push(`No redirect emitted in ${relPath} for ${pathname}`)
    return
  }

  const expected = expectedRedirect(pathname, search, hash)
  if (replaced !== expected) {
    failures.push(
      `Redirect mismatch in ${relPath}\n  expected: ${expected}\n  actual:   ${replaced}`
    )
  }
}

for (const relPath of shimFiles) {
  const absPath = path.join(distRoot, relPath)
  if (!fs.existsSync(absPath)) {
    failures.push(`Missing shim file: ${relPath}`)
    continue
  }

  const content = fs.readFileSync(absPath, 'utf8')
  for (const snippet of requiredShimSnippets) {
    if (!content.includes(snippet)) {
      failures.push(`Snippet missing in ${relPath}: ${snippet}`)
    }
  }

  const scriptSource = extractInlineScript(content, relPath)
  if (!scriptSource) {
    continue
  }

  const pathFromFile = `/mobile-h5/${relPath.replace(/\\/g, '/')}`
  runBehaviorCheck(scriptSource, relPath, pathFromFile, '?page=mobile-h5&from=shim-check', '#anchor')
  runBehaviorCheck(scriptSource, relPath, pathFromFile, '?phone=13800138000', '')

  if (relPath.endsWith('/index.html')) {
    const dirPath = pathFromFile.replace(/index\.html$/, '')
    runBehaviorCheck(scriptSource, relPath, dirPath, '?page=mobile-h5', '')
  }
}

const mainJsPath = path.resolve(scriptDir, '..', 'src', 'main.js')
if (!fs.existsSync(mainJsPath)) {
  failures.push('Missing source bootstrap file: src/main.js')
} else {
  const mainJs = fs.readFileSync(mainJsPath, 'utf8')
  const requiredMainSnippets = [
    "params.get('to')",
    "router.replace(redirectPath)",
    "normalizeTargetPath"
  ]
  for (const snippet of requiredMainSnippets) {
    if (!mainJs.includes(snippet)) {
      failures.push(`main.js missing snippet: ${snippet}`)
    }
  }
}

if (failures.length > 0) {
  console.error('mobile-h5 shim checks failed:')
  for (const item of failures) {
    console.error(`- ${item}`)
  }
  process.exit(1)
}

console.log(`mobile-h5 shim checks passed (${shimFiles.length} shim paths verified).`)
