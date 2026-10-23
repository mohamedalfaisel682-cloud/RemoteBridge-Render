package com.remotebridge.host.service

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.remotebridge.host.provider.HostDataProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * مصادر "الحدث الحي" على هاتف المضيف — يغذي الخدمة بكل ما يصل جديداً:
 *   1) إشعارات التطبيقات  (من خدمة مستمع الإشعارات BridgeNotificationListenerService)
 *   2) الرسائل القصيرة     (مراقب مزوّد الرسائل content://sms)
 *   3) سجل المكالمات      (مراقب مزوّد السجل content://call_log)
 *
 * الخدمة تمرر هذه الأحداث كـ push_event إلى المتحكم فوراً.
 * (ملاحظة: مراقبو المحتوى لا يحتاجون أي إذن إضافي — الأذونات المطلوبة لقراءة القوائم ممنوحة بالفعل)
 */
class LiveEventWatchers(
    private val context: Context,
    private val scope: CoroutineScope,
    private val provider: HostDataProvider,
    private val onEvent: (kind: String, payload: Any) -> Unit
) {
    companion object {
        private const val TAG = "LiveWatchers"
    }

    private val handler = Handler(Looper.getMainLooper())
    private var smsObserver: ContentObserver? = null
    private var callObserver: ContentObserver? = null
    private var notificationJob: Job? = null
    private var lastSmsKey: String? = null
    private var lastCallKey: String? = null
    private var started = false

    fun start() {
        if (started) return
        started = true

        // 1) تدفق إشعارات التطبيقات الحي
        notificationJob = scope.launch {
            try {
                BridgeNotificationListenerService.notifications.collect { item ->
                    onEvent("notification", item)
                }
            } catch (t: Throwable) {
                Log.e(TAG, "توقف تجمّع تدفق الإشعارات", t)
            }
        }

        // 2) مراقب الرسائل القصيرة
        smsObserver = object : ContentObserver(handler) {
            override fun onChange(selfChange: Boolean) {
                pushLatestSms()
            }
        }

        // 3) مراقب سجل المكالمات
        callObserver = object : ContentObserver(handler) {
            override fun onChange(selfChange: Boolean) {
                pushLatestCall()
            }
        }

        try {
            context.contentResolver.registerContentObserver(Uri.parse("content://sms"), true, smsObserver!!)
            context.contentResolver.registerContentObserver(Uri.parse("content://call_log/calls"), true, callObserver!!)
            Log.i(TAG, "تم تفعيل مراقبي الأحداث الحية (SMS / CallLog / Notifications)")
        } catch (t: Throwable) {
            Log.w(TAG, "تعذّر تسجيل مراقبي المحتوى: ${t.message}")
        }

        // مزامنة مفاتيح آخر عنصرين موجودين مسبقاً حتى لا نُدفع بهما عند أول تغيير إلا حقيقةً جديداً
        pushLatestSms(silent = true)
        pushLatestCall(silent = true)
    }

    fun stop() {
        try {
            smsObserver?.let { context.contentResolver.unregisterContentObserver(it) }
            callObserver?.let { context.contentResolver.unregisterContentObserver(it) }
        } catch (_: Throwable) { }
        notificationJob?.cancel()
        notificationJob = null
        smsObserver = null
        callObserver = null
        started = false
        Log.i(TAG, "أُوقف مراقبو الأحداث الحية")
    }

    private fun pushLatestSms(silent: Boolean = false) {
        scope.launch {
            try {
                val sms = provider.readLatestSms() ?: return@launch
                val key = "${sms.id}:${sms.date}"
                if (silent) {
                    lastSmsKey = key
                    return@launch
                }
                if (key == lastSmsKey) return@launch
                lastSmsKey = key
                onEvent("sms", sms)
            } catch (t: Throwable) {
                Log.e(TAG, "فشل دفع أحدث رسالة", t)
            }
        }
    }

    private fun pushLatestCall(silent: Boolean = false) {
        scope.launch {
            try {
                val call = provider.readLatestCall() ?: return@launch
                val key = "${call.id}:${call.date}"
                if (silent) {
                    lastCallKey = key
                    return@launch
                }
                if (key == lastCallKey) return@launch
                lastCallKey = key
                onEvent("call", call)
            } catch (t: Throwable) {
                Log.e(TAG, "فشل دفع أحدث مكالمة", t)
            }
        }
    }
}
