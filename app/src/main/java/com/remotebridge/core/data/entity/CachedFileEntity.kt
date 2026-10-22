package com.remotebridge.core.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * ملف تم تنزيله (أو وصفته فقط) من الجهاز البعيد.
 * [localPath] يكون null إذا كان مجرد وصف (metadata) دون تنزيل كامل.
 */
@Entity(tableName = "cached_files")
data class CachedFileEntity(
    @PrimaryKey(autoGenerate = true)
    val rowId: Long = 0,
    val remoteFileId: Long,
    val name: String,
    val size: Long,
    val mimeType: String,
    val dateAdded: Long,
    val bucket: String,
    val localPath: String?,
    val downloadedAt: Long?
)
