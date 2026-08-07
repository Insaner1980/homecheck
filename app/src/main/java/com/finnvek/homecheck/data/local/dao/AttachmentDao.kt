package com.finnvek.homecheck.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.finnvek.homecheck.data.local.entity.AttachmentEntity
import com.finnvek.homecheck.data.local.entity.AttachmentType
import kotlinx.coroutines.flow.Flow

@Dao
interface AttachmentDao {
    @Query("SELECT * FROM attachments ORDER BY createdAt DESC") fun observeAll(): Flow<List<AttachmentEntity>>
    @Query("SELECT * FROM attachments ORDER BY createdAt DESC") suspend fun getAll(): List<AttachmentEntity>
    @Query("SELECT * FROM attachments WHERE assetId = :assetId ORDER BY createdAt DESC") fun observeForAsset(assetId: String): Flow<List<AttachmentEntity>>
    @Query("SELECT * FROM attachments WHERE assetId = :assetId") suspend fun getForAsset(assetId: String): List<AttachmentEntity>
    @Query("SELECT * FROM attachments WHERE id = :id") suspend fun getById(id: String): AttachmentEntity?
    @Upsert suspend fun upsert(attachment: AttachmentEntity)
    @Upsert suspend fun upsertAll(attachments: List<AttachmentEntity>)
    @Query("UPDATE attachments SET displayName = :name WHERE id = :id") suspend fun rename(id: String, name: String)
    @Query("UPDATE attachments SET type = :type WHERE id = :id") suspend fun changeType(id: String, type: AttachmentType)
    @Query("DELETE FROM attachments WHERE id = :id") suspend fun delete(id: String)
    @Query("DELETE FROM attachments") suspend fun deleteAll()
}
