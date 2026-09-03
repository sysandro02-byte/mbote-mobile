$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
$targetBat = Join-Path $projectRoot "start-mbote-capture-sync.bat"
$startupFolder = [Environment]::GetFolderPath("Startup")
$shortcutPath = Join-Path $startupFolder "MBote Capture Sync.lnk"

if (-not (Test-Path -LiteralPath $targetBat)) {
  throw "Lanceur de synchronisation introuvable: $targetBat"
}

$shell = New-Object -ComObject WScript.Shell
$shortcut = $shell.CreateShortcut($shortcutPath)
$shortcut.TargetPath = $targetBat
$shortcut.WorkingDirectory = $projectRoot
$shortcut.Description = "Synchronise automatiquement les captures de l'emulateur MBote vers le dossier captures"
$shortcut.Save()

Write-Host "Synchronisation automatique au demarrage creee: $shortcutPath" -ForegroundColor Green
