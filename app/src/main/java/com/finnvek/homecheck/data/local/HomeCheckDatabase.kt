package com.finnvek.homecheck.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.finnvek.homecheck.data.local.dao.AssetDao
import com.finnvek.homecheck.data.local.dao.AttachmentDao
import com.finnvek.homecheck.data.local.dao.HistoryDao
import com.finnvek.homecheck.data.local.dao.MaintenanceDao
import com.finnvek.homecheck.data.local.entity.AssetEntity
import com.finnvek.homecheck.data.local.entity.AttachmentEntity
import com.finnvek.homecheck.data.local.entity.MaintenanceHistoryEntity
import com.finnvek.homecheck.data.local.entity.MaintenanceTaskEntity

@Database(
    entities = [AssetEntity::class, AttachmentEntity::class, MaintenanceTaskEntity::class, MaintenanceHistoryEntity::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class HomeCheckDatabase : RoomDatabase() {
    abstract fun assetDao(): AssetDao

    abstract fun attachmentDao(): AttachmentDao

    abstract fun maintenanceDao(): MaintenanceDao

    abstract fun historyDao(): HistoryDao
}
