# Génère les rapports de tests unitaires et d'intégration pour le rapport académique G5.
# Usage: .\scripts\generate-test-reports.ps1

$ErrorActionPreference = "Stop"
$ProjectRoot = Split-Path -Parent $PSScriptRoot
Set-Location $ProjectRoot

$ReportsDir = Join-Path $ProjectRoot "docs\reports"
New-Item -ItemType Directory -Force -Path $ReportsDir | Out-Null

$DateStamp = Get-Date -Format "yyyyMMdd-HHmm"
$SummaryFile = Join-Path $ReportsDir "TEST_SUMMARY_$DateStamp.txt"

Write-Host "=== G5 Notification — Generation des rapports de tests ===" -ForegroundColor Cyan
Write-Host "Repertoire: $ProjectRoot`n"

Write-Host "[1/2] Execution mvn clean test + rapport HTML..." -ForegroundColor Yellow
mvn clean test surefire-report:report -q
if ($LASTEXITCODE -ne 0) { throw "Echec mvn test" }

Write-Host "[2/2] Resume texte..." -ForegroundColor Yellow
$SurefireDir = Join-Path $ProjectRoot "target\surefire-reports"
$HtmlReport = Join-Path $ProjectRoot "target\site\surefire-report.html"

$unitTests = "NotificationServiceImplTest,ChannelRouterImplTest,TemplateServiceImplTest,RetryServiceImplTest,KafkaConsumerControllerTest"
$integrationTests = "G5ApplicationTests,NotificationControllerIntegrationTest,MicroserviceContractIntegrationTest,KafkaMicroserviceIntegrationTest"

$lines = @(
    "RAPPORT DE TESTS — Service G5 Notifications",
    "Date: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')",
    "Projet: service-notification",
    "",
    "=== COMMANDES ===",
    "Tests unitaires:  mvn test -Dtest=$unitTests",
    "Tests integration: mvn test -Dtest=$integrationTests",
    "Suite complete:   mvn clean test surefire-report:report",
    "",
    "=== FICHIERS DE PREUVE ===",
    "Rapport HTML: $HtmlReport",
    "Rapports XML: $SurefireDir",
    "",
    "=== DETAIL PAR CLASSE (Surefire XML) ==="
)

Get-ChildItem $SurefireDir -Filter "TEST-*.xml" | ForEach-Object {
    [xml]$xml = Get-Content $_.FullName
    $suite = $xml.testsuite
    $lines += ("{0,-55} tests={1,3} failures={2} errors={3} skipped={4} time={5}s" -f `
        $suite.name, $suite.tests, $suite.failures, $suite.errors, $suite.skipped, $suite.time)
}

$lines += ""
$lines += "=== POSTMAN (preuve manuelle / Newman) ==="
$lines += "Collection: docs/postman/G5-Notification-Full.postman_collection.json"
$lines += "Environment: docs/postman/G5-Notification-Local.postman_environment.json"
$lines += "Guide: docs/VALIDATION_TESTS.md"
$lines += ""
$lines += "Newman (optionnel):"
$lines += "  newman run docs/postman/G5-Notification-Full.postman_collection.json -e docs/postman/G5-Notification-Local.postman_environment.json -r htmlextra --reporter-htmlextra-export docs/reports/postman-report.html"

$lines | Set-Content -Path $SummaryFile -Encoding UTF8

Write-Host "`n=== TERMINE ===" -ForegroundColor Green
Write-Host "Resume:     $SummaryFile"
Write-Host "HTML Maven: $HtmlReport"
Write-Host "XML JUnit:  $SurefireDir"
Write-Host "`nOuvrez le rapport HTML dans le navigateur pour la capture d'ecran du rapport prof." -ForegroundColor Cyan
