package com.budgetbuddy

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

class ReceiptFileCopierTest {
    @Test
    fun copiesReceiptAtomically() {
        val directory = Files.createTempDirectory("receipt-copy-test").toFile()
        val expected = "receipt bytes".encodeToByteArray()
        try {
            val result = ReceiptFileCopier.copy(expected.inputStream(), directory, "png")
            assertTrue(result.isFile)
            assertTrue(result.extension == "png")
            assertArrayEquals(expected, result.readBytes())
            assertFalse(directory.listFiles().orEmpty().any { it.extension == "part" })
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun rejectsEmptyInputAndRemovesTemporaryFiles() {
        val directory = Files.createTempDirectory("receipt-empty-test").toFile()
        try {
            assertThrows(IllegalArgumentException::class.java) {
                ReceiptFileCopier.copy(byteArrayOf().inputStream(), directory, "jpg")
            }
            assertTrue(directory.listFiles().orEmpty().isEmpty())
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun enforcesSizeLimitWithoutLeavingPartialFiles() {
        val directory = Files.createTempDirectory("receipt-limit-test").toFile()
        try {
            assertThrows(IllegalArgumentException::class.java) {
                ReceiptFileCopier.copy(ByteArray(32).inputStream(), directory, "jpg", maxBytes = 16)
            }
            assertTrue(directory.listFiles().orEmpty().isEmpty())
        } finally {
            directory.deleteRecursively()
        }
    }
}
