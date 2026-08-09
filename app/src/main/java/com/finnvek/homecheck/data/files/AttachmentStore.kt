package com.finnvek.homecheck.data.files

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import com.finnvek.homecheck.BuildConfig
import com.finnvek.homecheck.data.DataMutationGate
import com.finnvek.homecheck.data.local.entity.AttachmentEntity
import com.finnvek.homecheck.data.local.entity.AttachmentType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AttachmentStore
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        private val mutationGate: DataMutationGate = DataMutationGate(),
    ) {
        private val root = File(context.filesDir, "attachments").apply(File::mkdirs)

        suspend fun importFromUri(
            assetId: String,
            type: AttachmentType,
            uri: Uri,
        ): AttachmentEntity =
            mutationGate.withLock {
                withContext(Dispatchers.IO) {
                    val resolver = context.contentResolver
                    val mimeType = resolver.getType(uri) ?: "application/octet-stream"
                    val displayName = resolver.displayName(uri) ?: defaultDisplayName(type, mimeType)
                    resolver.openInputStream(uri).use { input ->
                        requireNotNull(input) { "Selected file could not be opened" }
                        importFromStreamUnlocked(assetId, type, displayName, mimeType, input)
                    }
                }
            }

        suspend fun importFromStream(
            assetId: String,
            type: AttachmentType,
            displayName: String,
            mimeType: String,
            input: InputStream,
        ): AttachmentEntity =
            mutationGate.withLock {
                withContext(Dispatchers.IO) { importFromStreamUnlocked(assetId, type, displayName, mimeType, input) }
            }

        private fun importFromStreamUnlocked(
            assetId: String,
            type: AttachmentType,
            displayName: String,
            mimeType: String,
            input: InputStream,
        ): AttachmentEntity {
            val id = UUID.randomUUID().toString()
            val filename = "$id${extensionFor(mimeType)}"
            val target = File(root, filename)
            var imported = false
            try {
                target.outputStream().buffered().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var total = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        total += read
                        require(total <= MAX_ATTACHMENT_BYTES) { "Selected file is too large" }
                        output.write(buffer, 0, read)
                    }
                }
                imported = true
            } finally {
                if (!imported) target.delete()
            }
            return AttachmentEntity(
                id = id,
                assetId = assetId,
                type = type,
                displayName = displayName.take(MAX_DISPLAY_NAME_LENGTH),
                mimeType = mimeType,
                localPath = filename,
                createdAt = System.currentTimeMillis(),
            )
        }

        fun fileFor(localPath: String): File {
            require(localPath.matches(Regex("[A-Za-z0-9._-]+"))) { "Unsafe attachment path" }
            return File(root, localPath)
        }

        fun uriFor(localPath: String): Uri = FileProvider.getUriForFile(context, "${BuildConfig.APPLICATION_ID}.files", fileFor(localPath))

        fun createCameraFile(): File =
            File(
                File(context.cacheDir, "camera").apply(File::mkdirs),
                "${UUID.randomUUID()}.jpg",
            )

        fun uriForCameraFile(file: File): Uri = FileProvider.getUriForFile(context, "${BuildConfig.APPLICATION_ID}.files", file)

        suspend fun delete(localPath: String) = mutationGate.withLock { withContext(Dispatchers.IO) { fileFor(localPath).delete() } }

        suspend fun deleteAll(attachments: List<AttachmentEntity>) =
            mutationGate.withLock {
                withContext(Dispatchers.IO) {
                    attachments.forEach { fileFor(it.localPath).delete() }
                }
            }

        suspend fun replaceAllFrom(stagedAttachments: File): ReplacementHandle =
            mutationGate.withLock {
                withContext(Dispatchers.IO) {
                    require(stagedAttachments.isDirectory) { "Restored attachments are missing" }
                    val rollback = File(context.cacheDir, "attachment-rollback-${UUID.randomUUID()}")
                    if (root.exists() && !root.renameTo(rollback)) {
                        root.copyRecursively(rollback, overwrite = true)
                        root.deleteRecursively()
                    }
                    var replaced = false
                    try {
                        root.mkdirs()
                        stagedAttachments.copyRecursively(root, overwrite = true)
                        replaced = true
                        ReplacementHandle(rollback)
                    } finally {
                        if (!replaced) {
                            root.deleteRecursively()
                            restoreRollback(rollback)
                        }
                    }
                }
            }

        suspend fun commitReplacement(handle: ReplacementHandle) =
            mutationGate.withLock {
                withContext(Dispatchers.IO) {
                    handle.rollbackDirectory.deleteRecursively()
                }
            }

        suspend fun rollbackReplacement(handle: ReplacementHandle) =
            mutationGate.withLock {
                withContext(Dispatchers.IO) {
                    root.deleteRecursively()
                    restoreRollback(handle.rollbackDirectory)
                }
            }

        private fun restoreRollback(rollback: File) {
            if (!rollback.exists()) {
                root.mkdirs()
            } else if (!rollback.renameTo(root)) {
                rollback.copyRecursively(root, overwrite = true)
                rollback.deleteRecursively()
            }
        }

        private fun extensionFor(mimeType: String) =
            when (mimeType.lowercase()) {
                "application/pdf" -> ".pdf"
                "image/jpeg" -> ".jpg"
                "image/png" -> ".png"
                "image/webp" -> ".webp"
                else -> ".bin"
            }

        private fun defaultDisplayName(
            type: AttachmentType,
            mimeType: String,
        ) = "${type.name.lowercase().replace('_', '-')}${extensionFor(mimeType)}"

        private companion object {
            const val MAX_ATTACHMENT_BYTES = 128L * 1024 * 1024
            private const val MAX_DISPLAY_NAME_LENGTH = 180
        }
    }

class ReplacementHandle internal constructor(
    internal val rollbackDirectory: File,
)

private fun ContentResolver.displayName(uri: Uri): String? =
    query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) cursor.getString(0) else null
    }
