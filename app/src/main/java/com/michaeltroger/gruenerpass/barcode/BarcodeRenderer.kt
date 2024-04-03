package com.michaeltroger.gruenerpass.barcode

import android.graphics.Bitmap
import com.michaeltroger.gruenerpass.di.IoDispatcher
import de.markusfisch.android.zxingcpp.ZxingCpp
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject


private const val BARCODE_SIZE = 400
private val readerOptions = ZxingCpp.ReaderOptions(
    formats = setOf(ZxingCpp.BarcodeFormat.QR_CODE, ZxingCpp.BarcodeFormat.AZTEC),
    tryHarder = true,
    tryRotate = true,
    tryInvert = true,
    tryDownscale = true,
    maxNumberOfSymbols = 1
)

interface BarcodeRenderer {
    suspend fun getBarcodeIfPresent(document: Bitmap?): Bitmap?
}

class BarcodeRendererImpl @Inject constructor(
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) : BarcodeRenderer {

    override suspend fun getBarcodeIfPresent(document: Bitmap?): Bitmap? = withContext(dispatcher) {
        val extractedCode = document?.extractBarcode() ?: return@withContext null
        encodeBarcodeAsBitmap(extractedCode)
    }

    private fun Bitmap.extractBarcode(): ZxingCpp.Result? {
        try {
            return ZxingCpp.readBitmap(
                bitmap = this,
                left = 0,
                top = 0,
                width = this.width,
                height = this.height,
                rotation = 0,
                options = readerOptions
            )?.firstOrNull()
        } catch (ignore: Exception) {}
        catch (ignore: OutOfMemoryError) {}
        return null
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
