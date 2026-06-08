param(
  [string]$BaselinePath = "ops/baseline/current-baseline.json"
)

if (!(Test-Path $BaselinePath)) {
  throw "Baseline file not found: $BaselinePath"
}

$baseline = Get-Content -Raw -Encoding UTF8 -Path $BaselinePath | ConvertFrom-Json
$errors = New-Object System.Collections.Generic.List[string]

foreach ($group in $baseline.groups) {
  foreach ($fileItem in $group.files) {
    $path = [string]$fileItem.path
    $expectedHash = [string]$fileItem.sha256

    if (!(Test-Path $path)) {
      $errors.Add("[$($group.id)] missing file: $path")
      continue
    }

    if ([string]::IsNullOrWhiteSpace($expectedHash)) {
      $errors.Add("[$($group.id)] baseline hash missing: $path")
      continue
    }

    $actualHash = (Get-FileHash -Algorithm SHA256 -Path $path).Hash.ToLower()
    if ($actualHash -ne $expectedHash.ToLower()) {
      $errors.Add("[$($group.id)] hash mismatch: $path`n  expected=$expectedHash`n  actual=$actualHash")
    }
  }

  foreach ($marker in $group.markers) {
    $path = [string]$marker.path
    if (!(Test-Path $path)) {
      $errors.Add("[$($group.id)] marker file missing: $path")
      continue
    }

    $content = Get-Content -Raw -Encoding UTF8 -Path $path
    foreach ($snippet in $marker.contains) {
      $needle = [string]$snippet
      if ($content.IndexOf($needle, [System.StringComparison]::Ordinal) -lt 0) {
        $errors.Add("[$($group.id)] marker missing in $path : $needle")
      }
    }
  }
}

if ($errors.Count -gt 0) {
  Write-Output "Baseline check failed: $($errors.Count) issue(s)"
  foreach ($item in $errors) {
    Write-Output $item
  }
  exit 1
}

Write-Output "Baseline check passed: $($baseline.baselineName)"
Write-Output "Baseline generated at: $($baseline.generatedAt)"
if ($baseline.gitHead) {
  Write-Output "Baseline git head: $($baseline.gitHead)"
}
