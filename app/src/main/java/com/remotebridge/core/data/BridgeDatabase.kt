package com.remotebridge.core.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.remotebridge.core.data.dao.CachedFileDao
import com.remotebridge.core.data.dao.CachedSmsDao
import com.remotebridge.core.data.entity.CachedFileEntity
import com.remotebridge.core.data.entity.CachedSmsEntity

/**
 * قاعدة بيانات التخزين المؤقت المحلية على جهاز التحكم.
 * تحفظ نسخة من بيانات الجهاز البعيد لاستعراضها دون اتصال.
 */
@Database(
    entities = [CachedFileEntity::class, CachedSmsEntity::class],
    version = 1,
    exportSchema = false
)
abstract class BridgeDatabase : RoomDatabase() {
    abstract fun cachedFileDao(): CachedFileDao
    abstract fun cachedSmsDao(): CachedSmsDao
}
