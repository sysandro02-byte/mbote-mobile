param(
  [string]$AndroidSdkRoot = "D:\Android\Sdk",
  [string]$OutputDir = (Join-Path (Split-Path -Parent $PSScriptRoot) "captures")
)

$ErrorActionPreference = "Stop"

$adbExe = Join-Path $AndroidSdkRoot "platform-tools\adb.exe"
if (-not (Test-Path -LiteralPath $adbExe)) {
  throw "adb.exe introuvable: $adbExe"
}

$device = (& $adbExe devices | Select-String -Pattern "\s+device\b" | Select-Object -First 1)
if (-not $device) {
  throw "Aucun emulateur/appareil Android detecte. Lance d'abord l'emulateur avec Alt + I."
}

New-Item -ItemType Directory -Path $OutputDir -Force | Out-Null
$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$outputPath = Join-Path $OutputDir "mbote-emulator-$timestamp.png"

& cmd.exe /c "`"$adbExe`" exec-out screencap -p > `"$outputPath`""
Write-Host $outputPath
