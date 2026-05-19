# Generates 16x16 vanilla-style block textures for VoltCraft.
# Run: pwsh -File scripts/generate_textures.ps1
#
# Outputs to src/main/resources/assets/voltcraft/textures/block/
#
# Design goals:
#  * Resolution matches vanilla (16x16) so blocks visually match the rest of the world.
#  * Palette borrows from real vanilla blocks: coal-black rubber, diamond blue, vanilla orange/red.
#  * Cables use a black rubber tube with two tier-colored ID rings; only the central band
#    (texture rows y=6..9) is visible on the in-world thin-arm model.

Add-Type -AssemblyName System.Drawing

$root = Split-Path -Parent $PSScriptRoot
$out  = Join-Path $root 'src/main/resources/assets/voltcraft/textures/block'
New-Item -ItemType Directory -Force -Path $out | Out-Null

$SIZE = 16

function New-Bmp {
    $bmp = New-Object System.Drawing.Bitmap $SIZE, $SIZE, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    return $bmp
}

function Save-Bmp([System.Drawing.Bitmap]$bmp, [string]$name) {
    $path = Join-Path $out "$name.png"
    $bmp.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
    Write-Host "wrote $name.png"
}

function Hex([string]$h) {
    $h = $h.TrimStart('#')
    if ($h.Length -eq 6) { $h = "FF$h" }
    [System.Drawing.Color]::FromArgb([Convert]::ToInt32($h.Substring(0,2),16),
                                     [Convert]::ToInt32($h.Substring(2,2),16),
                                     [Convert]::ToInt32($h.Substring(4,2),16),
                                     [Convert]::ToInt32($h.Substring(6,2),16))
}

function Fill([System.Drawing.Bitmap]$bmp, [System.Drawing.Color]$c) {
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $brush = New-Object System.Drawing.SolidBrush $c
    $g.FillRectangle($brush, 0, 0, $SIZE, $SIZE)
    $brush.Dispose()
    $g.Dispose()
}

function Rect([System.Drawing.Bitmap]$bmp, [int]$x, [int]$y, [int]$w, [int]$h, [System.Drawing.Color]$c) {
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $brush = New-Object System.Drawing.SolidBrush $c
    $g.FillRectangle($brush, $x, $y, $w, $h)
    $brush.Dispose()
    $g.Dispose()
}

function Pixel([System.Drawing.Bitmap]$bmp, [int]$x, [int]$y, [System.Drawing.Color]$c) {
    if ($x -ge 0 -and $x -lt $SIZE -and $y -ge 0 -and $y -lt $SIZE) {
        $bmp.SetPixel($x, $y, $c)
    }
}

# Vanilla-aligned palette
$rubberHi   = Hex '2C2C2C'   # rubber highlight
$rubberMid  = Hex '161616'   # rubber body (close to coal block)
$rubberLo   = Hex '050505'   # rubber shadow
$voidBlack  = Hex '000000'

# Tier colors — picked to read clearly at distance on a 16x16 flat tube,
# while not colliding with common vanilla blocks of the same hue.
$tierColors = @{
    'low'        = @{ main = Hex 'C0C0C0'; accent = Hex 'E8E8E8'; lo = Hex '6F6F6F' }   # iron grey (220V)
    'medium'     = @{ main = Hex '3F76E4'; accent = Hex '6E9EFF'; lo = Hex '274BAA' }   # diamond blue (10kV)
    'high'       = @{ main = Hex 'F9801D'; accent = Hex 'FFB155'; lo = Hex 'B45A0E' }   # vanilla orange (35kV)
    'extra_high' = @{ main = Hex 'B02E26'; accent = Hex 'E94B3F'; lo = Hex '7A1B16' }   # vanilla red (110kV)
}

