package com.finnvek.homecheck.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.finnvek.homecheck.data.local.entity.AssetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AssetDao {
    @Query("SELECT * FROM assets ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<AssetEntity>>

    @Query("SELECT * FROM assets WHERE id = :id")
    fun observeById(id: String): Flow<AssetEntity?>

    @Query("SELECT * FROM assets WHERE id = :id")
    suspend fun getById(id: String): AssetEntity?

    @Query("SELECT * FROM assets")
    suspend fun getAll(): List<AssetEntity>

    @Query("SELECT COUNT(*) FROM assets")
    suspend fun count(): Int

    @Upsert suspend fun upsert(asset: AssetEntity)

    @Upsert suspend fun upsertAll(assets: List<AssetEntity>)

    @Query("DELETE FROM assets WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM assets")
    suspend fun deleteAll()
}
