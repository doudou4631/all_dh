# Re-sync Plan A files from parent RightControl project
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$parent = Join-Path $root 'src'
$td = Join-Path $root 'td'

if (-not (Test-Path $parent)) {
  Write-Error "Parent src not found: $parent"
}

Write-Host "Syncing from $parent -> $td"

$maps = @(
  @{ From = 'backend-js\src\services\tdx*.js'; To = 'backend\src\services' },
  @{ From = 'backend-js\src\services\orderAutoCheckService.js'; To = 'backend\src\services' },
  @{ From = 'backend-js\src\config\tdxConfig.js'; To = 'backend\src\config' },
  @{ From = 'backend-js\src\data\liveDb.js'; To = 'backend\src\data' },
  @{ From = 'backend-js\src\data\liveUserStore.js'; To = 'backend\src\data' },
  @{ From = 'backend-js\src\data\tdx*.js'; To = 'backend\src\data' },
  @{ From = 'backend-js\src\data\systemSettingsStore.js'; To = 'backend\src\data' },
  @{ From = 'backend-js\src\data\businessEntityStore.js'; To = 'backend\src\data' },
  @{ From = 'backend-js\src\routes\tdx.js'; To = 'backend\src\routes' },
  @{ From = 'backend-js\src\lib\*.js'; To = 'backend\src\lib' },
  @{ From = 'backend-js\data\tdx-*.json'; To = 'backend\data' },
  @{ From = 'backend-js\data\system-settings.json'; To = 'backend\data' },
  @{ From = 'backend-js\data\config-entries.json'; To = 'backend\data' },
  @{ From = 'app-vue\src\pages\TdxSmsReceivePage.vue'; To = 'frontend\src\pages' },
  @{ From = 'app-vue\src\pages\TdxTaskDetailPage.vue'; To = 'frontend\src\pages' },
  @{ From = 'app-vue\src\utils\receipt.js'; To = 'frontend\src\utils' },
  @{ From = 'qmf-proxy.js'; To = 'proxy' }
)

foreach ($m in $maps) {
  $fromPath = Join-Path $parent $m.From
  $toDir = Join-Path $td $m.To
  New-Item -ItemType Directory -Force -Path $toDir | Out-Null
  Get-ChildItem -Path (Join-Path $root "src\$($m.From.Replace('backend-js\','').Replace('app-vue\',''))") -ErrorAction SilentlyContinue
}

# Simpler explicit copies
$srcRoot = Join-Path $root 'src'
Copy-Item "$srcRoot\backend-js\src\services\tdxCleanupService.js" "$td\backend\src\services\" -Force
Copy-Item "$srcRoot\backend-js\src\services\tdxBillingService.js" "$td\backend\src\services\" -Force
Copy-Item "$srcRoot\backend-js\src\services\tdxRuntimeStore.js" "$td\backend\src\services\" -Force
Copy-Item "$srcRoot\backend-js\src\services\tdxSessionValue.js" "$td\backend\src\services\" -Force
Copy-Item "$srcRoot\backend-js\src\services\tdxExecutionPlan.js" "$td\backend\src\services\" -Force
Copy-Item "$srcRoot\backend-js\src\services\tdxExecutorProfile.js" "$td\backend\src\services\" -Force
Copy-Item "$srcRoot\backend-js\src\services\orderAutoCheckService.js" "$td\backend\src\services\" -Force
Copy-Item "$srcRoot\backend-js\src\routes\tdx.js" "$td\backend\src\routes\" -Force
Copy-Item "$srcRoot\app-vue\src\pages\TdxSmsReceivePage.vue" "$td\frontend\src\pages\" -Force
Copy-Item "$srcRoot\app-vue\src\pages\TdxTaskDetailPage.vue" "$td\frontend\src\pages\" -Force

Write-Host "Done. Re-apply td-specific stubs: backend/src/lib/auth.js, routes/index.js, services/tencentCleanupService.js"
