param(
  [string]$AvdName = "mbote_api26",
  [string]$AndroidSdkRoot = "D:\Android\Sdk",
  [string]$AndroidAvdHome = "D:\Android\Avd",
  [switch]$InstallDebugApk
)

$ErrorActionPreference = "Stop"

$emulatorExe = Join-Path $AndroidSdkRoot "emulator\emulator.exe"
$adbExe = Join-Path $AndroidSdkRoot "platform-tools\adb.exe"
$apkPath = Join-Path (Split-Path -Parent $PSScriptRoot) "app\build\outputs\apk\debug\app-debug.apk"

$env:ANDROID_SDK_ROOT = $AndroidSdkRoot
$env:ANDROID_HOME = $AndroidSdkRoot
$env:ANDROID_AVD_HOME = $AndroidAvdHome

function Show-MboteEmulatorWindow {
  $signature = @"
using System;
using System.Runtime.InteropServices;

public static class MboteWindowNative {
  [DllImport("user32.dll")]
  public static extern bool ShowWindowAsync(IntPtr hWnd, int nCmdShow);

  [DllImport("user32.dll")]
  public static extern bool SetForegroundWindow(IntPtr hWnd);
}
"@

  Add-Type -TypeDefinition $signature -ErrorAction SilentlyContinue
  $restoreWindow = 9
  $emulatorWindow = Get-Process emulator, qemu-system-x86_64 -ErrorAction SilentlyContinue |
    Where-Object { $_.MainWindowHandle -ne 0 } |
    Select-Object -First 1

  if ($emulatorWindow) {
    [MboteWindowNative]::ShowWindowAsync($emulatorWindow.MainWindowHandle, $restoreWindow) | Out-Null
    [MboteWindowNative]::SetForegroundWindow($emulatorWindow.MainWindowHandle) | Out-Null
  }
}

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
  Show-MboteEmulatorWindow
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
Show-MboteEmulatorWindow
