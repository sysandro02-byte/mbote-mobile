param(
  [string]$AvdName = "mbote_api26",
  [string]$AndroidSdkRoot = "D:\Android\Sdk",
  [switch]$InstallDebugApk
)

$ErrorActionPreference = "Stop"

$emulatorExe = Join-Path $AndroidSdkRoot "emulator\emulator.exe"
$adbExe = Join-Path $AndroidSdkRoot "platform-tools\adb.exe"
$apkPath = Join-Path (Split-Path -Parent $PSScriptRoot) "app\build\outputs\apk\debug\app-debug.apk"

if (-not (Test-Path -LiteralPath $emulatorExe)) {
  throw "emulator.exe introuvable: $emulatorExe"
}

if (-not (Test-Path -LiteralPath $adbExe)) {
  throw "adb.exe introuvable: $adbExe"
}

$runningDevice = (& $adbExe devices | Select-String -Pattern "\s+device\b" | Select-Object -First 1)
if (-not $runningDevice) {
  $availableAvds = & $emulatorExe -list-avds
  if (-not $availableAvds -or ($availableAvds -notcontains $AvdName)) {
    $fallbackAvd = $availableAvds | Select-Object -First 1
    if ([string]::IsNullOrWhiteSpace($fallbackAvd)) {
      throw "Aucun emulateur Android configure et aucun appareil Android connecte. Cree un AVD dans Android Studio ou branche ton telephone, puis relance ce bouton."
    }
    Write-Host "AVD '$AvdName' introuvable. Utilisation de '$fallbackAvd'." -ForegroundColor Yellow
    $AvdName = $fallbackAvd
  }

  Write-Host "Demarrage de l'emulateur Android '$AvdName'..." -ForegroundColor Cyan
  Start-Process -FilePath $emulatorExe -ArgumentList @(
    "-avd", $AvdName,
    "-netdelay", "none",
    "-netspeed", "full",
    "-writable-system"
  )

  Write-Host "Attente du demarrage Android..." -ForegroundColor Cyan
  & $adbExe wait-for-device
  do {
    Start-Sleep -Seconds 2
    $bootCompleted = (& $adbExe shell getprop sys.boot_completed 2>$null).Trim()
  } while ($bootCompleted -ne "1")
} else {
  Write-Host "Un appareil Android est deja connecte: $($runningDevice.Line.Trim())" -ForegroundColor Green
}

Write-Host "Activation du clavier PC pour l'emulateur..." -ForegroundColor Cyan
& $adbExe shell settings put secure show_ime_with_hard_keyboard 1 | Out-Null
& $adbExe shell settings put system show_touches 1 | Out-Null

if ($InstallDebugApk) {
  if (-not (Test-Path -LiteralPath $apkPath)) {
    throw "APK debug introuvable: $apkPath. Lance d'abord la compilation Gradle assembleDebug."
  }
  Write-Host "Installation de l'APK MBote..." -ForegroundColor Cyan
  & $adbExe install -r $apkPath
}

Write-Host "Emulateur pret. Le clavier PC est active." -ForegroundColor Green
Write-Host "Astuce: double-clique start-mbote-emulator.bat pour relancer rapidement." -ForegroundColor DarkGray
