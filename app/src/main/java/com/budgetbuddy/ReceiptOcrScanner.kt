package com.budgetbuddy

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.Closeable
import java.io.File

/** Uses the bundled Latin-script model; receipt pixels and recognized text stay on-device. */
/*
 * Start of class
 * Name of class and related classes (parent/child classes): ReceiptOcrScanner
 * Parent class: Closeable; child classes: none; related classes: ReceiptParser, ReceiptOcrResult, and AddImageActivity.
 * What the class does: Runs the bundled on-device text recognizer and parses its result.
 * What's important to other classes, if applicable: OCR callers treat its output as a suggestion and must keep user review and input validation in place.
 * Code with comments begins below.
 */
class ReceiptOcrScanner(private val context: Context) : Closeable {
    private val recognizer: TextRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    fun scan(file: File, onSuccess: (ReceiptOcrResult) -> Unit, onFailure: (Exception) -> Unit) {
        val image = runCatching { InputImage.fromFilePath(context, Uri.fromFile(file)) }
            .getOrElse {
                onFailure(it as? Exception ?: IllegalStateException(it))
                return
            }
        recognizer.process(image)
            .addOnSuccessListener { recognized ->
                val lines = recognized.textBlocks.flatMap { it.lines }
                val heights = lines.mapNotNull { it.boundingBox?.height()?.takeIf { height -> height > 0 } }.sorted()
                val medianHeight = heights.getOrNull(heights.size / 2)?.toDouble()?.coerceAtLeast(1.0) ?: 1.0
                val maximumBottom = lines.maxOfOrNull { it.boundingBox?.bottom ?: 0 }?.coerceAtLeast(1) ?: 1
                val prominentMerchant = lines
                    .mapNotNull { line ->
                        val box = line.boundingBox ?: return@mapNotNull null
                        val text = line.text.replace(Regex("""\s+"""), " ").trim()
                        if (!ReceiptParser.isMerchantCandidate(text)) return@mapNotNull null
                        val relativeHeight = box.height() / medianHeight
                        val topRatio = box.top.toDouble() / maximumBottom
                        val topPenalty = topRatio * 55.0
                        val topHeaderBonus = if (topRatio <= 0.22) 28.0 else 0.0
                        val uppercaseBonus = if (text.any(Char::isLetter) &&
                            text.filter(Char::isLetter).all { it.isUpperCase() }
                        ) 18.0 else 0.0
                        // Large/bold merchant headers produce taller OCR line boxes. Weight
                        // that signal strongly, while still favouring the receipt's top area.
                        text to (relativeHeight * 120.0 - topPenalty + topHeaderBonus + uppercaseBonus)
                    }
                    .maxByOrNull { it.second }
                    ?.first
                onSuccess(ReceiptParser.parse(recognized.text, prominentMerchant))
            }
            .addOnFailureListener(onFailure)
    }

    override fun close() = recognizer.close()
}
// End of class: ReceiptOcrScanner
