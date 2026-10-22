package com.remotebridge.core.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.map

/**
 * تخزين إعدادات التطبيق الدائمة (عنوان الخادم، اسم الجهاز، الدور).
 * يستخدم DataStore Preferences بدلاً من SharedPreferences لكونه آمناً مع Coroutines.
 */
val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "bridge_settings"
)

class SettingsRepository(private val context: Context) {

    private object Keys {
        val SERVER_URL = stringPreferencesKey("server_url")
        val STUN_SERVER = stringPreferencesKey("stun_server")
        val CONNECTION_PROFILE = stringPreferencesKey("connection_profile")
        val DEVICE_NAME = stringPreferencesKey("device_name")
        val ROLE = stringPreferencesKey("role")
        val AUTO_START = booleanPreferencesKey("auto_start_host")
    }

    companion object {
        const val DEFAULT_FLY_URL = "wss://my-ai-bridge-app.fly.dev/ws"
        const val DEFAULT_RENDER_URL = "wss://remotebridge1.onrender.com/ws"
        const val DEFAULT_LOCAL_URL = "ws://10.0.2.2:10000/ws"
        const val DEFAULT_SERVER_URL = DEFAULT_RENDER_URL
        const val DEFAULT_STUN_SERVER = "stun:stun.l.google.com:19302"
        const val DEFAULT_PROFILE = "RENDER_ONLY"
        const val DEFAULT_DEVICE_NAME = "Galaxy Note 20 Ultra"
    }

    val serverUrl: Flow<String> = context.settingsDataStore.data.map { 
        it[Keys.SERVER_URL]?.takeIf { url -> url.isNotBlank() } ?: DEFAULT_SERVER_URL 
    }
    val stunServer: Flow<String> = context.settingsDataStore.data.map {
        it[Keys.STUN_SERVER]?.takeIf { stun -> stun.isNotBlank() } ?: DEFAULT_STUN_SERVER
    }
    val connectionProfile: Flow<String> = context.settingsDataStore.data.map {
        it[Keys.CONNECTION_PROFILE]?.takeIf { p -> p.isNotBlank() } ?: DEFAULT_PROFILE
    }
    val deviceName: Flow<String> = context.settingsDataStore.data.map { 
        it[Keys.DEVICE_NAME]?.takeIf { name -> name.isNotBlank() } ?: DEFAULT_DEVICE_NAME 
    }
    val role: Flow<String> = context.settingsDataStore.data.map { it[Keys.ROLE] ?: "" }
    val autoStartHost: Flow<Boolean> = context.settingsDataStore.data.map {
        it[Keys.AUTO_START] ?: false
    }

    suspend fun setServerUrl(url: String) {
        context.settingsDataStore.edit { it[Keys.SERVER_URL] = url }
    }

    suspend fun setStunServer(stunUrl: String) {
        context.settingsDataStore.edit { it[Keys.STUN_SERVER] = stunUrl }
    }

    suspend fun setConnectionProfile(profile: String) {
        context.settingsDataStore.edit { it[Keys.CONNECTION_PROFILE] = profile }
    }

    suspend fun setDeviceName(name: String) {
        context.settingsDataStore.edit { it[Keys.DEVICE_NAME] = name }
    }

    suspend fun setRole(role: String) {
        context.settingsDataStore.edit { it[Keys.ROLE] = role }
    }

    suspend fun setAutoStartHost(autoStart: Boolean) {
        context.settingsDataStore.edit { it[Keys.AUTO_START] = autoStart }
    }
}
