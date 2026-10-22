package com.remotebridge.core.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * رسالة نصية محفوظة من الجهاز البعيد.
 */
@Entity(tableName = "cached_sms")
data class CachedSmsEntity(
    @PrimaryKey(autoGenerate = true)
    val rowId: Long = 0,
    val remoteSmsId: Long,
    val threadId: Long,
    val address: String,
    val body: String,
    val date: Long,
    val type: Int,
    val contactName: String?
)
