param(
    [string]$Reference = (Join-Path $PSScriptRoot '..\docs\design\layout-reference-2026-11-16.png'),
    [string]$Output = (Join-Path $PSScriptRoot '..\app\src\main\res\drawable-nodpi')
)
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
New-Item -ItemType Directory -Force -Path $Output | Out-Null
$source = [System.Drawing.Bitmap]::FromFile((Resolve-Path -LiteralPath $Reference))
try {
    function Save-Crop([string]$Name, [int]$X, [int]$Y, [int]$Width, [int]$Height, [bool]$Frame = $false) {
        $bitmap = New-Object System.Drawing.Bitmap($Width, $Height, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb))
        $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
        try {
            $graphics.DrawImage($source, (New-Object System.Drawing.Rectangle(0,0,$Width,$Height)), $X,$Y,$Width,$Height, [System.Drawing.GraphicsUnit]::Pixel)
            if ($Frame) {
                $graphics.CompositingMode = [System.Drawing.Drawing2D.CompositingMode]::SourceCopy
                $clear = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::Transparent)
                try { $graphics.FillRectangle($clear, 18,18,($Width-36),($Height-36)) } finally { $clear.Dispose() }
            }
            $bitmap.Save((Join-Path $Output $Name), [System.Drawing.Imaging.ImageFormat]::Png)
        } finally { $graphics.Dispose(); $bitmap.Dispose() }
    }
    # Only texture, transparent frame and ornament are reused. All text, cells and controls remain native Compose.
    Save-Crop 'ui_journal_paper.png' 8 150 106 700
    Save-Crop 'ui_workshop_frame.png' 175 247 534 600 $true
    Save-Crop 'ui_repair_brush.png' 822 773 116 87
} finally { $source.Dispose() }
