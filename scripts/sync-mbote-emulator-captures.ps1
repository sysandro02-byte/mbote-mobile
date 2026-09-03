param(
  [string]$AndroidSdkRoot = "D:\Android\Sdk",
  [string]$OutputDir = (Join-Path (Split-Path -Parent $PSScriptRoot) "captures"),
  [int]$PollSeconds = 2,
  [switch]$ImportExisting
)

$ErrorActionPreference = "Stop"

$adbExe = Join-Path $AndroidSdkRoot "platform-tools\adb.exe"
if (-not (Test-Path -LiteralPath $adbExe)) {
  throw "adb.exe introuvable: $adbExe"
}

New-Item -ItemType Directory -Path $OutputDir -Force | Out-Null
$logPath = Join-Path $OutputDir "mbote-capture-sync.log"

function Write-SyncLog {
  param([string]$Message)
  $line = "$(Get-Date -Format o) $Message"
  Add-Content -LiteralPath $logPath -Value $line
  Write-Host $line
}

function Get-SafeFileName {
  param([string]$Name)
  $invalid = [IO.Path]::GetInvalidFileNameChars()
  foreach ($char in $invalid) {
    $Name = $Name.Replace($char, "_")
  }
  return $Name
}

function Wait-FileReady {
  param([string]$Path)
  for ($i = 0; $i -lt 20; $i++) {
    try {
      $stream = [IO.File]::Open($Path, [IO.FileMode]::Open, [IO.FileAccess]::Read, [IO.FileShare]::Read)
      $stream.Close()
      return $true
    } catch {
      Start-Sleep -Milliseconds 250
    }
  }
  return $false
}

function Copy-HostScreenshot {
  param([string]$Path)
  if (-not (Test-Path -LiteralPath $Path)) { return }
  $name = [IO.Path]::GetFileName($Path)
  if ($name -notmatch "(?i)(screenshot|screen|emulator|mbote)") { return }
  if (-not (Wait-FileReady -Path $Path)) { return }

  $timestamp = Get-Date -Format "yyyyMMdd-HHmmss-fff"
  $safeName = Get-SafeFileName -Name $name
  $target = Join-Path $OutputDir "host-$timestamp-$safeName"
  Copy-Item -LiteralPath $Path -Destination $target -Force
  Write-SyncLog "Capture Windows copiee: $target"
}

function Get-RemoteScreenshots {
  $device = (& $adbExe devices | Select-String -Pattern "\s+device\b" | Select-Object -First 1)
  if (-not $device) { return @() }

  $remoteCommand = "for d in /sdcard/Pictures/Screenshots /sdcard/DCIM/Screenshots /sdcard/Pictures /sdcard/DCIM; do [ -d `$d ] && find `$d -maxdepth 1 -type f; done"
  $files = & $adbExe shell $remoteCommand 2>$null
  return @($files | ForEach-Object { $_.Trim() } | Where-Object { $_ -match "(?i)\.(png|jpg|jpeg)$" })
}

function Pull-RemoteScreenshot {
  param([string]$RemotePath)
  $timestamp = Get-Date -Format "yyyyMMdd-HHmmss-fff"
  $safeName = Get-SafeFileName -Name ([IO.Path]::GetFileName($RemotePath))
  $target = Join-Path $OutputDir "emulator-$timestamp-$safeName"
  & $adbExe pull $RemotePath $target | Out-Null
  if (Test-Path -LiteralPath $target) {
    Write-SyncLog "Capture emulateur copiee: $target"
  }
}

$knownRemote = [System.Collections.Generic.HashSet[string]]::new()
if (-not $ImportExisting) {
  foreach ($file in Get-RemoteScreenshots) {
    [void]$knownRemote.Add($file)
  }
}

$hostFolders = @(
  (Join-Path $env:USERPROFILE "Pictures"),
  (Join-Path $env:USERPROFILE "Pictures\Screenshots"),
  (Join-Path $env:USERPROFILE "Desktop"),
  (Join-Path $env:USERPROFILE "Downloads")
) | Where-Object { Test-Path -LiteralPath $_ } | Select-Object -Unique

$watchers = @()
foreach ($folder in $hostFolders) {
  foreach ($filter in @("*.png", "*.jpg", "*.jpeg")) {
    $watcher = New-Object IO.FileSystemWatcher $folder, $filter
    $watcher.IncludeSubdirectories = $false
    $watcher.EnableRaisingEvents = $true
    Register-ObjectEvent -InputObject $watcher -EventName Created -Action {
      Copy-HostScreenshot -Path $Event.SourceEventArgs.FullPath
    } | Out-Null
    $watchers += $watcher
  }
}

Write-SyncLog "Synchronisation captures MBote active vers $OutputDir"
Write-SyncLog "Dossiers Windows surveilles: $($hostFolders -join ', ')"
Write-SyncLog "Dossiers Android surveilles: /sdcard/Pictures, /sdcard/DCIM"

while ($true) {
  foreach ($file in Get-RemoteScreenshots) {
    if ($knownRemote.Add($file)) {
      Pull-RemoteScreenshot -RemotePath $file
    }
  }
  Start-Sleep -Seconds $PollSeconds
}
