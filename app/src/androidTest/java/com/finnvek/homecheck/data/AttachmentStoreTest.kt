package com.finnvek.homecheck.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.finnvek.homecheck.data.files.AttachmentStore
import com.finnvek.homecheck.data.local.entity.AttachmentType
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayInputStream

@RunWith(AndroidJUnit4::class)
class AttachmentStoreTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val store = AttachmentStore(context)

    @Test fun importedAttachmentUsesGeneratedInternalNameAndCanBeDeleted() =
        runTest {
            val bytes = "homecheck manual".encodeToByteArray()
            val attachment =
                store.importFromStream(
                    assetId = "asset-1",
                    type = AttachmentType.MANUAL,
                    displayName = "../../private-manual.pdf",
                    mimeType = "application/pdf",
                    input = ByteArrayInputStream(bytes),
                )

            val file = store.fileFor(attachment.localPath)
            assertTrue(file.isFile)
            assertArrayEquals(bytes, file.readBytes())
            assertNotEquals(attachment.displayName, file.name)
            assertFalse(attachment.localPath.contains(".."))

            store.delete(attachment.localPath)
            assertFalse(file.exists())
        }
}
