package com.finnvek.homecheck.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class BackupCodecTest {
    private val backup =
        HomeCheckBackup(
            schemaVersion = BackupCodec.SUPPORTED_SCHEMA_VERSION,
            exportedAtEpochMillis = 1_786_089_600_000,
            assets =
                listOf(
                    BackupAsset(
                        id = "asset-1",
                        name = "Dishwasher",
                        createdAt = 100,
                        updatedAt = 200,
                        manufacturer = "Bosch",
                    ),
                ),
            attachments =
                listOf(
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
            tasks =
                listOf(
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
            history =
                listOf(
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

    @Test fun `invalid reminder enabled type is reported as an invalid manifest`() {
        val manifest =
            """
            {
              "schemaVersion": 1,
              "exportedAtEpochMillis": 0,
              "assets": [],
              "attachments": [],
              "tasks": [
                {
                  "id": "task-1",
                  "assetId": "asset-1",
                  "title": "Clean filter",
                  "dueDate": "2026-09-07",
                  "reminderEnabled": "yes",
                  "createdAt": 1,
                  "updatedAt": 1
                }
              ],
              "history": []
            }
            """.trimIndent()

        val manifestFile = File.createTempFile("homecheck-invalid-manifest", ".json")
        val error =
            try {
                manifestFile.writeText(manifest)
                assertThrows(InvalidBackupException::class.java) { decodeBackupManifest(manifestFile) }
            } finally {
                manifestFile.delete()
            }

        assertEquals("Backup manifest is invalid", error.message)
        assertTrue(error.cause is IllegalStateException)
    }

    @Test fun `manifest larger than four mebibytes is rejected before decoding`() {
        val manifestFile = File.createTempFile("homecheck-large-manifest", ".json")
        val error =
            try {
                manifestFile.writeText("x".repeat(4 * 1024 * 1024 + 1))
                assertThrows(InvalidBackupException::class.java) { decodeBackupManifest(manifestFile) }
            } finally {
                manifestFile.delete()
            }

        assertEquals("Backup manifest is too large", error.message)
    }

    @Test fun `oversized restored metadata is rejected`() {
        val manifestFile = File.createTempFile("homecheck-large-metadata", ".json")
        val error =
            try {
                manifestFile.writeText(
                    BackupCodec.encode(
                        backup.copy(
                            assets = listOf(backup.assets.single().copy(notes = "x".repeat(10_001))),
                        ),
                    ),
                )
                assertThrows(InvalidBackupException::class.java) { decodeBackupManifest(manifestFile) }
            } finally {
                manifestFile.delete()
            }

        assertEquals("Backup contains oversized metadata", error.message)
    }

    @Test fun `backup with excessive record count is rejected`() {
        val manifestFile = File.createTempFile("homecheck-many-records", ".json")
        val error =
            try {
                manifestFile.writeText(
                    BackupCodec.encode(
                        backup.copy(
                            assets = emptyList(),
                            attachments = emptyList(),
                            tasks = emptyList(),
                            history =
                                List(10_001) {
                                    BackupHistory(
                                        id = "history-$it",
                                        assetId = "asset-1",
                                        title = "Done",
                                        completedAt = it.toLong(),
                                    )
                                },
                        ),
                    ),
                )
                assertThrows(InvalidBackupException::class.java) { decodeBackupManifest(manifestFile) }
            } finally {
                manifestFile.delete()
            }

        assertEquals("Backup contains too many records", error.message)
    }

    @Test fun `excessive aggregate restored metadata is rejected`() {
        val manifestFile = File.createTempFile("homecheck-metadata-total", ".json")
        val error =
            try {
                manifestFile.writeText(
                    BackupCodec.encode(
                        backup.copy(
                            assets =
                                List(2_001) {
                                    backup.assets.single().copy(
                                        id = "asset-$it",
                                        name = "A",
                                        category = "x".repeat(500),
                                    )
                                },
                        ),
                    ),
                )
                assertThrows(InvalidBackupException::class.java) { decodeBackupManifest(manifestFile) }
            } finally {
                manifestFile.delete()
            }

        assertEquals("Backup contains too much metadata", error.message)
    }
}
