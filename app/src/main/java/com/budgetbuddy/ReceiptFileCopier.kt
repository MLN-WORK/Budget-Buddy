package com.budgetbuddy

import java.io.File
import java.io.InputStream
import java.util.UUID

/** Pure file-copy core, kept Android-free so failure and cleanup behavior can be host-tested. */
/*
 * Start of class
 * Name of class and related classes (parent/child classes): ReceiptFileCopier
 * Parent class: Any; child classes: none; related classes: ReceiptStorage and ReceiptImageNormalizer.
 * What the class does: Copies an untrusted image stream into bounded app-owned temporary storage.
 * What's important to other classes, if applicable: Other classes depend on its validation and ownership boundaries to keep financial and receipt data private and safe.
 * Code with comments begins below.
 */
object ReceiptFileCopier {
    const val DEFAULT_MAX_BYTES = 30L * 1024L * 1024L

    fun copy(
        input: InputStream,
        directory: File,
        extension: String,
        maxBytes: Long = DEFAULT_MAX_BYTES
    ): File {
        require(maxBytes > 0L)
        check(directory.isDirectory || directory.mkdirs()) { "Receipt storage is unavailable" }
        val safeExtension = extension.takeIf { it.matches(Regex("[A-Za-z0-9]{1,5}")) } ?: "jpg"
        val temporary = File.createTempFile("gallery-${UUID.randomUUID()}-", ".part", directory)
        val destination = File(directory, "receipt-${UUID.randomUUID()}.$safeExtension")

        try {
            var copied = 0L
            input.buffered().use { sourceStream ->
                temporary.outputStream().buffered().use { destinationStream ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        val count = sourceStream.read(buffer)
                        if (count < 0) break
                        copied += count
                        require(copied <= maxBytes) { "The selected image is too large" }
                        destinationStream.write(buffer, 0, count)
                    }
                }
            }
            require(copied > 0L) { "The selected image is empty" }
            if (!temporary.renameTo(destination)) {
                temporary.copyTo(destination, overwrite = true)
                check(temporary.delete()) { "The temporary receipt could not be removed" }
            }
            return destination
        } catch (error: Throwable) {
            temporary.delete()
            destination.delete()
            throw error
        }
    }
}
// End of class: ReceiptFileCopier
