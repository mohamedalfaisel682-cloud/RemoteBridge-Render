# =============================================================================
# RemoteBridge Pro Studio - Project Initialization Script (Windows PowerShell)
# =============================================================================
# هذا السكريبت يقوم بتهيئة المشروع وتشغيل جميع المكونات المطلوبة
# التشغيل: كمدير النظام، نفذ: .\init.ps1
# =============================================================================

$ErrorActionPreference = "Stop"
$PROJECT_DIR = Split-Path -Parent $MyInvocation.MyCommand.Definition
Set-Location $PROJECT_DIR

Write-Host "=============================================================================" -ForegroundColor Cyan
Write-Host "  RemoteBridge Pro Studio - Project Initialization" -ForegroundColor Cyan
Write-Host "  $(Get-Date)" -ForegroundColor Cyan
Write-Host "=============================================================================" -ForegroundColor Cyan

# -----------------------------------------------------
# 1. التحقق من الأدوات المطلوبة
# -----------------------------------------------------
Write-Host ""
Write-Host "[1/6] التحقق من الأدوات المطلوبة..." -ForegroundColor Yellow

function Test-Tool {
    param($Name)
    if (Get-Command $Name -ErrorAction SilentlyContinue) {
        Write-Host "  ✅ $Name موجود" -ForegroundColor Green
    } else {
        Write-Host "  ❌ $Name غير مثبت. يرجى تثبيته أولاً." -ForegroundColor Red
        exit 1
    }
}

Test-Tool "java"
Test-Tool "go"
Test-Tool "docker"

# -----------------------------------------------------
# 2. إعداد متغيرات البيئة
# -----------------------------------------------------
Write-Host ""
Write-Host "[2/6] إعداد متغيرات البيئة..." -ForegroundColor Yellow

if (-not (Test-Path ".env")) {
    if (Test-Path ".env.example") {
        Copy-Item ".env.example" ".env"
        Write-Host "  ✅ تم إنشاء ملف .env من .env.example" -ForegroundColor Green
        Write-Host "  ⚠️  يرجى تعديل ملف .env وإضافة القيم الفعلية قبل التشغيل" -ForegroundColor Yellow
    } else {
        Write-Host "  ⚠️  ملف .env.example غير موجود، يتم استخدام القيم الافتراضية" -ForegroundColor Yellow
    }
} else {
    Write-Host "  ✅ ملف .env موجود" -ForegroundColor Green
}

# -----------------------------------------------------
# 3. تشغيل خادم TURN (coturn)
# -----------------------------------------------------
Write-Host ""
Write-Host "[3/6] تشغيل خادم TURN (coturn)..." -ForegroundColor Yellow

$turnContainer = docker ps --filter "name=remotebridge-turn" --format "{{.Names}}"
if ($turnContainer -eq "remotebridge-turn") {
    Write-Host "  ✅ خادم TURN يعمل بالفعل" -ForegroundColor Green
} else {
    if (Test-Path "docker-compose.turn.yml") {
        docker-compose -f docker-compose.turn.yml up -d coturn
        Write-Host "  ✅ تم تشغيل خادم TURN" -ForegroundColor Green
    } else {
        Write-Host "  ⚠️  ملف docker-compose.turn.yml غير موجود، تخطي تشغيل TURN" -ForegroundColor Yellow
    }
}

# -----------------------------------------------------
# 4. تشغيل خادم الإشارات (Signaling Server)
# -----------------------------------------------------
Write-Host ""
Write-Host "[4/6] تشغيل خادم الإشارات (Signaling Server)..." -ForegroundColor Yellow

$signalContainer = docker ps --filter "name=remotebridge-signaling" --format "{{.Names}}"
if ($signalContainer -eq "remotebridge-signaling") {
    Write-Host "  ✅ خادم الإشارات يعمل بالفعل" -ForegroundColor Green
} else {
    if (Test-Path "go.mod") {
        Write-Host "  جاري بناء خادم الإشارات..." -ForegroundColor Cyan
        go mod download
        go build -o signaling-server.exe main.go
        Start-Process -FilePath ".\signaling-server.exe" -ArgumentList @() -RedirectStandardOutput "signaling-server.log" -RedirectStandardError "signaling-server.log" -WindowStyle Hidden
        Write-Host "  ✅ تم تشغيل خادم الإشارات" -ForegroundColor Green
    } else {
        Write-Host "  ⚠️  ملف go.mod غير موجود، تخطي تشغيل الخادم" -ForegroundColor Yellow
    }
}

# -----------------------------------------------------
# 5. بناء تطبيق الأندرويد (إذا كان gradle متاحاً)
# -----------------------------------------------------
Write-Host ""
Write-Host "[5/6] بناء تطبيق الأندرويد..." -ForegroundColor Yellow

if (Test-Path "gradlew.bat") {
    Write-Host "  جاري مزامنة Gradle وبناء التطبيق..." -ForegroundColor Cyan
    .\gradlew.bat assembleDebug
    Write-Host "  ✅ تم بناء تطبيق الأندرويد بنجاح" -ForegroundColor Green
    Write-Host "  📁 مسار ملف APK: app\build\outputs\apk\debug\app-debug.apk" -ForegroundColor Green
} else {
    Write-Host "  ⚠️  ملف gradlew.bat غير موجود، يرجى بناء المشروع عبر Android Studio" -ForegroundColor Yellow
}

# -----------------------------------------------------
# 6. عرض ملخص الحالة
# -----------------------------------------------------
Write-Host ""
Write-Host "[6/6] ملخص الحالة..." -ForegroundColor Yellow
Write-Host ""
Write-Host "=============================================================================" -ForegroundColor Cyan
Write-Host "  اكتمل تهيئة المشروع بنجاح!" -ForegroundColor Green
Write-Host "=============================================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "الروابط المهمة:" -ForegroundColor Cyan
Write-Host "  - خادم الإشارات: http://localhost:10000/health" -ForegroundColor White
Write-Host "  - مقاييس Prometheus: http://localhost:9090/metrics" -ForegroundColor White
Write-Host "  - خادم TURN: turn:turn.remotebridge.app:3478" -ForegroundColor White
Write-Host ""
Write-Host "ملفات الإعداد:" -ForegroundColor Cyan
Write-Host "  - متغيرات البيئة: .env" -ForegroundColor White
Write-Host "  - إعدادات OpenCode/MCP: .opencode\opencode.jsonc" -ForegroundColor White
Write-Host "  - الخلاصة الشاملة: PROJECT_CONFIG_SUMMARY.md" -ForegroundColor White
Write-Host ""
Write-Host "للبدء:" -ForegroundColor Cyan
Write-Host "  1. ثبت ملف APK على الهاتف: app\build\outputs\apk\debug\app-debug.apk" -ForegroundColor White
Write-Host "  2. امنح جميع الأذونات المطلوبة" -ForegroundColor White
Write-Host "  3. انقر على 'بدء المضيف' في التطبيق" -ForegroundColor White
Write-Host "  4. افتح web-dashboard\index.html في المتصفح" -ForegroundColor White
Write-Host "  5. اكتب معرف الجهاز المضيف وانقر 'ربط WebRTC'" -ForegroundColor White
Write-Host ""
Write-Host "لمساعدة في استكشاف الأخطاء:" -ForegroundColor Cyan
Write-Host "  Get-Content signaling-server.log" -ForegroundColor White
Write-Host "  docker logs remotebridge-turn" -ForegroundColor White
Write-Host ""
Write-Host "=============================================================================" -ForegroundColor Cyan