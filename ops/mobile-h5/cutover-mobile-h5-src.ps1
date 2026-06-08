[CmdletBinding(SupportsShouldProcess = $true)]
param(
  [string]$DistRelativePath = "frontend/mobile-h5-src/dist/mobile-h5",
  [string]$TargetRelativePath = "frontend/public/mobile-h5",
  [switch]$RunBuild
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path

$distPath = Join-Path $repoRoot $DistRelativePath
$targetPath = Join-Path $repoRoot $TargetRelativePath

if ($RunBuild) {
  Write-Output "Running build:mobile-h5-src ..."
  npm --prefix (Join-Path $repoRoot "frontend") run build:mobile-h5-src
  if ($LASTEXITCODE -ne 0) {
    throw "build:mobile-h5-src failed with exit code $LASTEXITCODE"
  }
}

if (!(Test-Path -LiteralPath $distPath)) {
  throw "Dist path not found: $distPath"
}

$distIndexPath = Join-Path $distPath "index.html"
if (!(Test-Path -LiteralPath $distIndexPath)) {
  throw "Dist entry file missing: $distIndexPath"
}

if ($PSCmdlet.ShouldProcess($targetPath, "Replace with build output from $distPath")) {
  if (Test-Path -LiteralPath $targetPath) {
    Remove-Item -LiteralPath $targetPath -Recurse -Force
  }
  New-Item -ItemType Directory -Path $targetPath -Force | Out-Null

  $distItems = Get-ChildItem -LiteralPath $distPath -Force
  foreach ($item in $distItems) {
    Copy-Item -LiteralPath $item.FullName -Destination $targetPath -Recurse -Force
  }
}

Write-Output "mobile-h5 cutover finished."
Write-Output "Dist source: $distPath"
Write-Output "Published target: $targetPath"

if ($WhatIfPreference) {
  Write-Output "WhatIf mode enabled: no files were changed."
}
