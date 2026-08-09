package com.finnvek.homecheck.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.finnvek.homecheck.data.local.entity.MaintenanceTaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MaintenanceDao {
    @Query("SELECT * FROM maintenance_tasks ORDER BY dueDate, title")
    fun observeAll(): Flow<List<MaintenanceTaskEntity>>

    @Query("SELECT * FROM maintenance_tasks WHERE assetId = :assetId ORDER BY dueDate, title")
    fun observeForAsset(assetId: String): Flow<List<MaintenanceTaskEntity>>

    @Query("SELECT * FROM maintenance_tasks WHERE id = :id")
    suspend fun getById(id: String): MaintenanceTaskEntity?

    @Query("SELECT * FROM maintenance_tasks")
    suspend fun getAll(): List<MaintenanceTaskEntity>

    @Upsert suspend fun upsert(task: MaintenanceTaskEntity)

    @Upsert suspend fun upsertAll(tasks: List<MaintenanceTaskEntity>)

    @Query("DELETE FROM maintenance_tasks WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM maintenance_tasks")
    suspend fun deleteAll()
}
