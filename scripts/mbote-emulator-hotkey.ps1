param(
  [string]$LauncherPath = (Join-Path (Split-Path -Parent $PSScriptRoot) "start-mbote-emulator.bat")
)

$ErrorActionPreference = "Stop"
$logPath = Join-Path $env:TEMP "mbote-emulator-hotkey.log"

if (-not (Test-Path -LiteralPath $LauncherPath)) {
  throw "Lanceur introuvable: $LauncherPath"
}

$signature = @"
using System;
using System.Runtime.InteropServices;

public static class MboteHotkeyNative {
  [DllImport("user32.dll", SetLastError = true)]
  public static extern bool RegisterHotKey(IntPtr hWnd, int id, uint fsModifiers, uint vk);

  [DllImport("user32.dll", SetLastError = true)]
  public static extern bool UnregisterHotKey(IntPtr hWnd, int id);

  [DllImport("user32.dll")]
  public static extern sbyte GetMessage(out MSG lpMsg, IntPtr hWnd, uint wMsgFilterMin, uint wMsgFilterMax);
}

[StructLayout(LayoutKind.Sequential)]
public struct MSG {
  public IntPtr hwnd;
  public uint message;
  public UIntPtr wParam;
  public IntPtr lParam;
  public uint time;
  public int pt_x;
  public int pt_y;
}
"@

Add-Type -TypeDefinition $signature -ErrorAction SilentlyContinue

$hotkeyId = 0x4D42
$modAlt = 0x0001
$vkI = 0x49
$wmHotkey = 0x0312

if (-not [MboteHotkeyNative]::RegisterHotKey([IntPtr]::Zero, $hotkeyId, $modAlt, $vkI)) {
  $lastError = [Runtime.InteropServices.Marshal]::GetLastWin32Error()
  throw "Impossible d'enregistrer Alt + I. Il est probablement deja utilise par une autre application. Code Win32: $lastError"
}

Write-Host "Raccourci MBote actif: Alt + I lance l'emulateur Android." -ForegroundColor Green
Write-Host "Laissez cette fenetre/processus actif, ou installez le raccourci au demarrage Windows." -ForegroundColor DarkGray
Add-Content -LiteralPath $logPath -Value "$(Get-Date -Format o) Alt+I watcher actif"

try {
  while ($true) {
    $msg = New-Object MSG
    $result = [MboteHotkeyNative]::GetMessage([ref]$msg, [IntPtr]::Zero, 0, 0)
    if ($result -eq 0) { break }
    if ($msg.message -eq $wmHotkey -and $msg.wParam.ToUInt32() -eq $hotkeyId) {
      Add-Content -LiteralPath $logPath -Value "$(Get-Date -Format o) Alt+I recu, lancement de $LauncherPath"
      Start-Process -FilePath $LauncherPath -WorkingDirectory (Split-Path -Parent $LauncherPath)
    }
  }
} finally {
  [MboteHotkeyNative]::UnregisterHotKey([IntPtr]::Zero, $hotkeyId) | Out-Null
}
