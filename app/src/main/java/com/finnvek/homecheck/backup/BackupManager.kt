package com.finnvek.homecheck.backup

import android.content.Context
import android.net.Uri
import com.finnvek.homecheck.data.files.AttachmentStore
import com.finnvek.homecheck.data.local.entity.AssetEntity
import com.finnvek.homecheck.data.local.entity.AttachmentEntity
import com.finnvek.homecheck.data.local.entity.AttachmentType
import com.finnvek.homecheck.data.local.entity.MaintenanceHistoryEntity
import com.finnvek.homecheck.data.local.entity.MaintenanceTaskEntity
import com.finnvek.homecheck.data.repository.DatabaseSnapshot
import com.finnvek.homecheck.data.repository.HomeRepository
import com.finnvek.homecheck.domain.RecurrenceUnit
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.time.LocalDate
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class InvalidBackupException(message: String, cause: Throwable? = null) : IllegalArgumentException(message, cause)

@Singleton
class BackupManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val repository: HomeRepository,
    private val attachmentStore: AttachmentStore,
) {
    suspend fun export(uri: Uri) = withContext(Dispatchers.IO) {
        context.contentResolver.openOutputStream(uri, "w").use { output ->
            requireNotNull(output) { "Backup destination could not be opened" }
            exportTo(output)
        }
    }

    suspend fun restore(uri: Uri) = withContext(Dispatchers.IO) {
        context.contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "Backup file could not be opened" }
            restoreFrom(input)
        }
    }

    suspend fun exportTo(output: OutputStream) = withContext(Dispatchers.IO) {
        val snapshot = repository.snapshot()
        val backup = snapshot.toBackup()
        ZipOutputStream(output.buffered()).use { zip ->
            zip.putNextEntry(ZipEntry(MANIFEST_ENTRY))
            zip.write(BackupCodec.encode(backup).encodeToByteArray())
            zip.closeEntry()
            snapshot.attachments.forEach { attachment ->
                val file = attachmentStore.fileFor(attachment.localPath)
                require(file.isFile) { "Attachment file is missing: ${attachment.displayName}" }
                zip.putNextEntry(ZipEntry("attachments/${attachment.localPath}"))
                file.inputStream().buffered().use { it.copyTo(zip) }
                zip.closeEntry()
            }
        }
    }

    suspend fun restoreFrom(input: InputStream) = withContext(Dispatchers.IO) {
        val staging = File(context.cacheDir, "restore-${UUID.randomUUID()}").apply { mkdirs() }
        try {
            extractValidated(input, staging)
            val manifest = File(staging, MANIFEST_ENTRY)
            if (!manifest.isFile) throw InvalidBackupException("Backup manifest is missing")
            val backup = try {
                BackupCodec.decode(manifest.readText())
            } catch (error: UnsupportedBackupVersionException) {
                throw error
            } catch (error: Throwable) {
                throw InvalidBackupException("Backup manifest is invalid", error)
            }
            val restored = backup.toSnapshot(staging)
            val oldSnapshot = repository.snapshot()
            val stagedAttachments = File(staging, "attachments").apply(File::mkdirs)
            val replacement = attachmentStore.replaceAllFrom(stagedAttachments)
            try {
                repository.replaceAll(restored)
                attachmentStore.commitReplacement(replacement)
            } catch (error: Throwable) {
                attachmentStore.rollbackReplacement(replacement)
                repository.replaceAll(oldSnapshot)
                throw error
            }
        } finally {
            staging.deleteRecursively()
        }
    }

    private fun extractValidated(input: InputStream, staging: File) {
        val seen = mutableSetOf<String>()
        var entryCount = 0
        var totalBytes = 0L
        ZipInputStream(input.buffered()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                entryCount++
                if (entryCount > MAX_ENTRIES) throw InvalidBackupException("Backup contains too many files")
                if (entry.isDirectory || !BackupPathValidator.isSafe(entry.name) || !seen.add(entry.name)) {
                    throw InvalidBackupException("Backup contains an unsafe file path")
                }
                val target = File(staging, entry.name)
                val stagingPath = staging.canonicalPath + File.separator
                if (!target.canonicalPath.startsWith(stagingPath)) throw InvalidBackupException("Backup contains an unsafe file path")
                target.parentFile?.mkdirs()
                target.outputStream().buffered().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var entryBytes = 0L
                    while (true) {
                        val read = zip.read(buffer)
                        if (read < 0) break
                        entryBytes += read
                        totalBytes += read
                        if (entryBytes > MAX_ENTRY_BYTES || totalBytes > MAX_TOTAL_BYTES) {
                            throw InvalidBackupException("Backup is too large")
                        }
                        output.write(buffer, 0, read)
                    }
                }
                zip.closeEntry()
            }
        }
    }

    companion object {
        private const val MANIFEST_ENTRY = "manifest.json"
        private const val MAX_ENTRIES = 10_000
        private const val MAX_ENTRY_BYTES = 128L * 1024 * 1024
        private const val MAX_TOTAL_BYTES = 512L * 1024 * 1024
    }
}

