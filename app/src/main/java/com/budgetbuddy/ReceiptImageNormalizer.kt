package com.budgetbuddy

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import java.io.File
import java.util.UUID
import kotlin.math.max

/** Decodes, rotates, scales, and rewrites receipts as dependable local JPEG files. */
object ReceiptImageNormalizer {
    private const val MAX_DIMENSION = 2048
    private const val JPEG_QUALITY = 90

    fun normalize(source: File, directory: File): File {
        require(source.isFile && source.length() in 1..ReceiptFileCopier.DEFAULT_MAX_BYTES) {
            "The selected image is empty or too large"
        }
        check(directory.isDirectory || directory.mkdirs()) { "Receipt storage is unavailable" }

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(source.absolutePath, bounds)
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "The selected file is not a readable image" }

        var sampleSize = 1
        while (max(bounds.outWidth, bounds.outHeight) / sampleSize > MAX_DIMENSION) sampleSize *= 2
        val decoded = requireNotNull(BitmapFactory.decodeFile(
            source.absolutePath,
            BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
        )) { "The selected image could not be decoded" }

        val oriented = orient(decoded, source)
        val temporary = File.createTempFile("receipt-normalized-", ".part", directory)
        val destination = File(directory, "receipt-${UUID.randomUUID()}.jpg")
        try {
            temporary.outputStream().buffered().use { output ->
                check(oriented.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)) {
                    "The selected image could not be saved"
                }
            }
            require(temporary.length() > 0L) { "The saved image is empty" }
            if (!temporary.renameTo(destination)) {
                temporary.copyTo(destination, overwrite = true)
                check(temporary.delete()) { "The temporary receipt could not be removed" }
            }
            return destination
        } catch (error: Throwable) {
            temporary.delete()
            destination.delete()
            throw error
        } finally {
            if (oriented !== decoded) oriented.recycle()
            decoded.recycle()
        }
    }

    private fun orient(bitmap: Bitmap, source: File): Bitmap {
        val orientation = runCatching {
            ExifInterface(source.absolutePath).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.setScale(-1f, 1f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.setRotate(180f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.setScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.setRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.setRotate(90f)
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.setRotate(-90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.setRotate(-90f)
            else -> return bitmap
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}