# === CABLES ===
# Texture is split into three UV-sampled regions matching the arm/core models:
#   * Side strips (along arm length): black rubber sheath with a tier-colored
#     identification line running parallel to the cable axis. Multi-arm cables
#     stay seamless because every arm samples the same axial slice.
#   * Cross-section (4x4 center): the visible "cable end" — three insulated
#     conductors (hot/neutral/ground) embedded in black rubber.
# Outside the strips the texture is filled with rubber so the inventory cube_all
# preview reads as "a piece of cable" (rubber background with a tier cross).
function Make-Cable($tier) {
    $c = $tierColors[$tier]
    $bmp = New-Bmp

    # All-black rubber base (the insulation jacket)
    Fill $bmp $rubberMid

    # Horizontal sheath band y=6..9 (east/west arm sides sample x=0..5)
    Rect $bmp 0 6 16 1 $rubberHi    # top highlight
    Rect $bmp 0 7 16 1 $c.main      # tier-color identification line
    Rect $bmp 0 8 16 1 $rubberMid   # body
    Rect $bmp 0 9 16 1 $rubberLo    # bottom shadow

    # Vertical sheath band x=6..9 (up/down arm sides sample y=0..5)
    Rect $bmp 6 0 1 16 $rubberHi
    Rect $bmp 7 0 1 16 $c.main
    Rect $bmp 8 0 1 16 $rubberMid
    Rect $bmp 9 0 1 16 $rubberLo

    # Cross-section panel x=6..9, y=6..9 (overrides the strip intersection)
    # Black rubber, with three conductor strands packed in the corners.
    Rect $bmp 6 6 4 4 $rubberLo
    Pixel $bmp 7 7 (Hex 'EF4444')   # hot — red
    Pixel $bmp 8 7 (Hex '3F76E4')   # neutral — blue
    Pixel $bmp 7 8 (Hex 'EAB308')   # ground — yellow
    Pixel $bmp 8 8 $c.accent        # tier indicator dot

    Save-Bmp $bmp "${tier}_voltage_cable"
}
foreach ($t in @('low','medium','high','extra_high')) { Make-Cable $t }

# === TRANSFORMER ===
$tBody = Hex '6B6B6B'   # cobblestone-ish grey
$tHi   = Hex '8C8C8C'
$tLo   = Hex '3E3E3E'
$tDark = Hex '1A1A1A'

function Bevel([System.Drawing.Bitmap]$bmp, [System.Drawing.Color]$hi, [System.Drawing.Color]$lo) {
    Rect $bmp 0 0 16 1 $hi
    Rect $bmp 0 0 1 16 $hi
    Rect $bmp 0 15 16 1 $lo
    Rect $bmp 15 0 1 16 $lo
}

function Make-TransformerFront($tier) {
    $c = $tierColors[$tier]
    $bmp = New-Bmp
    Fill $bmp $tBody
    Bevel $bmp $tHi $tLo

    # Tier band along the top
    Rect $bmp 2 2 12 1 $c.accent
    Rect $bmp 2 3 12 2 $c.main
    Rect $bmp 2 5 12 1 $c.lo

    # Recessed lightning panel
    Rect $bmp 4 7 8 6 $tDark
    Rect $bmp 5 8 6 4 (Hex '262626')

    # Lightning bolt (5 pixels, vanilla style)
    $bolt = Hex 'FFE066'
    Pixel $bmp 8 8 $bolt
    Pixel $bmp 7 9 $bolt
    Pixel $bmp 8 9 $bolt
    Pixel $bmp 8 10 $bolt
    Pixel $bmp 9 10 $bolt
    Pixel $bmp 7 11 $bolt

    # Output port at bottom edge
    Rect $bmp 6 13 4 2 $tDark
    Pixel $bmp 7 14 $c.main
    Pixel $bmp 8 14 $c.main

    Save-Bmp $bmp "${tier}_voltage_transformer_front"
}

function Make-TransformerSide($tier) {
    $c = $tierColors[$tier]
    $bmp = New-Bmp
    Fill $bmp $tBody
    Bevel $bmp $tHi $tLo

    # Tier band top
    Rect $bmp 2 2 12 1 $c.accent
    Rect $bmp 2 3 12 1 $c.main

    # Cooling fins: 4 vertical bars
    for ($x = 3; $x -lt 14; $x += 3) {
        Rect $bmp $x 5 1 9 $tLo
        Rect $bmp ($x + 1) 5 1 9 $tDark
    }

    Save-Bmp $bmp "${tier}_voltage_transformer_side"
}

# Transformer back = low-voltage FE input port (no tier band — accepts any FE)
function Make-TransformerBack($tier) {
    $bmp = New-Bmp
    Fill $bmp $tBody
    Bevel $bmp $tHi $tLo

    # "FE IN" copper port: large recessed square
    Rect $bmp 3 3 10 10 $tDark
    Rect $bmp 4 4 8 8 (Hex 'B87333')      # copper plate
    Rect $bmp 5 5 6 6 (Hex 'D69453')      # copper highlight
    # 4 hex-bolt corners
    foreach ($p in @(@(4,4), @(11,4), @(4,11), @(11,11))) {
        Pixel $bmp $p[0] $p[1] (Hex '8A4D1A')
    }
    # Center "+" jack
    Pixel $bmp 7 8 $tDark
    Pixel $bmp 8 8 $tDark
    Pixel $bmp 7 7 $tDark
    Pixel $bmp 8 7 $tDark
    Save-Bmp $bmp "${tier}_voltage_transformer_back"
}

