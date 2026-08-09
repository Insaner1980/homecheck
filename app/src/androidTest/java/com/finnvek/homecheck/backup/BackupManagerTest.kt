package com.finnvek.homecheck.backup

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.finnvek.homecheck.data.DataMutationGate
import com.finnvek.homecheck.data.files.AttachmentStore
import com.finnvek.homecheck.data.local.HomeCheckDatabase
import com.finnvek.homecheck.data.local.entity.AssetEntity
import com.finnvek.homecheck.data.local.entity.AttachmentType
import com.finnvek.homecheck.data.repository.HomeRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File

@RunWith(AndroidJUnit4::class)
class BackupManagerTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var database: HomeCheckDatabase
    private lateinit var repository: HomeRepository
    private lateinit var store: AttachmentStore
    private lateinit var manager: BackupManager
    private lateinit var mutationGate: DataMutationGate

    @Before fun setUp() {
        database =
            Room
                .inMemoryDatabaseBuilder(context, HomeCheckDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        mutationGate = DataMutationGate()
        repository = HomeRepository(database, mutationGate)
        store = AttachmentStore(context, mutationGate)
        manager = BackupManager(context, repository, store, mutationGate)
    }

    @After fun tearDown() = database.close()

    @Test fun backupRoundTripRestoresRecordsAndAttachmentBytes() =
        runTest {
            val asset = AssetEntity("asset-backup", "Boiler", 1, 1, location = "Basement")
            repository.upsertAsset(asset)
            val bytes = "warranty document".encodeToByteArray()
            val attachment =
                store.importFromStream(
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

    @Test fun invalidBackupLeavesExistingDataUntouched() =
        runTest {
            val asset = AssetEntity("asset-safe", "Smoke alarm", 1, 1)
            repository.upsertAsset(asset)

            runCatching { manager.restoreFrom(ByteArrayInputStream("not a zip".encodeToByteArray())) }

            assertEquals(asset, repository.assets.first().single())
            assertTrue(repository.attachmentsForAsset(asset.id).first().isEmpty())
        }

    @Test fun cancellationAfterAttachmentReplacementRestoresOriginalDataAndFiles() =
        runTest {
            val incomingAsset = AssetEntity("asset-incoming", "Incoming boiler", 1, 1)
            repository.upsertAsset(incomingAsset)
            val incomingAttachment =
                store.importFromStream(
                    incomingAsset.id,
                    AttachmentType.MANUAL,
                    "Incoming.pdf",
                    "application/pdf",
                    ByteArrayInputStream("incoming".encodeToByteArray()),
                )
            repository.upsertAttachment(incomingAttachment)
            val backup = ByteArrayOutputStream()
            manager.exportTo(backup)
            repository.deleteAsset(incomingAsset.id)
            store.delete(incomingAttachment.localPath)

            val originalAsset = AssetEntity("asset-original", "Original boiler", 2, 2)
            val originalBytes = "original".encodeToByteArray()
            repository.upsertAsset(originalAsset)
            val originalAttachment =
                store.importFromStream(
                    originalAsset.id,
                    AttachmentType.WARRANTY,
                    "Original.pdf",
                    "application/pdf",
                    ByteArrayInputStream(originalBytes),
                )
            repository.upsertAttachment(originalAttachment)
            val cancellingManager =
                BackupManager.createForTest(context, repository, store, mutationGate) {
                    throw CancellationException("cancel after replacement")
                }

            val failure = runCatching { cancellingManager.restoreFrom(ByteArrayInputStream(backup.toByteArray())) }.exceptionOrNull()

            assertTrue(failure is CancellationException)
            assertEquals(originalAsset, repository.assets.first().single())
            val retainedAttachment = repository.attachmentsForAsset(originalAsset.id).first().single()
            assertEquals(originalAttachment, retainedAttachment)
            assertArrayEquals(originalBytes, store.fileFor(retainedAttachment.localPath).readBytes())
            store.delete(retainedAttachment.localPath)
        }

    @Test fun sharedGateAllowsUriImportAndKeepsCompetitorOutOfMultiStepMutation() =
        runTest {
            val source = File.createTempFile("homecheck-import", ".pdf", context.cacheDir).apply { writeText("manual") }
            val asset = AssetEntity("asset-import", "Imported boiler", 1, 1)
            val outerEntered = CompletableDeferred<Unit>()
            val operationCompleted = CompletableDeferred<com.finnvek.homecheck.data.local.entity.AttachmentEntity>()
            val releaseOuter = CompletableDeferred<Unit>()
            val events = mutableListOf<String>()
            val outer =
                async {
                    mutationGate.withLock {
                        outerEntered.complete(Unit)
                        val attachment = store.importFromUri(asset.id, AttachmentType.MANUAL, Uri.fromFile(source))
                        repository.upsertAsset(asset)
                        repository.upsertAttachment(attachment)
                        events += "operation"
                        operationCompleted.complete(attachment)
                        releaseOuter.await()
                        events += "outer-end"
                    }
                }
            outerEntered.await()
            val competitor = async { mutationGate.withLock { events += "competitor" } }

            val attachment = withTimeout(5_000) { operationCompleted.await() }
            yield()
            assertEquals(listOf("operation"), events)
            releaseOuter.complete(Unit)
            outer.await()
            competitor.await()
            assertEquals(listOf("operation", "outer-end", "competitor"), events)
            assertArrayEquals("manual".encodeToByteArray(), store.fileFor(attachment.localPath).readBytes())

            store.delete(attachment.localPath)
            repository.deleteAsset(asset.id)
            source.delete()
        }
}
