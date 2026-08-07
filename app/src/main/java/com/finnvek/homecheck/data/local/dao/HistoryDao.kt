package com.finnvek.homecheck.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.finnvek.homecheck.data.local.entity.MaintenanceHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Query("SELECT * FROM maintenance_history ORDER BY completedAt DESC") fun observeAll(): Flow<List<MaintenanceHistoryEntity>>
    @Query("SELECT * FROM maintenance_history WHERE assetId = :assetId ORDER BY completedAt DESC") fun observeForAsset(assetId: String): Flow<List<MaintenanceHistoryEntity>>
    @Query("SELECT * FROM maintenance_history") suspend fun getAll(): List<MaintenanceHistoryEntity>
    @Insert suspend fun insert(entry: MaintenanceHistoryEntity)
    @Insert suspend fun insertAll(entries: List<MaintenanceHistoryEntity>)
    @Query("DELETE FROM maintenance_history WHERE id = :id") suspend fun delete(id: String)
    @Query("DELETE FROM maintenance_history") suspend fun deleteAll()
}

