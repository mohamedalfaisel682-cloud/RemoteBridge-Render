package com.remotebridge.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.remotebridge.core.data.entity.CachedFileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CachedFileDao {

    @Query("SELECT * FROM cached_files ORDER BY dateAdded DESC")
    fun getAll(): Flow<List<CachedFileEntity>>

    @Query("SELECT * FROM cached_files WHERE bucket = :bucket ORDER BY dateAdded DESC")
    fun getByBucket(bucket: String): Flow<List<CachedFileEntity>>

    @Query("SELECT * FROM cached_files WHERE localPath IS NOT NULL ORDER BY downloadedAt DESC")
    fun getDownloaded(): Flow<List<CachedFileEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<CachedFileEntity>)

    @Query("DELETE FROM cached_files WHERE rowId = :rowId")
    suspend fun delete(rowId: Long)

    @Query("DELETE FROM cached_files")
    suspend fun clear()
}
