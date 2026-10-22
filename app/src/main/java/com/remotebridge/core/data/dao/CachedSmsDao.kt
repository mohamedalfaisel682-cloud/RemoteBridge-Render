package com.remotebridge.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.remotebridge.core.data.entity.CachedSmsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CachedSmsDao {

    @Query("SELECT * FROM cached_sms ORDER BY date DESC")
    fun getAll(): Flow<List<CachedSmsEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<CachedSmsEntity>)

    @Query("DELETE FROM cached_sms")
    suspend fun clear()
}
