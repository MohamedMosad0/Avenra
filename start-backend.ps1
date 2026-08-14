# ==============================================================================
# Avenra Local Backend Development Startup Script (PowerShell)
# Starts and keeps the local REST API server running for Android development.
# ==============================================================================

$ErrorActionPreference = "Continue"

Write-Host "=====================================================" -ForegroundColor Cyan
Write-Host "           Starting Avenra Local Backend            " -ForegroundColor Cyan
Write-Host "=====================================================" -ForegroundColor Cyan

# 1. Resolve Node.js Executable
$NodeExe = $null

if (Get-Command node -ErrorAction SilentlyContinue) {
    $NodeExe = "node"
} else {
    $CandidatePaths = @(
        "$env:LOCALAPPDATA\ms-playwright-go\1.57.0\node.exe",
        "$env:LOCALAPPDATA\ms-playwright-go\1.50.1\node.exe",
        "$env:ProgramFiles\nodejs\node.exe",
        "${env:ProgramFiles(x86)}\nodejs\node.exe",
        "$env:LOCALAPPDATA\Programs\node\node.exe",
        "$env:APPDATA\nvm\current\node.exe"
    )

    foreach ($Path in $CandidatePaths) {
        if (Test-Path $Path) {
            $NodeExe = $Path
            break
        }
    }
}

if (-not $NodeExe) {
    Write-Host "[ERROR] Node.js executable not found in PATH or standard directories." -ForegroundColor Red
    Write-Host "Please install Node.js (v18+) or add node.exe to your PATH." -ForegroundColor Yellow
    exit 1
}

$NodeVersion = & $NodeExe -v
Write-Host "[OK] Using Node: $NodeExe ($NodeVersion)" -ForegroundColor Green

# 2. Configure Android ADB Port Forwarding (if adb is available)
$AdbExe = $null
if (Get-Command adb -ErrorAction SilentlyContinue) {
    $AdbExe = "adb"
} else {
    $AdbCandidates = @(
        "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe",
        "$env:ANDROID_HOME\platform-tools\adb.exe",
        "$env:ANDROID_SDK_ROOT\platform-tools\adb.exe"
    )
    foreach ($Path in $AdbCandidates) {
        if (Test-Path $Path) {
            $AdbExe = $Path
            break
        }
    }
}

if ($AdbExe) {
    Write-Host "[INFO] Setting up ADB reverse tcp:3000 tcp:3000..." -ForegroundColor Cyan
    try {
        & $AdbExe reverse tcp:3000 tcp:3000 2>$null
        Write-Host "[OK] ADB reverse mapping active (Android device -> localhost:3000)." -ForegroundColor Green
    } catch {
        Write-Host "[WARN] Could not apply adb reverse (no device connected yet; continuing)." -ForegroundColor Yellow
    }
} else {
    Write-Host "[INFO] ADB not found in standard paths; continuing backend startup." -ForegroundColor DarkGray
}

# 3. Change to backend directory
$BackendDir = Join-Path $PSScriptRoot "backend"
if (-not (Test-Path $BackendDir)) {
    $BackendDir = $PSScriptRoot
}

Set-Location -Path $BackendDir

# 4. Build TypeScript dist if needed
$TscPath = Join-Path $BackendDir "node_modules\typescript\bin\tsc"
if (Test-Path $TscPath) {
    Write-Host "[INFO] Ensuring TypeScript build is up to date..." -ForegroundColor Cyan
    & $NodeExe $TscPath
}

# 5. Start Server with Auto-Restart Keep-Alive Loop
$ServerScript = Join-Path $BackendDir "dist\server.js"
if (-not (Test-Path $ServerScript)) {
    # Fallback to tsx with src/server.ts
    $TsxPath = Join-Path $BackendDir "node_modules\tsx\dist\cli.mjs"
    $ServerScript = Join-Path $BackendDir "src\server.ts"
    $ServerArgs = @($TsxPath, $ServerScript)
} else {
    $ServerArgs = @($ServerScript)
}

Write-Host "`n[Avenra Backend] Press Ctrl+C at any time to stop the server.`n" -ForegroundColor Yellow

$RestartCount = 0
$MaxConsecutiveRestarts = 10

while ($true) {
    $StartTime = Get-Date
    Write-Host "[Avenra Backend] Starting server process..." -ForegroundColor Cyan
    
    & $NodeExe $ServerArgs

    $ExitCode = $LASTEXITCODE
    $Runtime = ((Get-Date) - $StartTime).TotalSeconds

    if ($ExitCode -eq 0) {
        Write-Host "[Avenra Backend] Server process stopped cleanly." -ForegroundColor Yellow
        break
    }

    # Reset restart count if the server ran for at least 10 seconds before crashing
    if ($Runtime -ge 10) {
        $RestartCount = 0
    } else {
        $RestartCount++
    }

    if ($RestartCount -ge $MaxConsecutiveRestarts) {
        Write-Host "[ERROR] Server crashed repeatedly ($RestartCount times in short succession). Aborting auto-restart." -ForegroundColor Red
        break
    }

    Write-Host "[WARN] Server process exited with code $ExitCode. Auto-restarting in 2 seconds..." -ForegroundColor Yellow
    Start-Sleep -Seconds 2
}
