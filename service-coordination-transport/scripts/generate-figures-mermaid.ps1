# Genere les PNG Mermaid pour le rapport G4
$ErrorActionPreference = "Continue"
$root = Split-Path -Parent $PSScriptRoot
$mmdDir = Join-Path $root "figures\mermaid"
$outDir = Join-Path $root "figures"
New-Item -ItemType Directory -Force -Path $outDir | Out-Null

$files = Get-ChildItem $mmdDir -Filter "*.mmd"
Write-Host "Found $($files.Count) mermaid files"

foreach ($f in $files) {
    $out = Join-Path $outDir ($f.BaseName + ".png")
    Write-Host ">>> $($f.Name)"
    npx --yes @mermaid-js/mermaid-cli -i $f.FullName -o $out -b white -w 1400 2>&1
}
Write-Host "PNG dans: $outDir"
Get-ChildItem $outDir -Filter "*.png" | Select-Object Name, Length
