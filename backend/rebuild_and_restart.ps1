$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$jar = Join-Path $root 'geek-admin\target\geek-admin.jar'

function Find-Mvn {
  $cmd = Get-Command mvn -ErrorAction SilentlyContinue
  if ($cmd) { return $cmd.Source }
  $candidates = @(
    'C:\Program Files\Apache\maven\bin\mvn.cmd',
    'C:\apache-maven\bin\mvn.cmd',
    'C:\tools\maven\bin\mvn.cmd'
  )
  foreach ($p in $candidates) {
    if (Test-Path $p) { return $p }
  }
  return $null
}

$mvn = Find-Mvn
if (-not $mvn) {
  Write-Error 'Maven not found. Install Maven or add mvn to PATH, then rerun this script.'
}

Write-Host 'Stopping backend...'
Get-Process java -ErrorAction SilentlyContinue | Where-Object {
  $_.Path -like '*java*'
} | ForEach-Object {
  $cmd = (Get-CimInstance Win32_Process -Filter "ProcessId=$($_.Id)").CommandLine
  if ($cmd -like '*geek-admin.jar*') { Stop-Process -Id $_.Id -Force }
}

Write-Host 'Building backend...'
& $mvn -f (Join-Path $root 'pom.xml') package -pl geek-admin -am -DskipTests
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host 'Starting backend...'
Start-Process -FilePath 'java' -ArgumentList @('-jar', $jar) -WorkingDirectory (Split-Path $jar)
Write-Host 'Backend restarted.'
