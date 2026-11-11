param(
    [Parameter(Mandatory = $true)]
    [string]$Destination
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

# Pinned upstream distribution; never download mutable "latest" binaries.
$sourceVersion = "5.0.1"
$sourceUrl = "https://github.com/Genymobile/scrcpy/releases/download/v$sourceVersion/scrcpy-win64-v$sourceVersion.zip"
$expectedSha256 = "b12a2c4ee8be317422451fc7dcf8ee20a71b5ea7ef9ad73ddd825a25316227a5"
$temp = Join-Path ([System.IO.Path]::GetTempPath()) ("wristporter-adb-" + [guid]::NewGuid().ToString("N"))
$archive = Join-Path $temp "scrcpy.zip"
$extract = Join-Path $temp "expanded"

New-Item -ItemType Directory -Force -Path $temp, $extract, $Destination | Out-Null
try {
    Write-Host "Downloading pinned ADB source archive: $sourceUrl"
    Invoke-WebRequest -Uri $sourceUrl -OutFile $archive -MaximumRedirection 8
    $actualSha256 = (Get-FileHash -Path $archive -Algorithm SHA256).Hash.ToLowerInvariant()
    if ($actualSha256 -ne $expectedSha256) {
        throw "ADB source checksum mismatch: refusing to package unverified binaries."
    }

    Expand-Archive -Path $archive -DestinationPath $extract
    $adb = Get-ChildItem -Path $extract -Recurse -File -Filter "adb.exe" | Select-Object -First 1
    if ($null -eq $adb) { throw "Pinned archive does not contain adb.exe" }

    foreach ($file in @("adb.exe", "AdbWinApi.dll", "AdbWinUsbApi.dll")) {
        $source = Join-Path $adb.DirectoryName $file
        if (!(Test-Path -LiteralPath $source -PathType Leaf)) {
            throw "Required bundled ADB component missing: $file"
        }
        Copy-Item -LiteralPath $source -Destination (Join-Path $Destination $file) -Force
    }

    $licenses = Join-Path $Destination "licenses"
    New-Item -ItemType Directory -Force -Path $licenses | Out-Null
    $license = Get-ChildItem -Path $extract -Recurse -File | Where-Object { $_.Name -eq "LICENSE" } | Select-Object -First 1
    $licensePath = Join-Path $licenses "Apache-2.0.txt"
    if ($null -eq $license) {
        Invoke-WebRequest -Uri "https://raw.githubusercontent.com/Genymobile/scrcpy/v$sourceVersion/LICENSE" -OutFile $licensePath
    } else {
        Copy-Item -LiteralPath $license.FullName -Destination $licensePath -Force
    }
    if (!(Test-Path -LiteralPath $licensePath) -or (Get-Item $licensePath).Length -lt 1000) {
        throw "Third-party Apache 2.0 license is missing."
    }

    $provenance = @"
Bundled Android Debug Bridge components:
adb.exe
AdbWinApi.dll
AdbWinUsbApi.dll

Source: official Genymobile scrcpy Windows x64 v$sourceVersion distribution
Source URL: $sourceUrl
Source ZIP SHA-256: $expectedSha256
ADB upstream: https://android.googlesource.com/platform/packages/modules/adb/
scrcpy upstream: https://github.com/Genymobile/scrcpy
License: Apache License 2.0 (licenses/Apache-2.0.txt)

Only ADB components are extracted; the scrcpy GUI/server is not bundled.
"@
    Set-Content -Path (Join-Path $Destination "THIRD_PARTY_NOTICES.txt") -Encoding utf8 -Value $provenance

    & (Join-Path $Destination "adb.exe") version
    if ($LASTEXITCODE -ne 0) { throw "Bundled adb.exe did not pass its version check." }
    Write-Host "Bundled ADB installed at $Destination"
} finally {
    Remove-Item -LiteralPath $temp -Recurse -Force -ErrorAction SilentlyContinue
}
