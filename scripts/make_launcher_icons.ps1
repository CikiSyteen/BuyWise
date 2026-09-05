# Generate BuyWise adaptive-icon foreground PNGs from image.png
# Crop white margin, center content at 78% of the 108dp canvas, transparent bg
Set-StrictMode -Version 2
Add-Type -AssemblyName System.Drawing

$ErrorActionPreference = "Stop"
$srcPath = "D:\Code\MyProjects\buywise\image.png"
$outRoot = "D:\Code\MyProjects\buywise\app\src\main\res"

$src = New-Object System.Drawing.Bitmap($srcPath)

# Crop outer white margin (scanned card bounds: 45..1233)
$cropX = 48
$cropY = 48
$cropSize = 1182
$cropRect = New-Object System.Drawing.Rectangle($cropX, $cropY, $cropSize, $cropSize)
$cropped = $src.Clone($cropRect, $src.PixelFormat)

# density -> 108dp canvas pixels
$densities = [ordered]@{
    "mipmap-mdpi"    = 108
    "mipmap-hdpi"    = 162
    "mipmap-xhdpi"   = 216
    "mipmap-xxhdpi"  = 324
    "mipmap-xxxhdpi" = 432
}

$contentRatio = 0.78

foreach ($entry in $densities.GetEnumerator()) {
    $dir = Join-Path $outRoot $entry.Key
    if (-not (Test-Path $dir)) { New-Item -ItemType Directory -Path $dir | Out-Null }

    $size = $entry.Value
    $bmp = New-Object System.Drawing.Bitmap($size, $size)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
    $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    $g.Clear([System.Drawing.Color]::Transparent)

    $inner = [int]($size * $contentRatio)
    $offset = [int](($size - $inner) / 2)
    $g.DrawImage($cropped, $offset, $offset, $inner, $inner)
    $g.Dispose()

    $outFile = Join-Path $dir "ic_launcher_foreground.png"
    $bmp.Save($outFile, [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
    Write-Output ("written: " + $outFile + " (" + $size + "px)")
}

$cropped.Dispose()
$src.Dispose()
Write-Output "done"