foreach ($t in @('low','medium','high','extra_high')) {
    Make-TransformerFront $t
    Make-TransformerSide $t
    Make-TransformerBack $t
}

# Shared transformer top: 4 ceramic insulators
$bmp = New-Bmp
Fill $bmp $tBody
Bevel $bmp $tHi $tLo
foreach ($p in @(@(4,4), @(10,4), @(4,10), @(10,10))) {
    $px = $p[0]; $py = $p[1]
    Rect $bmp $px $py 2 2 (Hex 'D4D4D4')
    Pixel $bmp $px $py (Hex 'F2F2F2')
    Pixel $bmp ($px + 1) ($py + 1) (Hex '8A8A8A')
}
# Central oil cooler
Rect $bmp 7 7 2 2 $tDark
Save-Bmp $bmp 'transformer_top'

# === BREAKER ===
$bBody = Hex '4A4A4A'
$bHi   = Hex '6E6E6E'
$bLo   = Hex '262626'
$bDark = Hex '111111'

function Make-BreakerFront($tier, $stateName) {
    $c = $tierColors[$tier]
    $bmp = New-Bmp
    Fill $bmp $bBody
    Bevel $bmp $bHi $bLo

    # Tier band top
    Rect $bmp 2 2 12 1 $c.accent
    Rect $bmp 2 3 12 1 $c.main

    # Recessed toggle slot
    Rect $bmp 5 5 6 9 $bDark
    Rect $bmp 6 6 4 7 (Hex '1F1F1F')

    if ($stateName -eq 'tripped') {
        # Red fault light + handle up + red handle
        Rect $bmp 6 6 4 2 (Hex '7F1D1D')
        Pixel $bmp 7 7 (Hex 'FF4747')
        Pixel $bmp 8 7 (Hex 'FF4747')
        Rect $bmp 7 8 2 4 (Hex 'B91C1C')
        Pixel $bmp 7 8 (Hex 'FECACA')
        Pixel $bmp 8 8 (Hex 'FECACA')
    } elseif ($stateName -eq 'open') {
        # Manual open: grey light + handle up + neutral grey handle
        Rect $bmp 6 6 4 2 (Hex '3F3F46')
        Pixel $bmp 7 7 (Hex 'A1A1AA')
        Pixel $bmp 8 7 (Hex 'A1A1AA')
        Rect $bmp 7 8 2 4 (Hex '6B7280')
        Pixel $bmp 7 8 (Hex 'D4D4D8')
        Pixel $bmp 8 8 (Hex 'D4D4D8')
    } else {
        # Closed: green light + handle down
        Rect $bmp 6 6 4 2 (Hex '14532D')
        Pixel $bmp 7 7 (Hex '4ADE80')
        Pixel $bmp 8 7 (Hex '4ADE80')
        Rect $bmp 7 9 2 4 (Hex '52525B')
        Pixel $bmp 7 12 (Hex 'BBF7D0')
        Pixel $bmp 8 12 (Hex 'BBF7D0')
    }

    Save-Bmp $bmp "${tier}_voltage_breaker_${stateName}"
}

foreach ($t in @('low','medium','high','extra_high')) {
    Make-BreakerFront $t 'closed'
    Make-BreakerFront $t 'tripped'
    Make-BreakerFront $t 'open'
}

# Breaker back = output side (cable jack with tier-color ring)
function Make-BreakerBack($tier) {
    $c = $tierColors[$tier]
    $bmp = New-Bmp
    Fill $bmp $bBody
    Bevel $bmp $bHi $bLo

    # Tier band top
    Rect $bmp 2 2 12 1 $c.accent
    Rect $bmp 2 3 12 1 $c.main

    # Cable jack (round-ish recessed)
    Rect $bmp 5 6 6 6 $bDark
    Rect $bmp 6 7 4 4 $c.lo               # tier color ring
    Rect $bmp 7 8 2 2 (Hex '0A0A0A')      # inner hole

    # Tiny "OUT" mark dots below
    Pixel $bmp 6 13 (Hex 'A0A0A0')
    Pixel $bmp 8 13 (Hex 'A0A0A0')
    Pixel $bmp 10 13 (Hex 'A0A0A0')
    Save-Bmp $bmp "${tier}_voltage_breaker_back"
}

foreach ($t in @('low','medium','high','extra_high')) {
    Make-BreakerBack $t
}

