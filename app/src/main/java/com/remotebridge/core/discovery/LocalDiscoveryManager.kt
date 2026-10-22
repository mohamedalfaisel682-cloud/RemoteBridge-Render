package com.remotebridge.core.discovery

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket

/**
 * مدير الاكتشاف التلقائي لخوادم الإشارات المتاحة على الشبكة المحلية (LAN Discovery).
 * يفحص المنافذ والعناوين في النطاق المحلي لإيجاد خادم RemoteBridge دون الحاجة لكتابة الـ IP يدوياً.
 */
object LocalDiscoveryManager {

    private const val TAG = "LocalDiscovery"
    private const val DEFAULT_PORT = 10000
    private const val TIMEOUT_MS = 350

    /**
     * يبحث عن عنوان خادم الإشارات في شبكة الـ Wi-Fi الحالية.
     * يعيد رابط WebSocket الجاهز للاتصال (مثل: ws://192.168.1.50:10000/ws) أو null إذا لم يجد.
     */
    suspend fun discoverLocalServer(context: Context): String? = withContext(Dispatchers.IO) {
        val subnetPrefix = getSubnetPrefix(context) ?: "192.168.1"
        Log.i(TAG, "بدء البحث التلقائي بداخل النطاق المحلي: $subnetPrefix.x على المرفأ $DEFAULT_PORT")

        // اختبر العنوان المحلي للمحاكي أولاً
        if (isServerActive("10.0.2.2", DEFAULT_PORT)) {
            Log.i(TAG, "تم العثور على الخادم بداخل البيئة المحلية للمحاكي (10.0.2.2)")
            return@withContext "ws://10.0.2.2:$DEFAULT_PORT/ws"
        }

        // افحص أول 50 عنوان IP في الشبكة بتوازٍ سريع
        val deferreds = (1..60).map { host ->
            async {
                val ip = "$subnetPrefix.$host"
                if (isServerActive(ip, DEFAULT_PORT)) ip else null
            }
        }

        val results = deferreds.awaitAll().filterNotNull()
        val foundIp = results.firstOrNull()

        if (foundIp != null) {
            Log.i(TAG, "تم اكتشاف الخادم المحلي بنجاح على العنوان: $foundIp")
            "ws://$foundIp:$DEFAULT_PORT/ws"
        } else {
            Log.w(TAG, "لم يتم العثور على خادم محلي نشط بالشبكة")
            null
        }
    }

    private fun isServerActive(ip: String, port: Int): Boolean {
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(ip, port), TIMEOUT_MS)
                true
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun getSubnetPrefix(context: Context): String? {
        return try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            val ipInt = wifiManager?.connectionInfo?.ipAddress ?: return null
            if (ipInt == 0) return null
            val ipBytes = byteArrayOf(
                (ipInt and 0xff).toByte(),
                (ipInt shr 8 and 0xff).toByte(),
                (ipInt shr 16 and 0xff).toByte(),
                (ipInt shr 24 and 0xff).toByte()
            )
            val inetAddress = InetAddress.getByAddress(ipBytes)
            val hostAddress = inetAddress.hostAddress ?: return null
            val parts = hostAddress.split(".")
            if (parts.size == 4) "${parts[0]}.${parts[1]}.${parts[2]}" else null
        } catch (e: Exception) {
            null
        }
    }
}
