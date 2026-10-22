# RemoteBridge Automated Build Script
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "   RemoteBridge Enterprise Build System   " -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan

# 1. Build Relay Server
Write-Host "[1/2] Building Go Relay Server..." -ForegroundColor Yellow
Set-Location "server"
go build -o relay-server.exe main.go
if ($?) {
    Write-Host "[OK] Relay Server built successfully!" -ForegroundColor Green
} else {
    Write-Host "[ERROR] Relay Server build failed!" -ForegroundColor Red
}
Set-Location ".."

# 2. Build Android Agent App
Write-Host "[2/2] Building Android Agent APK (Debug)..." -ForegroundColor Yellow
./gradlew assembleDebug
if ($?) {
    Write-Host "[OK] Android Agent APK built successfully!" -ForegroundColor Green
    Write-Host "APK Location: app\build\outputs\apk\debug\app-debug.apk" -ForegroundColor Cyan
} else {
    Write-Host "[ERROR] Android Agent APK build failed!" -ForegroundColor Red
}

Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "         Build Process Completed          " -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan
