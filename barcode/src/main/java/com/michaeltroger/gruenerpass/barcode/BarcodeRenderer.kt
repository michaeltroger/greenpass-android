package com.michaeltroger.gruenerpass.barcode

import android.graphics.Bitmap
import android.graphics.Rect
import com.michaeltroger.gruenerpass.coroutines.dispatcher.di.IoDispatcher
import de.markusfisch.android.zxingcpp.ZxingCpp
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import javax.inject.Inject


private const val BARCODE_SIZE = 400
private val preferredPriority = listOf(
    ZxingCpp.BarcodeFormat.AZTEC,
    ZxingCpp.BarcodeFormat.DATA_MATRIX,
    ZxingCpp.BarcodeFormat.PDF_417,
    ZxingCpp.BarcodeFormat.QR_CODE,
    ZxingCpp.BarcodeFormat.UPC_A,
    ZxingCpp.BarcodeFormat.UPC_E,
    ZxingCpp.BarcodeFormat.EAN_8,
    ZxingCpp.BarcodeFormat.EAN_13,
    ZxingCpp.BarcodeFormat.CODE_39,
    ZxingCpp.BarcodeFormat.CODE_93,
    ZxingCpp.BarcodeFormat.CODE_128,
    ZxingCpp.BarcodeFormat.CODABAR,
    ZxingCpp.BarcodeFormat.ITF,
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
        if (!isActive) return@withContext null
        encodeBarcodeAsBitmap(extractedCode)
    }

    private suspend fun Bitmap.extractBarcode(): ZxingCpp.Result? = withContext(dispatcher) {
        val resultSet = try {
            getCropRectangles()
                .onEach {
                    if (!isActive) return@withContext null
                }.flatMap { cropRect ->
                    ZxingCpp.readBitmap(
                        bitmap = this@extractBarcode,
                        cropRect = cropRect,
                        rotation = 0,
                        options = readerOptions,
                    )?: emptyList()
                }
        } catch (ignore: Exception) {
            emptyList()
        } catch (ignore: OutOfMemoryError) {
            emptyList()
        }

        if (resultSet.isEmpty()) return@withContext null
        val resultsMap = resultSet.associateBy { it.format }
        preferredPriority.firstNotNullOfOrNull { resultsMap[it] }
    }

    private fun Bitmap.getCropRectangles(): List<Rect> {
        val cropRectList = mutableListOf(
            Rect(0, 0, width, height),
        )
        cropRectList += getCropRectangles(divisorLongerSize = 2, divisorShorterSize = 2)
        cropRectList += getCropRectangles(divisorLongerSize = 4, divisorShorterSize = 2)
        cropRectList += getCropRectangles(divisorLongerSize = 5, divisorShorterSize = 1)

        return cropRectList
    }

    private fun Bitmap.getCropRectangles(divisorLongerSize: Int, divisorShorterSize: Int): List<Rect> {
        val divisorX: Int
        val divisorY: Int
        if (width > height) {
            divisorX = divisorLongerSize
            divisorY = divisorShorterSize
        } else {
            divisorX = divisorShorterSize
            divisorY = divisorLongerSize
        }

        val tempX = width / divisorX
        val tempY = height / divisorY
        val cropRectList = mutableListOf<Rect>()
        for (multiplierX in 0 until divisorX) {
            for (multiplierY in 0 until divisorY) {
                cropRectList += Rect(
                    tempX * multiplierX,
                    tempY * multiplierY,
                    tempX * (multiplierX + 1),
                    tempY * (multiplierY + 1)
                )
            }
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