private fun DatabaseSnapshot.toBackup() = HomeCheckBackup(
    schemaVersion = BackupCodec.SUPPORTED_SCHEMA_VERSION,
    exportedAtEpochMillis = System.currentTimeMillis(),
    assets = assets.map { asset ->
        BackupAsset(
            id = asset.id, name = asset.name, createdAt = asset.createdAt, updatedAt = asset.updatedAt,
            category = asset.category, location = asset.location, manufacturer = asset.manufacturer,
            modelNumber = asset.modelNumber, serialNumber = asset.serialNumber, purchaseDate = asset.purchaseDate?.toString(),
            retailer = asset.retailer, warrantyExpirationDate = asset.warrantyExpirationDate?.toString(), notes = asset.notes,
        )
    },
    attachments = attachments.map { attachment ->
        BackupAttachment(
            id = attachment.id, assetId = attachment.assetId, type = attachment.type.name,
            displayName = attachment.displayName, mimeType = attachment.mimeType,
            archivePath = "attachments/${attachment.localPath}", createdAt = attachment.createdAt,
        )
    },
    tasks = tasks.map { task ->
        BackupTask(
            id = task.id, assetId = task.assetId, title = task.title, dueDate = task.dueDate.toString(), notes = task.notes,
            recurrenceInterval = task.recurrenceInterval, recurrenceUnit = task.recurrenceUnit?.name,
            reminderEnabled = task.reminderEnabled, createdAt = task.createdAt, updatedAt = task.updatedAt,
        )
    },
    history = history.map { entry ->
        BackupHistory(
            id = entry.id, assetId = entry.assetId, sourceTaskId = entry.sourceTaskId,
            title = entry.titleSnapshot, completedAt = entry.completedAt, note = entry.note,
        )
    },
)

