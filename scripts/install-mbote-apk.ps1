param(
  [string]$AndroidSdkRoot = "D:\Android\Sdk",
  [string]$ApkPath = (Join-Path (Split-Path -Parent $PSScriptRoot) "app\build\outputs\apk\debug\app-debug.apk")
)

$ErrorActionPreference = "Stop"

$adbExe = Join-Path $AndroidSdkRoot "platform-tools\adb.exe"
if (-not (Test-Path -LiteralPath $adbExe)) {
  throw "adb.exe introuvable: $adbExe"
}

if (-not (Test-Path -LiteralPath $ApkPath)) {
  throw "APK debug introuvable: $ApkPath. Compile d'abord l'application avec assembleDebug."
}

$device = (& $adbExe devices | Select-String -Pattern "\s+device\b" | Select-Object -First 1)
if (-not $device) {
  throw "Aucun emulateur/appareil Android detecte. Lance d'abord l'emulateur avec Alt + I ou start-mbote-emulator.bat."
}

Write-Host "Installation de MBote sur $($device.Line.Trim())..." -ForegroundColor Cyan
& $adbExe install -r $ApkPath
Write-Host "MBote installe. Ouvre l'application depuis le launcher Android." -ForegroundColor Green
