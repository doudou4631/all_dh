param(
  [string]$SpecPath = "ops/baseline/baseline-spec.json",
  [string]$OutputPath = "ops/baseline/current-baseline.json"
)

if (!(Test-Path $SpecPath)) {
  throw "Spec file not found: $SpecPath"
}

$spec = Get-Content -Raw -Encoding UTF8 -Path $SpecPath | ConvertFrom-Json
$outputDir = Split-Path -Parent $OutputPath
if ($outputDir -and !(Test-Path $outputDir)) {
  New-Item -ItemType Directory -Path $outputDir -Force | Out-Null
}

$gitHead = ""
try {
  $gitHead = (git rev-parse HEAD).Trim()
} catch {
  $gitHead = ""
}

$result = [ordered]@{
  baselineName = $spec.baselineName
  generatedAt = (Get-Date).ToUniversalTime().ToString("o")
  gitHead = $gitHead
  groups = @()
}

foreach ($group in $spec.groups) {
  $groupResult = [ordered]@{
    id = $group.id
    description = $group.description
    files = @()
    markers = @()
  }

  foreach ($filePath in $group.files) {
    $hash = $null
    if (Test-Path $filePath) {
      $hash = (Get-FileHash -Algorithm SHA256 -Path $filePath).Hash.ToLower()
    }
    $groupResult.files += [ordered]@{
      path = $filePath
      sha256 = $hash
    }
  }

  foreach ($marker in $group.markers) {
    $groupResult.markers += [ordered]@{
      path = $marker.path
      contains = @($marker.contains)
    }
  }

  $result.groups += $groupResult
}

$json = $result | ConvertTo-Json -Depth 100
Set-Content -Path $OutputPath -Value $json -Encoding UTF8
Write-Output "Baseline generated: $OutputPath"
