package com.michaeltroger.gruenerpass.barcode

import android.graphics.Bitmap
import android.graphics.Rect
import com.michaeltroger.gruenerpass.coroutines.dispatcher.di.IoDispatcher
import de.markusfisch.android.zxingcpp.ZxingCpp
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject


private const val BARCODE_SIZE = 400
private val preferredPriority = listOf(
    ZxingCpp.BarcodeFormat.AZTEC,
    ZxingCpp.BarcodeFormat.DATA_MATRIX,
    ZxingCpp.BarcodeFormat.PDF_417,
    ZxingCpp.BarcodeFormat.QR_CODE,
)
private val readerOptions = ZxingCpp.ReaderOptions(
    formats = preferredPriority.toSet(),
    tryHarder = true,
    tryRotate = true,
    tryInvert = true,
    tryDownscale = true,
    maxNumberOfSymbols = 2,
)

public interface BarcodeRenderer {
    public suspend fun getBarcodeIfPresent(document: Bitmap?): Bitmap?
}

internal class BarcodeRendererImpl @Inject constructor(
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) : BarcodeRenderer {

    override suspend fun getBarcodeIfPresent(document: Bitmap?): Bitmap? = withContext(dispatcher) {
        val extractedCode = document?.extractBarcode() ?: return@withContext null
        encodeBarcodeAsBitmap(extractedCode)
    }

    @Suppress("ReturnCount")
    private fun Bitmap.extractBarcode(): ZxingCpp.Result? {
        try {
            val resultSet: Set<ZxingCpp.Result> = getCropRectangles().flatMap { cropRect ->
                ZxingCpp.readBitmap(
                    bitmap = this,
                    cropRect = cropRect,
                    rotation = 0,
                    options = readerOptions,
                )?: emptyList()
            }.toSet()
            if (resultSet.isEmpty()) return null
            val resultsMap = resultSet.associateBy { it.format }
            return preferredPriority.firstNotNullOfOrNull { resultsMap[it] }
        } catch (ignore: Exception) {}
        catch (ignore: OutOfMemoryError) {}
        return null
    }

    @Suppress("MagicNumber")
    private fun Bitmap.getCropRectangles(): List<Rect> {
        val halfWidth = width / 2
        val halfHeight = height / 2
        val cropRectList = mutableListOf(
            Rect(0, 0, width, height),

            Rect(0,0, halfWidth, halfHeight),
            Rect(halfWidth,0, width, halfHeight),

            Rect(0, halfHeight, halfWidth, height),
            Rect(halfWidth, halfHeight, width, height),
        )

        cropRectList += if (width > height) {
            val widthStep = width / 4
            val heightStep = height / 2
            listOf(
                Rect(0, 0, widthStep, heightStep),
                Rect(0, heightStep, widthStep, height),

                Rect(widthStep, 0, widthStep * 2, heightStep),
                Rect(widthStep, heightStep, widthStep * 2, height),

                Rect(widthStep * 2, 0, widthStep * 3, heightStep),
                Rect(widthStep * 2, heightStep, widthStep * 3, height),

                Rect(widthStep * 3, 0, width, heightStep),
                Rect(widthStep * 3, heightStep, width, height),
            )
        } else {
            val widthStep = width / 2
            val heightStep = height / 4
            listOf(
                Rect(0, 0, widthStep, heightStep),
                Rect(widthStep, 0, width, heightStep),

                Rect(0, heightStep, widthStep, heightStep * 2),
                Rect(widthStep, heightStep, width, heightStep * 2),

                Rect(0, heightStep * 2, widthStep, heightStep * 3),
                Rect(widthStep, heightStep * 2, width, heightStep * 3),

                Rect(0, heightStep * 3, widthStep, height),
                Rect(widthStep, heightStep * 3, width, height),
            )
        }
        return cropRectList
    }

    private fun encodeBarcodeAsBitmap(extractedCode: ZxingCpp.Result): Bitmap {
        val content = if (extractedCode.contentType == ZxingCpp.ContentType.BINARY) {
            extractedCode.rawBytes
        } else {
            extractedCode.text
        }

        return ZxingCpp.encodeAsBitmap(
            content = content,
            format = extractedCode.format,
            width = BARCODE_SIZE,
            height = BARCODE_SIZE
        )
    }
}
