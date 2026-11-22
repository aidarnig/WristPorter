# Packages an isolated, no-install Windows/iPhone connectivity probe for Issue #39.
# Test-only: no photos, no ADB, no watch management, no firewall changes.
$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
Push-Location $root
try {
    & mvn -B -ntp -pl watch-web -am test
    if ($LASTEXITCODE -ne 0) { throw 'watch-web tests failed: no artifact will be produced.' }

    $dist = Join-Path $root 'dist'
    $probe = Join-Path $dist 'web-gateway-probe'
    New-Item -ItemType Directory -Force $dist | Out-Null
    if (Test-Path $probe) { Remove-Item $probe -Recurse -Force }
    New-Item -ItemType Directory -Force $probe | Out-Null

    & jlink --add-modules java.base,jdk.httpserver --strip-debug --no-header-files --no-man-pages --output (Join-Path $probe 'runtime')
    if ($LASTEXITCODE -ne 0) { throw 'jlink failed.' }

    $package = 'io/github/aidarnig/wgs/web'
    $classes = Join-Path $probe ('classes/' + $package)
    New-Item -ItemType Directory -Force $classes | Out-Null
    $mainClasses = Join-Path $root ('watch-web/target/classes/' + $package)
    $testClasses = Join-Path $root ('watch-web/target/test-classes/' + $package)
    $gatewayClasses = @(Get-ChildItem -LiteralPath $mainClasses -Filter 'LanUploadGateway*.class' -File)
    if ($gatewayClasses.Count -lt 2) { throw 'Expected gateway classes were not compiled.' }
    foreach ($source in $gatewayClasses) { Copy-Item $source.FullName $classes }
    $probeMain = Join-Path $testClasses 'GatewayProbeMain.class'
    if (-not (Test-Path $probeMain)) { throw 'Test-only entry point not compiled.' }
    Copy-Item $probeMain $classes
    Copy-Item (Join-Path $root 'ci/start-web-gateway-probe.cmd') (Join-Path $probe 'Start-GatewayProbe.cmd')

    @'
WRISTPORTER #39 - TEST-ONLY WINDOWS/iPHONE LAN GATEWAY PROBE
NOT A RELEASE. NOT FOR PUBLIC WIFI. NO UPLOAD OR ADB FEATURES.

1. Extract the complete ZIP somewhere on Windows.
2. Open a PowerShell window in the extracted directory.
3. Find your own PC's Wi-Fi IPv4 address using ipconfig.
4. On your own trusted private Wi-Fi only, run:
       .\Start-GatewayProbe.cmd 192.168.1.123
   Replace the example IPv4 with your PC's address.
5. Read the plaintext HTTP warning. Only if you agree, type I UNDERSTAND.
6. From iPhone Safari on the same private Wi-Fi, open the printed URL.
7. Type the one-time PIN displayed only on Windows and submit within 2 minutes.
   Expected: Paired (test only). No photos/files should be accessible.
8. Press ENTER in the Windows console to stop; Safari reload should fail.

If the firewall asks, permit PRIVATE only, never PUBLIC. Do not change
Windows Defender Firewall settings globally, create port forwarding, use
guest/hotel/public networks or share the PIN, cookie or private IP.

This is a pre-release plain-HTTP security test: traffic can be intercepted
and modified on the LAN. Close immediately if you see unexpected behavior.
'@ | Set-Content -LiteralPath (Join-Path $probe 'README-TEST-ONLY.txt') -Encoding utf8

    & (Join-Path $probe 'runtime/bin/java.exe') --list-modules | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'Bundled runtime smoke-check failed.' }

    $zip = Join-Path $dist 'WristPorter-W14B-GatewayProbe-Windows-x64.zip'
    if (Test-Path $zip) { Remove-Item $zip -Force }
    Compress-Archive -Path (Join-Path $probe '*') -DestinationPath $zip -CompressionLevel Optimal
    $digest = (Get-FileHash -Algorithm SHA256 -LiteralPath $zip).Hash.ToLowerInvariant()
    "$digest  $(Split-Path $zip -Leaf)" | Set-Content -LiteralPath ($zip + '.sha256') -Encoding ascii
    Write-Host "Probe ZIP: $zip"
    Write-Host "SHA256: $digest"
} finally {
    Pop-Location
}
