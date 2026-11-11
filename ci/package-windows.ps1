$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$root = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$desktop = Join-Path $root "watch-desktop"
$inputDir = Join-Path $desktop "target\jpackage-input"
$dist = Join-Path $root "dist"
$appName = "WristPorter"
$version = "1.1.0"
[xml]$projectPom = Get-Content -LiteralPath (Join-Path $root "pom.xml")
if ($projectPom.project.version -ne $version) { throw "Package version must match Maven version." }

Push-Location $root
try {
    mvn -B -ntp verify
    if ($LASTEXITCODE -ne 0) { throw "Full Maven reactor tests failed." }

    mvn -B -ntp -DskipTests install
    if ($LASTEXITCODE -ne 0) { throw "Reactor installation failed." }

    New-Item -ItemType Directory -Force -Path $inputDir, $dist | Out-Null
    Get-ChildItem -Path $inputDir -Filter "*.jar" -ErrorAction SilentlyContinue | Remove-Item -Force

    mvn -B -ntp -pl watch-desktop dependency:copy-dependencies "-DincludeScope=runtime" "-DoutputDirectory=$inputDir"
    if ($LASTEXITCODE -ne 0) { throw "Could not collect runtime dependencies." }

    $mainJar = "watch-desktop-$version.jar"
    $mainPath = Join-Path $desktop "target\$mainJar"
    if (!(Test-Path -LiteralPath $mainPath)) { throw "Desktop JAR missing: $mainPath" }
    Copy-Item -LiteralPath $mainPath -Destination (Join-Path $inputDir $mainJar) -Force

    $appImage = Join-Path $dist $appName
    if (Test-Path $appImage) { Remove-Item $appImage -Recurse -Force }

    & (Join-Path $env:JAVA_HOME "bin\jpackage.exe") --type app-image --icon (Join-Path $root "docs\assets\wristporter.ico") --name $appName --input $inputDir --main-jar $mainJar --main-class io.github.aidarnig.wgs.desktop.DesktopLauncher --app-version $version --dest $dist --java-options "-Dfile.encoding=UTF-8"
    if ($LASTEXITCODE -ne 0) { throw "jpackage failed." }

    foreach ($entry in @("$appName.exe", "runtime", "app")) {
        if (!(Test-Path (Join-Path $appImage $entry))) { throw "App image missing $entry" }
    }

    # Download ADB only at packaging time; end users need no separate installation.
    & (Join-Path $PSScriptRoot "bundle-adb.ps1") -Destination (Join-Path $appImage "platform-tools")

    foreach ($component in @("adb.exe", "AdbWinApi.dll", "AdbWinUsbApi.dll", "THIRD_PARTY_NOTICES.txt", "licenses\Apache-2.0.txt")) {
        $componentPath = Join-Path (Join-Path $appImage "platform-tools") $component
        if (!(Test-Path -LiteralPath $componentPath)) { throw "Bundled ADB component missing: $component" }
    }
    Copy-Item -LiteralPath (Join-Path $root "LICENSE") -Destination (Join-Path $appImage "LICENSE.txt") -Force
    Copy-Item -LiteralPath (Join-Path $root "docs\QUICK_START.md") -Destination (Join-Path $appImage "START_HERE.md") -Force
    Copy-Item -LiteralPath (Join-Path $root "CHANGELOG.md") -Destination (Join-Path $appImage "RELEASE_NOTES.md") -Force

    $filename = "$appName-$version-windows-x64.zip"
    $zip = Join-Path $dist $filename
    if (Test-Path $zip) { Remove-Item $zip -Force }
    Compress-Archive -Path $appImage -DestinationPath $zip -CompressionLevel Optimal

    $checksum = (Get-FileHash -Algorithm SHA256 $zip).Hash.ToLowerInvariant()
    Set-Content -Path "$zip.sha256" -Encoding ascii -Value "$checksum  $filename"
    Write-Host "Created WristPorter stable portable package: $zip"
} finally {
    Pop-Location
}
