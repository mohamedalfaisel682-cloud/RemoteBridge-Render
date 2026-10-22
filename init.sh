#!/bin/bash
# =============================================================================
# RemoteBridge Pro Studio - Project Initialization Script (Linux/macOS)
# =============================================================================
# هذا السكريبت يقوم بتهيئة المشروع وتشغيل جميع المكونات المطلوبة
# التشغيل: chmod +x init.sh && ./init.sh
# =============================================================================

set -e

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$PROJECT_DIR"

echo "============================================================================="
echo "  RemoteBridge Pro Studio - Project Initialization"
echo "  $(date)"
echo "============================================================================="

# -----------------------------------------------------
# 1. التحقق من الأدوات المطلوبة
# -----------------------------------------------------
echo ""
echo "[1/6] التحقق من الأدوات المطلوبة..."

check_tool() {
    if ! command -v "$1" &> /dev/null; then
        echo "  ❌ $1 غير مثبت. يرجى تثبيته أولاً."
        exit 1
    else
        echo "  ✅ $1 موجود"
    fi
}

check_tool "java"
check_tool "kotlin"
check_tool "go"
check_tool "docker"
check_tool "docker-compose" || check_tool "docker" # docker compose v2

# -----------------------------------------------------
# 2. إعداد متغيرات البيئة
# -----------------------------------------------------
echo ""
echo "[2/6] إعداد متغيرات البيئة..."

if [ ! -f .env ]; then
    if [ -f .env.example ]; then
        cp .env.example .env
        echo "  ✅ تم إنشاء ملف .env من .env.example"
        echo "  ⚠️  يرجى تعديل ملف .env وإضافة القيم الفعلية قبل التشغيل"
    else
        echo "  ⚠️  ملف .env.example غير موجود، يتم استخدام القيم الافتراضية"
    fi
else
    echo "  ✅ ملف .env موجود"
fi

# تحميل المتغيرات من .env إذا وجد
if [ -f .env ]; then
    export $(grep -v '^#' .env | grep -v '^$' | xargs)
fi

# -----------------------------------------------------
# 3. تشغيل خادم TURN (coturn)
# -----------------------------------------------------
echo ""
echo "[3/6] تشغيل خادم TURN (coturn)..."

if docker ps | grep -q "remotebridge-turn"; then
    echo "  ✅ خادم TURN يعمل بالفعل"
else
    if [ -f docker-compose.turn.yml ]; then
        docker-compose -f docker-compose.turn.yml up -d coturn
        echo "  ✅ تم تشغيل خادم TURN"
    else
        echo "  ⚠️  ملف docker-compose.turn.yml غير موجود، تخطي تشغيل TURN"
    fi
fi

# -----------------------------------------------------
# 4. تشغيل خادم الإشارات (Signaling Server)
# -----------------------------------------------------
echo ""
echo "[4/6] تشغيل خادم الإشارات (Signaling Server)..."

if docker ps | grep -q "remotebridge-signaling"; then
    echo "  ✅ خادم الإشارات يعمل بالفعل"
else
    if [ -f go.mod ]; then
        # محاولة البناء والتشغيل عبر Go مباشرة
        echo "  جاري بناء خادم الإشارات..."
        cd "$PROJECT_DIR"
        go mod download
        go build -o signaling-server main.go
        nohup ./signaling-server > signaling-server.log 2>&1 &
        echo "  ✅ تم تشغيل خادم الإشارات (PID: $!)"
    else
        echo "  ⚠�  ملف go.mod غير موجود، تخطي تشغيل الخادم"
    fi
fi

# -----------------------------------------------------
# 5. بناء تطبيق الأندرويد (إذا كان gradle متاحاً)
# -----------------------------------------------------
echo ""
echo "[5/6] بناء تطبيق الأندرويد..."

if [ -f gradlew ]; then
    chmod +x gradlew
    echo "  جاري مزامنة Gradle وبناء التطبيق..."
    ./gradlew assembleDebug
    echo "  ✅ تم بناء تطبيق الأندرويد بنجاح"
    echo "  📁 مسار ملف APK: app/build/outputs/apk/debug/app-debug.apk"
else
    echo "  ⚠�  ملف gradlew غير موجود، يرجى بناء المشروع عبر Android Studio"
fi

# -----------------------------------------------------
# 6. عرض ملخص الحالة
# -----------------------------------------------------
echo ""
echo "[6/6] ملخص الحالة..."
echo ""
echo "============================================================================="
echo "  اكتمل تهيئة المشروع بنجاح!"
echo "============================================================================="
echo ""
echo "الروابط المهمة:"
echo "  - خادم الإشارات: http://localhost:10000/health"
echo "  - مقاييس Prometheus: http://localhost:9090/metrics"
echo "  - خادم TURN: turn:turn.remotebridge.app:3478"
echo ""
echo "ملفات الإعداد:"
echo "  - متغيرات البيئة: .env"
echo "  - إعدادات OpenCode/MCP: .opencode/opencode.jsonc"
echo "  - الخلاصة الشاملة: PROJECT_CONFIG_SUMMARY.md"
echo ""
echo "للبدء:"
echo "  1. ثبت ملف APK على الهاتف: app/build/outputs/apk/debug/app-debug.apk"
echo "  2. امنح جميع الأذونات المطلوبة"
echo "  3. انقر على 'بدء المضيف' في التطبيق"
echo "  4. افتح web-dashboard/index.html في المتصفح"
echo "  5. اكتب معرف الجهاز المضيف وانقر 'ربط WebRTC'"
echo ""
echo "لمساعدة في استكشاف الأخطاء:"
echo "  cat signaling-server.log"
echo "  docker logs remotebridge-turn"
echo ""
echo "============================================================================="