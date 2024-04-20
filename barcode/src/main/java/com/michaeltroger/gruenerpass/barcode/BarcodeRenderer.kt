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
private const val DIVISOR_LONGER_SIDE = 4
private const val DIVISOR_SHORTER_SIDE = 2

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

    private fun Bitmap.extractBarcode(): ZxingCpp.Result? {
        val resultSet = try {
            getCropRectangles().flatMap { cropRect ->
                ZxingCpp.readBitmap(
                    bitmap = this,
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

        if (resultSet.isEmpty()) return null
        val resultsMap = resultSet.associateBy { it.format }
        return preferredPriority.firstNotNullOfOrNull { resultsMap[it] }
    }

    private fun Bitmap.getCropRectangles(): List<Rect> {
        val cropRectList = mutableListOf(
            Rect(0, 0, width, height),
        )

        cropRectList += getCropRectangles(divisorX = 2, divisorY = 2)

        val divisorX: Int
        val divisorY: Int
        if (width > height) {
            divisorX = DIVISOR_LONGER_SIDE
            divisorY = DIVISOR_SHORTER_SIDE
        } else {
            divisorX = DIVISOR_SHORTER_SIDE
            divisorY = DIVISOR_LONGER_SIDE
        }
        cropRectList += getCropRectangles(divisorX = divisorX, divisorY = divisorY)

        return cropRectList
    }

    private fun Bitmap.getCropRectangles(divisorX: Int, divisorY: Int): List<Rect> {
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
