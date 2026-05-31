# Assemble G4_SGITU_Final.zip — Livraison VCA Pr. BESRI
# Usage: .\scripts\assemble-livraison-G4.ps1
# Prérequis PDF: compiler RAPPORT_G4_COORDINATION.tex et PRESENTATION_G4.tex (pdflatex x2)

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
$OutDir = Join-Path $Root "livraison-temp"
$ZipName = Join-Path $Root "G4_SGITU_Final.zip"

if (Test-Path $OutDir) { Remove-Item $OutDir -Recurse -Force }
New-Item -ItemType Directory -Path $OutDir | Out-Null

$DestCode = Join-Path $OutDir "service-coordination-transport"
Write-Host "Copie du code source..."
robocopy $Root $DestCode /E /XD target .git .mvn livraison-temp node_modules .idea /XF *.class *.log /NFL /NDL /NJH /NJS | Out-Null
if ($LASTEXITCODE -ge 8) { throw "robocopy failed: $LASTEXITCODE" }

# PDFs rapport et slides (si compilés)
$RapportPdf = Join-Path $Root "RAPPORT_G4_COORDINATION.pdf"
$SlidesPdf = Join-Path $Root "PRESENTATION_G4.pdf"
if (Test-Path $RapportPdf) {
    Copy-Item $RapportPdf (Join-Path $OutDir "RAPPORT_TECHNIQUE_G4.pdf")
    Write-Host "OK: RAPPORT_TECHNIQUE_G4.pdf"
} else {
    Write-Warning "Manquant: RAPPORT_G4_COORDINATION.pdf — compiler avec pdflatex RAPPORT_G4_COORDINATION.tex"
    Copy-Item (Join-Path $Root "RAPPORT_G4_COORDINATION.tex") (Join-Path $OutDir "RAPPORT_G4_COORDINATION.tex")
}
if (Test-Path $SlidesPdf) {
    Copy-Item $SlidesPdf (Join-Path $OutDir "PRESENTATION_G4.pdf")
    Write-Host "OK: PRESENTATION_G4.pdf"
} else {
    Write-Warning "Manquant: PRESENTATION_G4.pdf — compiler avec pdflatex PRESENTATION_G4.tex"
    Copy-Item (Join-Path $Root "PRESENTATION_G4.tex") (Join-Path $OutDir "PRESENTATION_G4.tex")
}

# Index livraison
@"
G4 — SGITU Livraison Finale
============================
Groupe G4 — Coordination des transports
Pr. BESRI — Année 2025-2026

Contenu:
- RAPPORT_TECHNIQUE_G4.pdf (ou .tex)
- PRESENTATION_G4.pdf (ou .tex)
- service-coordination-transport/ (code, Docker, Postman, docs)

Démarrage rapide:
  cd service-coordination-transport
  docker compose --profile monitoring up -d --build

Tests:
  .\mvnw.cmd test

- Documentation :
  docs/LIVRAISON_VCA_BESRI_G4.md
  docs/3_PILIERS_LIVRAISON_G4.md
"@ | Set-Content (Join-Path $OutDir "LISEZMOI.txt") -Encoding UTF8

if (Test-Path $ZipName) { Remove-Item $ZipName -Force }
Compress-Archive -Path (Join-Path $OutDir "*") -DestinationPath $ZipName -Force
Remove-Item $OutDir -Recurse -Force

Write-Host ""
Write-Host "Archive créée: $ZipName" -ForegroundColor Green
Write-Host "Taille: $((Get-Item $ZipName).Length / 1MB) Mo"
