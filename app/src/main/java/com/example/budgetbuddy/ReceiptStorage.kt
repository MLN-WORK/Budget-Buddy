package com.example.budgetbuddy

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import java.io.File

/** Owns every receipt file so camera and gallery providers never become long-term dependencies. */
object ReceiptStorage {
    fun createCameraDestination(context: Context): File =
        File.createTempFile("camera-", ".jpg", writableDirectory(context))

    fun finalizeCameraCapture(context: Context, source: File): File =
        normalizeOwnedSource(context, source)

    fun importFromGallery(context: Context, source: Uri): File {
        val extension = context.contentResolver.getType(source)
            ?.let(MimeTypeMap.getSingleton()::getExtensionFromMimeType)
            ?.takeIf { it.matches(Regex("[A-Za-z0-9]{1,5}")) }
            ?: "jpg"
        val directory = writableDirectory(context)
        val input = requireNotNull(context.contentResolver.openInputStream(source)) {
            "The selected image cannot be opened"
        }
        val imported = ReceiptFileCopier.copy(input, directory, extension)
        return normalizeOwnedSource(context, imported)
    }

    fun isUsableOwnedReceipt(context: Context, file: File?): Boolean =
        file?.isFile == true && file.length() > 0L && isOwnedReceipt(context, file)

    fun deleteIfOwned(context: Context, path: String?) {
        val file = path?.let(::File) ?: return
        if (isOwnedReceipt(context, file) && file.isFile) file.delete()
    }

    private fun isOwnedReceipt(context: Context, file: File): Boolean {
        val candidate = runCatching { file.canonicalFile }.getOrNull() ?: return false
        return candidate.parentFile?.let { parent ->
            ownedDirectories(context).any { it == parent }
        } == true
    }

    private fun normalizeOwnedSource(context: Context, source: File): File {
        require(isOwnedReceipt(context, source)) { "The receipt source is not app-owned" }
        return try {
            ReceiptImageNormalizer.normalize(source, writableDirectory(context))
        } finally {
            source.delete()
        }
    }

    private fun writableDirectory(context: Context): File =
        File(context.filesDir, "receipts").also {
            check(it.isDirectory || it.mkdirs()) { "Receipt storage is unavailable" }
        }

    private fun ownedDirectories(context: Context): List<File> = buildList {
        runCatching { File(context.filesDir, "receipts").canonicalFile }.getOrNull()?.let(::add)
        context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES)?.let { root ->
            runCatching { File(root, "BudgetBuddy/Receipts").canonicalFile }.getOrNull()?.let(::add)
        }
    }
}
