package com.finnvek.homecheck.data.repository

import androidx.room.withTransaction
import com.finnvek.homecheck.data.local.HomeCheckDatabase
import com.finnvek.homecheck.data.local.entity.AssetEntity
import com.finnvek.homecheck.data.local.entity.AttachmentEntity
import com.finnvek.homecheck.data.local.entity.AttachmentType
import com.finnvek.homecheck.data.local.entity.MaintenanceHistoryEntity
import com.finnvek.homecheck.data.local.entity.MaintenanceTaskEntity
import com.finnvek.homecheck.domain.MaintenanceCompletionPlan
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

data class CompletionResult(
    val previousTask: MaintenanceTaskEntity,
    val historyEntry: MaintenanceHistoryEntity,
    val nextDueDate: LocalDate?,
)

data class DatabaseSnapshot(
    val assets: List<AssetEntity>,
    val attachments: List<AttachmentEntity>,
    val tasks: List<MaintenanceTaskEntity>,
    val history: List<MaintenanceHistoryEntity>,
)

@Singleton
class HomeRepository @Inject constructor(
    private val database: HomeCheckDatabase,
) {
    private val assetsDao = database.assetDao()
    private val attachmentsDao = database.attachmentDao()
    private val tasksDao = database.maintenanceDao()
    private val historyDao = database.historyDao()

    val assets: Flow<List<AssetEntity>> = assetsDao.observeAll()
    val tasks: Flow<List<MaintenanceTaskEntity>> = tasksDao.observeAll()
    val history: Flow<List<MaintenanceHistoryEntity>> = historyDao.observeAll()
    val attachments: Flow<List<AttachmentEntity>> = attachmentsDao.observeAll()

    fun asset(id: String): Flow<AssetEntity?> = assetsDao.observeById(id)
    fun tasksForAsset(assetId: String): Flow<List<MaintenanceTaskEntity>> = tasksDao.observeForAsset(assetId)
    fun historyForAsset(assetId: String): Flow<List<MaintenanceHistoryEntity>> = historyDao.observeForAsset(assetId)
    fun attachmentsForAsset(assetId: String): Flow<List<AttachmentEntity>> = attachmentsDao.observeForAsset(assetId)

    suspend fun assetNow(id: String): AssetEntity? = assetsDao.getById(id)
    suspend fun task(id: String): MaintenanceTaskEntity? = tasksDao.getById(id)
    suspend fun assetCount(): Int = assetsDao.count()
    suspend fun attachmentsNow(assetId: String): List<AttachmentEntity> = attachmentsDao.getForAsset(assetId)
    suspend fun attachment(id: String): AttachmentEntity? = attachmentsDao.getById(id)

    suspend fun upsertAsset(asset: AssetEntity) = assetsDao.upsert(asset)
    suspend fun upsertTask(task: MaintenanceTaskEntity) = tasksDao.upsert(task)
    suspend fun upsertAttachment(attachment: AttachmentEntity) = attachmentsDao.upsert(attachment)
    suspend fun deleteAsset(id: String) = assetsDao.delete(id)
    suspend fun deleteTask(id: String) = tasksDao.delete(id)
    suspend fun deleteAttachment(id: String) = attachmentsDao.delete(id)
    suspend fun renameAttachment(id: String, name: String) = attachmentsDao.rename(id, name)
    suspend fun changeAttachmentType(id: String, type: AttachmentType) = attachmentsDao.changeType(id, type)

    suspend fun completeMaintenance(taskId: String, completedOn: LocalDate, note: String?): CompletionResult =
        database.withTransaction {
            val task = requireNotNull(tasksDao.getById(taskId)) { "Maintenance task not found" }
            val plan = MaintenanceCompletionPlan.create(
                taskId = task.id,
                assetId = task.assetId,
                title = task.title,
                recurrence = task.recurrence,
                completedOn = completedOn,
            )
            val historyEntry = MaintenanceHistoryEntity(
                id = UUID.randomUUID().toString(),
                assetId = task.assetId,
                sourceTaskId = task.id,
                titleSnapshot = task.title,
                completedAt = System.currentTimeMillis(),
                note = note?.trim()?.takeIf(String::isNotEmpty),
            )
            historyDao.insert(historyEntry)
            if (plan.removeActiveTask) {
                tasksDao.delete(task.id)
            } else {
                tasksDao.upsert(task.copy(dueDate = requireNotNull(plan.nextDueDate), updatedAt = System.currentTimeMillis()))
            }
            CompletionResult(task, historyEntry, plan.nextDueDate)
        }

    suspend fun undoCompletion(result: CompletionResult) = database.withTransaction {
        historyDao.delete(result.historyEntry.id)
        tasksDao.upsert(result.previousTask)
    }

    suspend fun snapshot(): DatabaseSnapshot = DatabaseSnapshot(
        assets = assetsDao.getAll(),
        attachments = attachmentsDao.getAll(),
        tasks = tasksDao.getAll(),
        history = historyDao.getAll(),
    )

    suspend fun replaceAll(snapshot: DatabaseSnapshot) = database.withTransaction {
        assetsDao.deleteAll()
        assetsDao.upsertAll(snapshot.assets)
        attachmentsDao.upsertAll(snapshot.attachments)
        tasksDao.upsertAll(snapshot.tasks)
        historyDao.insertAll(snapshot.history)
    }
}
