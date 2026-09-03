$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
$targetBat = Join-Path $projectRoot "start-mbote-emulator-hotkey.bat"
$startupFolder = [Environment]::GetFolderPath("Startup")
$shortcutPath = Join-Path $startupFolder "MBote Emulator Hotkey.lnk"

if (-not (Test-Path -LiteralPath $targetBat)) {
  throw "Lanceur hotkey introuvable: $targetBat"
}

$shell = New-Object -ComObject WScript.Shell
$shortcut = $shell.CreateShortcut($shortcutPath)
$shortcut.TargetPath = $targetBat
$shortcut.WorkingDirectory = $projectRoot
$shortcut.Description = "Active Ctrl + I pour lancer l'emulateur Android MBote"
$shortcut.Save()

Write-Host "Raccourci de demarrage cree: $shortcutPath" -ForegroundColor Green
Write-Host "Ctrl + I sera actif apres ouverture de session Windows." -ForegroundColor Green
