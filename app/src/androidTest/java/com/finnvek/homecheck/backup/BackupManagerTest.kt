package com.finnvek.homecheck.backup

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.finnvek.homecheck.data.files.AttachmentStore
import com.finnvek.homecheck.data.local.HomeCheckDatabase
import com.finnvek.homecheck.data.local.entity.AssetEntity
import com.finnvek.homecheck.data.local.entity.AttachmentType
import com.finnvek.homecheck.data.repository.HomeRepository
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BackupManagerTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var database: HomeCheckDatabase
    private lateinit var repository: HomeRepository
    private lateinit var store: AttachmentStore
    private lateinit var manager: BackupManager

    @Before fun setUp() {
        database = Room.inMemoryDatabaseBuilder(context, HomeCheckDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = HomeRepository(database)
        store = AttachmentStore(context)
        manager = BackupManager(context, repository, store)
    }

    @After fun tearDown() = database.close()

    @Test fun backupRoundTripRestoresRecordsAndAttachmentBytes() = runTest {
        val asset = AssetEntity("asset-backup", "Boiler", 1, 1, location = "Basement")
        repository.upsertAsset(asset)
        val bytes = "warranty document".encodeToByteArray()
        val attachment = store.importFromStream(
            asset.id,
            AttachmentType.WARRANTY,
            "Warranty.pdf",
            "application/pdf",
            ByteArrayInputStream(bytes),
        )
        repository.upsertAttachment(attachment)
        val output = ByteArrayOutputStream()

        manager.exportTo(output)
        store.delete(attachment.localPath)
        repository.deleteAsset(asset.id)
        manager.restoreFrom(ByteArrayInputStream(output.toByteArray()))

        assertEquals(asset, repository.assets.first().single())
        val restored = repository.attachmentsForAsset(asset.id).first().single()
        assertArrayEquals(bytes, store.fileFor(restored.localPath).readBytes())
        store.delete(restored.localPath)
    }

    @Test fun invalidBackupLeavesExistingDataUntouched() = runTest {
        val asset = AssetEntity("asset-safe", "Smoke alarm", 1, 1)
        repository.upsertAsset(asset)

        runCatching { manager.restoreFrom(ByteArrayInputStream("not a zip".encodeToByteArray())) }

        assertEquals(asset, repository.assets.first().single())
        assertTrue(repository.attachmentsForAsset(asset.id).first().isEmpty())
    }
}
