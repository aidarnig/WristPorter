<#
.SYNOPSIS
  Explicitly install a local test APK to one selected, authorized Wi-Fi ADB watch.
.DESCRIPTION
  No discovery scans, pairing, unpairing, factory reset, shell commands, or
  unattended actions. An interactive INSTALL confirmation is always required.
  This script is NOT run by CI and does NOT make the APK trusted/signed.
.EXAMPLE
  .\ci\install-watch-test-apk.ps1 -ApkPath .\app-debug.apk
.EXAMPLE
  .\ci\install-watch-test-apk.ps1 -ApkPath .\app-debug.apk -AdbPath .\platform-tools\adb.exe
#>
[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)][string]$ApkPath,
    [string]$AdbPath = '',
    [string]$Serial = ''
)
$ErrorActionPreference = 'Stop'
$resolvedApk = (Resolve-Path -LiteralPath $ApkPath -ErrorAction Stop).Path
if ([IO.Path]::GetExtension($resolvedApk).ToLowerInvariant() -ne '.apk') {
    throw 'Specify an existing .apk file.'
}
if (-not $AdbPath) {
    $adbCommand = Get-Command adb -ErrorAction SilentlyContinue
    if ($adbCommand) { $AdbPath = $adbCommand.Source }
    else {
        $candidates = @(
            (Join-Path (Get-Location).Path 'platform-tools\adb.exe'),
            (Join-Path (Get-Location).Path 'WristPorter\platform-tools\adb.exe')
        )
        foreach ($candidate in $candidates) {
            if (Test-Path -LiteralPath $candidate -PathType Leaf) { $AdbPath = $candidate; break }
        }
    }
}
if (-not $AdbPath) { throw 'ADB not found. Supply -AdbPath pointing to the bundled adb.exe.' }
$resolvedAdb = (Resolve-Path -LiteralPath $AdbPath -ErrorAction Stop).Path
$lines = @(& $resolvedAdb devices)
if ($LASTEXITCODE -ne 0) { throw 'adb devices failed. Check ADB and watch authorization.' }
$authorized = @($lines | ForEach-Object {
    if ($_ -match '^(\S+)\s+device\s*$') { $Matches[1] }
} | Where-Object { $_ })
if ($authorized.Count -eq 0) { throw 'No authorized watch found. Connect through WristPorter first.' }
if (-not $Serial) {
    if ($authorized.Count -ne 1) {
        throw 'Multiple authorized ADB devices. Re-run with -Serial copied locally from adb devices.'
    }
    $Serial = $authorized[0]
}
if ($Serial -notin $authorized) { throw 'Specified -Serial is not an authorized ADB device.' }
Write-Host 'One authorized ADB device selected. Confirm on your watch before continuing.'
Write-Host 'This operation will install or update the explicitly selected test APK.'
$confirmation = Read-Host 'Type INSTALL to continue (anything else cancels)'
if ($confirmation -cne 'INSTALL') { Write-Host 'Cancelled: no installation attempted.'; return }
& $resolvedAdb -s $Serial install -r $resolvedApk
if ($LASTEXITCODE -ne 0) { throw 'APK installation failed; inspect the ADB error above.' }
Write-Host 'ADB reported success. Now follow the assigned Issue test scenario on the watch.'
