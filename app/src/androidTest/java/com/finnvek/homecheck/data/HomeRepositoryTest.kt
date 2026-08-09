package com.finnvek.homecheck.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.finnvek.homecheck.data.local.HomeCheckDatabase
import com.finnvek.homecheck.data.local.entity.AssetEntity
import com.finnvek.homecheck.data.local.entity.AttachmentEntity
import com.finnvek.homecheck.data.local.entity.AttachmentType
import com.finnvek.homecheck.data.local.entity.MaintenanceHistoryEntity
import com.finnvek.homecheck.data.local.entity.MaintenanceTaskEntity
import com.finnvek.homecheck.data.repository.HomeRepository
import com.finnvek.homecheck.domain.RecurrenceUnit
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class HomeRepositoryTest {
    private lateinit var database: HomeCheckDatabase
    private lateinit var repository: HomeRepository

    @Before fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database =
            Room
                .inMemoryDatabaseBuilder(context, HomeCheckDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        repository = HomeRepository(database)
    }

    @After fun tearDown() = database.close()

    @Test fun recurringCompletionCreatesHistoryAndAdvancesFromActualCompletion() =
        runTest {
            repository.upsertAsset(asset())
            repository.upsertTask(
                task(recurrenceInterval = 3, recurrenceUnit = RecurrenceUnit.MONTHS),
            )

            repository.completeMaintenance("task-1", LocalDate.of(2026, 8, 7), "Rinsed and dried")

            val active = repository.task("task-1")
            val history = repository.historyForAsset("asset-1").first()
            assertEquals(LocalDate.of(2026, 11, 7), active?.dueDate)
            assertEquals(1, history.size)
            assertEquals("Clean filter", history.single().titleSnapshot)
            assertEquals("Rinsed and dried", history.single().note)
        }

    @Test fun oneTimeCompletionCreatesHistoryAndRemovesActiveTask() =
        runTest {
            repository.upsertAsset(asset())
            repository.upsertTask(task())

            repository.completeMaintenance("task-1", LocalDate.of(2026, 8, 7), null)

            assertNull(repository.task("task-1"))
            assertEquals(
                "Clean filter",
                repository
                    .historyForAsset("asset-1")
                    .first()
                    .single()
                    .titleSnapshot,
            )
        }

    @Test fun deletingAssetCascadesItsDatabaseRecords() =
        runTest {
            repository.upsertAsset(asset())
            repository.upsertTask(task())
            database.attachmentDao().upsert(
                AttachmentEntity(
                    id = "attachment-1",
                    assetId = "asset-1",
                    type = AttachmentType.MANUAL,
                    displayName = "Manual.pdf",
                    mimeType = "application/pdf",
                    localPath = "asset-1/file.pdf",
                    createdAt = 1,
                ),
            )
            database.historyDao().insert(
                MaintenanceHistoryEntity(
                    id = "history-1",
                    assetId = "asset-1",
                    sourceTaskId = "task-1",
                    titleSnapshot = "Clean filter",
                    completedAt = 1,
                ),
            )

            repository.deleteAsset("asset-1")

            assertTrue(repository.assets.first().isEmpty())
            assertTrue(repository.tasks.first().isEmpty())
            assertTrue(
                database
                    .attachmentDao()
                    .observeForAsset("asset-1")
                    .first()
                    .isEmpty(),
            )
            assertTrue(repository.historyForAsset("asset-1").first().isEmpty())
        }

    private fun asset() =
        AssetEntity(
            id = "asset-1",
            name = "Heat pump",
            createdAt = 1,
            updatedAt = 1,
        )

    private fun task(
        recurrenceInterval: Int? = null,
        recurrenceUnit: RecurrenceUnit? = null,
    ) = MaintenanceTaskEntity(
        id = "task-1",
        assetId = "asset-1",
        title = "Clean filter",
        dueDate = LocalDate.of(2026, 8, 1),
        recurrenceInterval = recurrenceInterval,
        recurrenceUnit = recurrenceUnit,
        createdAt = 1,
        updatedAt = 1,
    )
}
