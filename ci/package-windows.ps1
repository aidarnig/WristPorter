$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$root = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$desktop = Join-Path $root "watch-desktop"
$inputDir = Join-Path $desktop "target\jpackage-input"
$dist = Join-Path $root "dist"
$version = "0.1.0"

Push-Location $root
try {
    mvn -B -ntp verify
    if ($LASTEXITCODE -ne 0) { throw "Full reactor tests failed." }

    mvn -B -ntp -DskipTests install
    if ($LASTEXITCODE -ne 0) { throw "Reactor installation failed." }

    New-Item -ItemType Directory -Force -Path $inputDir, $dist | Out-Null
    Get-ChildItem $inputDir -Filter "*.jar" -ErrorAction SilentlyContinue | Remove-Item -Force

    mvn -B -ntp -pl watch-desktop dependency:copy-dependencies "-DincludeScope=runtime" "-DoutputDirectory=$inputDir"
    if ($LASTEXITCODE -ne 0) { throw "Failed to collect runtime dependencies." }

    $mainJar = "watch-desktop-$version-SNAPSHOT.jar"
    $mainPath = Join-Path $desktop "target\$mainJar"
    if (!(Test-Path $mainPath)) { throw "Desktop application jar not found: $mainPath" }
    Copy-Item $mainPath (Join-Path $inputDir $mainJar) -Force

    $appImage = Join-Path $dist "WatchGallerySync"
    if (Test-Path $appImage) { Remove-Item $appImage -Recurse -Force }

    & (Join-Path $env:JAVA_HOME "bin\jpackage.exe") --type app-image --name WatchGallerySync --input $inputDir --main-jar $mainJar --main-class io.github.aidarnig.wgs.desktop.DesktopLauncher --app-version $version --dest $dist --java-options "-Dfile.encoding=UTF-8"
    if ($LASTEXITCODE -ne 0) { throw "jpackage failed." }

    $exe = Join-Path $appImage "WatchGallerySync.exe"
    if (!(Test-Path $exe)) { throw "Expected executable missing from app image." }

    $zip = Join-Path $dist "WatchGallerySync-windows-x64.zip"
    if (Test-Path $zip) { Remove-Item $zip -Force }
    Compress-Archive -Path $appImage -DestinationPath $zip -CompressionLevel Optimal
    $checksum = (Get-FileHash -Algorithm SHA256 $zip).Hash.ToLowerInvariant()
    Set-Content -Path "$zip.sha256" -Encoding ascii -Value "$checksum  WatchGallerySync-windows-x64.zip"
    Write-Host "Created $zip"
} finally {
    Pop-Location
}
