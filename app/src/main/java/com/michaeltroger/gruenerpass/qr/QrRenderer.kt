package com.michaeltroger.gruenerpass.qr

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.LuminanceSource
import com.google.zxing.MultiFormatWriter
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.BitMatrix
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val QR_CODE_SIZE = 400

interface QrRenderer {
    suspend fun getQrCodeIfPresent(document: Bitmap?): Bitmap?
}

class QrRendererImpl(
    private val qrCodeReader: QRCodeReader = QRCodeReader(),
    private val qrCodeWriter: MultiFormatWriter = MultiFormatWriter(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : QrRenderer {

    override suspend fun getQrCodeIfPresent(document: Bitmap?): Bitmap? = withContext(dispatcher) {
        val qrText = document?.extractQrCodeText() ?: return@withContext null
        encodeQrCodeAsBitmap(qrText)
    }

    private fun Bitmap.extractQrCodeText(): String? {
        try {
            val intArray = IntArray(width * height)
            getPixels(intArray, 0, width, 0, 0, width, height)
            val source: LuminanceSource = RGBLuminanceSource(width, height, intArray)
            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))

            return qrCodeReader.decode(binaryBitmap).text
        } catch (ignore: Exception) {}
        catch (ignore: OutOfMemoryError) {}
        return null
    }

    private fun encodeQrCodeAsBitmap(source: String): Bitmap? {
        val result: BitMatrix
        try {
            result = qrCodeWriter.encode(source, BarcodeFormat.QR_CODE, QR_CODE_SIZE, QR_CODE_SIZE)
        } catch (ignore: Exception) {
            return null
        }

        val w = result.width
        val h = result.height
        val pixels = IntArray(w * h)

        for (y in 0 until h) {
            val offset = y * w
            for (x in 0 until w) {
                pixels[offset + x] = if (result[x, y]) Color.BLACK else Color.WHITE
            }
        }

        val bitmapQrCode = Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565)
        bitmapQrCode.setPixels(pixels, 0, QR_CODE_SIZE, 0, 0, w, h)
        return bitmapQrCode
    }
}