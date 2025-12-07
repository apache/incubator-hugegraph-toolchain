# Build script for HugeGraph Hubble
$MavenBin = "C:\Tools\maven\bin\mvn.cmd"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Building HugeGraph Toolchain Dependencies" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

# Step 1: Build all dependencies
Write-Host "`n[Step 1/5] Installing all dependencies..." -ForegroundColor Yellow
& $MavenBin -q -DskipTests install
if ($LASTEXITCODE -ne 0) {
    Write-Host "Build failed! Exit code: $LASTEXITCODE" -ForegroundColor Red
    exit 1
}
