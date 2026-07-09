$ErrorActionPreference = 'Continue'
$root = 'c:\Users\Administrator\Desktop\1500'
$jarDir = Join-Path $root 'backend\geek-admin\target'
$jar = Join-Path $jarDir 'geek-admin.jar'
$frontend = Join-Path $root 'frontend'
$stdout = Join-Path $jarDir 'backend-stdout.log'
$stderr = Join-Path $jarDir 'backend-stderr.log'

function Test-Port([int]$Port) {
  try {
    $tcp = New-Object System.Net.Sockets.TcpClient
    $iar = $tcp.BeginConnect('127.0.0.1', $Port, $null, $null)
    $ok = $iar.AsyncWaitHandle.WaitOne(1500, $false)
    $connected = $ok -and $tcp.Connected
    $tcp.Close()
    return $connected
  } catch {
    return $false
  }
}

Write-Host '==== 1) Check MySQL :3306 ===='
if (Test-Port 3306) {
  Write-Host 'MySQL already listening on 3306'
} else {
  Write-Host 'MySQL NOT listening. Trying to start service...'
  $services = Get-Service | Where-Object {
    $_.Name -match 'mysql|maria' -or $_.DisplayName -match 'mysql|maria'
  }
  if (-not $services) {
    Write-Host 'ERROR: No MySQL/MariaDB Windows service found.'
    Write-Host 'Please install/start MySQL on 127.0.0.1:3306 database verifynum, then rerun this script.'
    exit 2
  }
  foreach ($svc in $services) {
    Write-Host ("Found service: {0} status={1}" -f $svc.Name, $svc.Status)
    if ($svc.Status -ne 'Running') {
      try {
        Start-Service -Name $svc.Name -ErrorAction Stop
        Write-Host ("Started service {0}" -f $svc.Name)
      } catch {
        Write-Host ("Failed to start {0}: {1}" -f $svc.Name, $_.Exception.Message)
      }
    }
  }
  Start-Sleep -Seconds 3
  if (-not (Test-Port 3306)) {
    Write-Host 'ERROR: MySQL still not listening on 3306 after Start-Service.'
    exit 3
  }
  Write-Host 'MySQL is now listening on 3306'
}

Write-Host '==== 2) Verify DB login ===='
$py = @'
import sys
try:
    import pymysql
    conn = pymysql.connect(host="127.0.0.1", user="verifyNum", password="pL6NspjfTHLazatP", database="verifynum", charset="utf8mb4", connect_timeout=5)
    conn.close()
    print("DB_OK")
except Exception as e:
    print("DB_FAIL:", e)
    sys.exit(4)
'@
$pyFile = Join-Path $env:TEMP 'check_verifynum_db.py'
Set-Content -Path $pyFile -Value $py -Encoding UTF8
python $pyFile
if ($LASTEXITCODE -ne 0) {
  Write-Host 'ERROR: DB credentials/database check failed.'
  exit 4
}

Write-Host '==== 3) Start/restart backend ===='
Get-CimInstance Win32_Process -Filter "name='java.exe'" | ForEach-Object {
  if ($_.CommandLine -and $_.CommandLine -like '*geek-admin.jar*') {
    Write-Host ("Stopping old backend PID {0}" -f $_.ProcessId)
    Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue
  }
}
if (-not (Test-Path $jar)) {
  Write-Host "ERROR: jar missing: $jar"
  exit 5
}
if (Test-Path $stdout) { Remove-Item $stdout -Force -ErrorAction SilentlyContinue }
if (Test-Path $stderr) { Remove-Item $stderr -Force -ErrorAction SilentlyContinue }
$p = Start-Process -FilePath 'java' -ArgumentList @('-jar', 'geek-admin.jar') -WorkingDirectory $jarDir -RedirectStandardOutput $stdout -RedirectStandardError $stderr -PassThru
Write-Host ("Backend starting PID={0}" -f $p.Id)

$ready = $false
for ($i = 0; $i -lt 60; $i++) {
  Start-Sleep -Seconds 2
  if (Test-Path $stdout) {
    $tail = Get-Content $stdout -Raw -ErrorAction SilentlyContinue
    if ($tail -match 'Started GeekApplication') {
      $ready = $true
      break
    }
    if ($tail -match 'Application run failed|Communications link failure') {
      Write-Host 'ERROR: Backend failed during startup. See backend-stdout.log'
      Get-Content $stdout -Tail 40
      exit 6
    }
  }
  if (-not (Get-Process -Id $p.Id -ErrorAction SilentlyContinue)) {
    Write-Host 'ERROR: Backend process exited early.'
    if (Test-Path $stdout) { Get-Content $stdout -Tail 50 }
    if (Test-Path $stderr) { Get-Content $stderr -Tail 50 }
    exit 7
  }
}
if (-not $ready -and -not (Test-Port 8080)) {
  Write-Host 'ERROR: Backend did not become ready on 8080 in time.'
  if (Test-Path $stdout) { Get-Content $stdout -Tail 50 }
  exit 8
}
Write-Host 'Backend OK -> http://localhost:8080'

Write-Host '==== 4) Ensure frontend ===='
if (Test-Port 80) {
  Write-Host 'Frontend already listening on 80'
} else {
  Write-Host 'Starting frontend npm run dev...'
  Start-Process -FilePath 'cmd.exe' -ArgumentList @('/c', 'npm run dev') -WorkingDirectory $frontend -WindowStyle Minimized
  Start-Sleep -Seconds 5
  if (Test-Port 80) {
    Write-Host 'Frontend OK -> http://localhost/'
  } else {
    Write-Host 'WARN: Frontend port 80 not confirmed yet. Check frontend manually.'
  }
}

Write-Host ''
Write-Host '==== DONE ===='
Write-Host 'Frontend: http://localhost/'
Write-Host 'Backend : http://localhost:8080'
Write-Host 'Open the frontend URL in browser, then re-login.'
