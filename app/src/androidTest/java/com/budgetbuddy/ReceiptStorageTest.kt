package com.budgetbuddy

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
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
        val source = File(context.cacheDir, "receipt-gallery-source.png")
        Bitmap.createBitmap(24, 12, Bitmap.Config.ARGB_8888).apply {
            eraseColor(android.graphics.Color.CYAN)
            source.outputStream().use { compress(Bitmap.CompressFormat.PNG, 100, it) }
            recycle()
        }
        val imported = ReceiptStorage.importFromGallery(context, Uri.fromFile(source))

        try {
            assertTrue(ReceiptStorage.isUsableOwnedReceipt(context, imported))
            assertTrue(imported.extension.equals("jpg", ignoreCase = true))
            val decoded = requireNotNull(BitmapFactory.decodeFile(imported.absolutePath))
            assertTrue(decoded.width > 0 && decoded.height > 0)
            decoded.recycle()
            ReceiptStorage.deleteIfOwned(context, imported.absolutePath)
            assertFalse(imported.exists())
        } finally {
            source.delete()
            imported.delete()
        }
    }

    @Test
    fun invalidGalleryContentIsRejectedWithoutLeavingAReceipt() {
        val source = File(context.cacheDir, "not-an-image.bin").apply { writeText("not an image") }
        try {
            val result = runCatching { ReceiptStorage.importFromGallery(context, Uri.fromFile(source)) }
            assertTrue(result.isFailure)
        } finally {
            source.delete()
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

    @Test
    fun normalizedReceiptSurvivesTransactionSaveAndIsRemovedWithTransaction() {
        val source = File(context.cacheDir, "saved-transaction-receipt.png")
        Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888).apply {
            eraseColor(android.graphics.Color.MAGENTA)
            source.outputStream().use { compress(Bitmap.CompressFormat.PNG, 100, it) }
            recycle()
        }
        val imported = ReceiptStorage.importFromGallery(context, Uri.fromFile(source))
        val transactionId = "receipt-storage-test-${System.nanoTime()}"
        val localData = LocalDataStore(context)
        try {
            localData.saveTransaction(Transaction(
                transactionId = transactionId,
                categoryId = "Other",
                amount = 1.0,
                date = "2026-08-27",
                photoPath = imported.absolutePath
            ))
            val restored = localData.getTransaction(transactionId)?.photoPath?.let(::File)
            assertTrue(ReceiptStorage.isUsableOwnedReceipt(context, restored))
            assertTrue(localData.deleteTransaction(transactionId))
            assertFalse(imported.exists())
        } finally {
            localData.deleteTransaction(transactionId)
            source.delete()
            imported.delete()
        }
    }
}
