package com.example.budgetbuddy

import android.content.Context
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class ReceiptStorageTest {
    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun galleryImageIsCopiedToOwnedStorageAndCanBeDeletedSafely() {
        val expected = "test-image-content".encodeToByteArray()
        val source = File(context.cacheDir, "receipt-gallery-source.bin").apply { writeBytes(expected) }
        val imported = ReceiptStorage.importFromGallery(context, Uri.fromFile(source))

        try {
            assertTrue(ReceiptStorage.isUsableOwnedReceipt(context, imported))
            assertArrayEquals(expected, imported.readBytes())
            ReceiptStorage.deleteIfOwned(context, imported.absolutePath)
            assertFalse(imported.exists())
        } finally {
            source.delete()
            imported.delete()
        }
    }

    @Test
    fun unrelatedFilesAreNeverAcceptedAsReceipts() {
        val unrelated = File(context.cacheDir, "not-a-receipt.jpg").apply { writeBytes(byteArrayOf(1)) }
        try {
            assertFalse(ReceiptStorage.isUsableOwnedReceipt(context, unrelated))
            ReceiptStorage.deleteIfOwned(context, unrelated.absolutePath)
            assertTrue(unrelated.exists())
        } finally {
            unrelated.delete()
        }
    }
}