private fun HomeCheckBackup.toSnapshot(staging: File): DatabaseSnapshot {
    assets.forEach { asset ->
        requireSafeId("asset", asset.id)
        requireText("asset name", asset.name, 200)
    }
    attachments.forEach { attachment ->
        requireSafeId("attachment", attachment.id)
        requireSafeId("asset", attachment.assetId)
        requireText("attachment name", attachment.displayName, 180)
        requireText("attachment MIME type", attachment.mimeType, 200)
    }
    tasks.forEach { task ->
        requireSafeId("maintenance task", task.id)
        requireSafeId("asset", task.assetId)
        requireText("maintenance title", task.title, 200)
    }
    history.forEach { entry ->
        requireSafeId("history", entry.id)
        requireSafeId("asset", entry.assetId)
        entry.sourceTaskId?.let { requireSafeId("source task", it) }
    }
    val assetIds = assets.mapTo(mutableSetOf()) { it.id }
    requireUnique("asset", assets.map { it.id })
    requireUnique("attachment", attachments.map { it.id })
    requireUnique("maintenance task", tasks.map { it.id })
    requireUnique("history", history.map { it.id })
    requireUnique("attachment archive path", attachments.map { it.archivePath })

    fun requireAsset(assetId: String) {
        if (assetId !in assetIds) throw InvalidBackupException("Backup contains an orphaned record")
    }

    val restoredAttachments = attachments.map { attachment ->
        requireAsset(attachment.assetId)
        if (attachment.archivePath.count { it == '/' } != 1 ||
            !attachment.archivePath.startsWith("attachments/") ||
            !BackupPathValidator.isSafe(attachment.archivePath)
        ) {
            throw InvalidBackupException("Backup contains an unsafe attachment path")
        }
        val file = File(staging, attachment.archivePath)
        if (!file.isFile) throw InvalidBackupException("Backup attachment is missing")
        val localPath = attachment.archivePath.substringAfterLast('/')
        AttachmentEntity(
            id = attachment.id,
            assetId = attachment.assetId,
            type = enumValueOrInvalid(attachment.type),
            displayName = attachment.displayName.take(180),
            mimeType = attachment.mimeType,
            localPath = localPath,
            createdAt = attachment.createdAt,
        )
    }
    return DatabaseSnapshot(
        assets = assets.map { asset ->
            AssetEntity(
                id = asset.id, name = asset.name, createdAt = asset.createdAt, updatedAt = asset.updatedAt,
                category = asset.category, location = asset.location, manufacturer = asset.manufacturer,
                modelNumber = asset.modelNumber, serialNumber = asset.serialNumber,
                purchaseDate = asset.purchaseDate?.let(::parseDate), retailer = asset.retailer,
                warrantyExpirationDate = asset.warrantyExpirationDate?.let(::parseDate), notes = asset.notes,
            )
        },
        attachments = restoredAttachments,
        tasks = tasks.map { task ->
            requireAsset(task.assetId)
            if ((task.recurrenceInterval == null) != (task.recurrenceUnit == null) || (task.recurrenceInterval ?: 1) <= 0) {
                throw InvalidBackupException("Backup recurrence is invalid")
            }
            MaintenanceTaskEntity(
                id = task.id, assetId = task.assetId, title = task.title, dueDate = parseDate(task.dueDate), notes = task.notes,
                recurrenceInterval = task.recurrenceInterval,
                recurrenceUnit = task.recurrenceUnit?.let { enumValueOrInvalid<RecurrenceUnit>(it) },
                reminderEnabled = task.reminderEnabled, createdAt = task.createdAt, updatedAt = task.updatedAt,
            )
        },
        history = history.map { entry ->
            requireAsset(entry.assetId)
            MaintenanceHistoryEntity(
                id = entry.id, assetId = entry.assetId, sourceTaskId = entry.sourceTaskId,
                titleSnapshot = entry.title, completedAt = entry.completedAt, note = entry.note,
            )
        },
    )
}

private fun requireUnique(label: String, values: List<String>) {
    if (values.size != values.toSet().size) throw InvalidBackupException("Backup contains duplicate $label IDs")
}

private fun requireSafeId(label: String, value: String) {
    if (!value.matches(Regex("[A-Za-z0-9_-]{1,100}"))) {
        throw InvalidBackupException("Backup contains an invalid $label ID")
    }
}

private fun requireText(label: String, value: String, maxLength: Int) {
    if (value.isBlank() || value.length > maxLength) {
        throw InvalidBackupException("Backup contains an invalid $label")
    }
}

private fun parseDate(value: String): LocalDate = try {
    LocalDate.parse(value)
} catch (error: Throwable) {
    throw InvalidBackupException("Backup contains an invalid date", error)
}

private inline fun <reified T : Enum<T>> enumValueOrInvalid(value: String): T = try {
    enumValueOf<T>(value)
} catch (error: Throwable) {
    throw InvalidBackupException("Backup contains an unsupported value", error)
}
