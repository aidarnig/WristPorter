param(
    [Parameter(Mandatory = $true)]
    [string]$Exe,
    [Parameter(Mandatory = $true)]
    [string]$ExpectedIcon
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest
Add-Type -AssemblyName System.Drawing

# Ensure jpackage did not accidentally leave its stock launcher icon.
$expected = [System.Drawing.Icon]::new($ExpectedIcon, [System.Drawing.Size]::new(32, 32))
$actual = [System.Drawing.Icon]::ExtractAssociatedIcon($Exe)
if ($null -eq $actual) { throw "No embedded Windows executable icon: $Exe" }

$srcBitmap = $expected.ToBitmap()
$exeBitmap = $actual.ToBitmap()
try {
    function BluePixelCount([System.Drawing.Bitmap]$bitmap) {
        $count = 0
        for ($y = 0; $y -lt $bitmap.Height; $y++) {
            for ($x = 0; $x -lt $bitmap.Width; $x++) {
                $pixel = $bitmap.GetPixel($x, $y)
                if ($pixel.A -gt 100 -and $pixel.B -gt 105 -and
                    $pixel.B -gt $pixel.R * 1.3 -and
                    $pixel.B -gt $pixel.G * 1.07) {
                    $count++
                }
            }
        }
        return $count
    }

    $expectedBlue = BluePixelCount $srcBitmap
    $actualBlue = BluePixelCount $exeBitmap
    Write-Host "Source watch-icon blue pixels: $expectedBlue"
    Write-Host "Packaged EXE icon blue pixels: $actualBlue"
    if ($expectedBlue -lt 12) { throw "Source ICO does not visibly contain the blue WP watch mark." }
    if ($actualBlue -lt [Math]::Max(12, [Math]::Floor($expectedBlue * 0.3))) {
        throw "Packaged EXE appears to have a generic icon instead of the blue WP watch mark."
    }
} finally {
    $srcBitmap.Dispose()
    $exeBitmap.Dispose()
    $expected.Dispose()
    $actual.Dispose()
}
