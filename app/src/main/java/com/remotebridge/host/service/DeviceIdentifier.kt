package com.remotebridge.host.service

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.remotebridge.core.data.settingsDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID

/**
 * يولّد معرّفاً فريداً ثابتاً لكل جهاز ويحفظه في DataStore.
 *
 * لا نستخدم ANDROID_ID لأنه قد يتغير عند إعادة ضبط المصنع،
 * ولا نستخدم معرّفات الجهاز (IMEI) لأنها تتطلب أذونات إضافية.
 */
object DeviceIdentifier {

    private val KEY = stringPreferencesKey("device_id")

    /**
     * يعيد المعرّف الحالي، أو يولّد واحداً جديداً ويحفظه.
     * يجب استدعاؤها من coroutine (تعليق).
     */
    suspend fun getOrGenerate(context: Context): String {
        val current = context.settingsDataStore.data.map { it[KEY] }.first()
        if (!current.isNullOrBlank()) return current
        val newId = UUID.randomUUID().toString()
        context.settingsDataStore.edit { it[KEY] = newId }
        return newId
    }
}