# Shared breaker top
$bmp = New-Bmp
Fill $bmp $bBody
Bevel $bmp $bHi $bLo
# 4 terminal screws
foreach ($p in @(@(3,4), @(10,4), @(3,10), @(10,10))) {
    $px = $p[0]; $py = $p[1]
    Rect $bmp $px $py 3 3 (Hex 'C7C7C7')
    Pixel $bmp ($px + 1) ($py + 1) $bLo
}
Save-Bmp $bmp 'breaker_top'

# Shared breaker side (mounting groove)
$bmp = New-Bmp
Fill $bmp $bBody
Bevel $bmp $bHi $bLo
Rect $bmp 7 2 2 12 $bDark
Pixel $bmp 7 3 (Hex 'C7C7C7')
Pixel $bmp 8 12 (Hex 'C7C7C7')
Save-Bmp $bmp 'breaker_side'

# === TERMINAL ===
$tmBody = Hex '52525B'
$tmHi   = Hex '7A7A85'
$tmLo   = Hex '2D2D33'
$tmDark = Hex '111111'

function Make-TerminalFront($tier, $state) {
    $c = $tierColors[$tier]
    $bmp = New-Bmp
    Fill $bmp $tmBody
    Bevel $bmp $tmHi $tmLo

    # Tier band top
    Rect $bmp 2 2 12 1 $c.accent
    Rect $bmp 2 3 12 1 $c.main

    # Three screw posts: hot (red, x=3), neutral (blue, x=7), ground (yellow, x=11)
    $hot     = Hex 'EF4444'
    $neutral = Hex '3F76E4'
    $ground  = Hex 'FACC15'

    if ($state -eq 'fault') {
        $ground = Hex '4B5563'   # ground "missing" — greyed
    } elseif ($state -eq 'short') {
        $hot     = Hex 'B91C1C'
        $neutral = Hex '7F1D1D'
    }

    foreach ($post in @(@(3,$hot), @(7,$neutral), @(11,$ground))) {
        $x = $post[0]; $col = $post[1]
        Rect $bmp $x 7 2 4 $tmDark
        Pixel $bmp $x 7 $col
        Pixel $bmp ($x + 1) 7 $col
        Pixel $bmp ($x + 1) 9 $tmHi
    }

    # State indicators
    if ($state -eq 'short') {
        # diagonal red danger stripe
        for ($i = 5; $i -lt 14; $i++) {
            Pixel $bmp $i (18 - $i) (Hex 'EF4444')
        }
    } elseif ($state -eq 'fault') {
        # small yellow warning triangle in bottom-right
        Pixel $bmp 13 12 (Hex 'FACC15')
        Pixel $bmp 12 13 (Hex 'FACC15')
        Pixel $bmp 13 13 (Hex '111827')
        Pixel $bmp 14 13 (Hex 'FACC15')
    }

    Save-Bmp $bmp "${tier}_voltage_terminal_$state"
}

foreach ($t in @('low','medium','high','extra_high')) {
    Make-TerminalFront $t 'correct'
    Make-TerminalFront $t 'fault'
    Make-TerminalFront $t 'short'
}

# Terminal back = cable side (a single cable jack with tier color)
function Make-TerminalBack($tier) {
    $c = $tierColors[$tier]
    $bmp = New-Bmp
    Fill $bmp $tmBody
    Bevel $bmp $tmHi $tmLo

    # Tier band top
    Rect $bmp 2 2 12 1 $c.accent
    Rect $bmp 2 3 12 1 $c.main

    # Cable jack centered
    Rect $bmp 5 6 6 6 $tmDark
    Rect $bmp 6 7 4 4 $c.lo
    Rect $bmp 7 8 2 2 (Hex '0A0A0A')
    Save-Bmp $bmp "${tier}_voltage_terminal_back"
}

foreach ($t in @('low','medium','high','extra_high')) {
    Make-TerminalBack $t
}

# Shared terminal top: 3 cable entry holes
$bmp = New-Bmp
Fill $bmp $tmBody
Bevel $bmp $tmHi $tmLo
Rect $bmp 3 6 2 4 $tmDark
Rect $bmp 7 6 2 4 $tmDark
Rect $bmp 11 6 2 4 $tmDark
Save-Bmp $bmp 'terminal_top'

# Shared terminal side: mounting bracket
$bmp = New-Bmp
Fill $bmp $tmBody
Bevel $bmp $tmHi $tmLo
Rect $bmp 2 7 12 2 $tmDark
Rect $bmp 3 8 10 1 (Hex '1A1A1A')
Save-Bmp $bmp 'terminal_side'

Write-Host ""
Write-Host "All textures generated to: $out"
