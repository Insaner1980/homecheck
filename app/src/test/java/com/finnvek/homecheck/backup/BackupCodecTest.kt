package com.finnvek.homecheck.backup

import org.junit.Assert.assertEquals
import org.junit.Test

class BackupCodecTest {
    private val backup = HomeCheckBackup(
        schemaVersion = BackupCodec.SUPPORTED_SCHEMA_VERSION,
        exportedAtEpochMillis = 1_786_089_600_000,
        assets = listOf(
            BackupAsset(
                id = "asset-1",
                name = "Dishwasher",
                createdAt = 100,
                updatedAt = 200,
                manufacturer = "Bosch",
            ),
        ),
        attachments = listOf(
            BackupAttachment(
                id = "attachment-1",
                assetId = "asset-1",
                type = "MANUAL",
                displayName = "Manual.pdf",
                mimeType = "application/pdf",
                archivePath = "attachments/attachment-1.pdf",
                createdAt = 300,
            ),
        ),
        tasks = listOf(
            BackupTask(
                id = "task-1",
                assetId = "asset-1",
                title = "Clean filter",
                dueDate = "2026-09-07",
                recurrenceInterval = 1,
                recurrenceUnit = "MONTHS",
                reminderEnabled = true,
                createdAt = 400,
                updatedAt = 500,
            ),
        ),
        history = listOf(
            BackupHistory(
                id = "history-1",
                assetId = "asset-1",
                sourceTaskId = "task-1",
                title = "Clean filter",
                completedAt = 600,
            ),
        ),
    )

    @Test fun `backup JSON round trips without losing records`() {
        val encoded = BackupCodec.encode(backup)
        assertEquals(backup, BackupCodec.decode(encoded))
    }

    @Test(expected = UnsupportedBackupVersionException::class)
    fun `unsupported schema is rejected`() {
        BackupCodec.decode("""{"schemaVersion":99,"exportedAtEpochMillis":0,"assets":[],"attachments":[],"tasks":[],"history":[]}""")
    }
}

