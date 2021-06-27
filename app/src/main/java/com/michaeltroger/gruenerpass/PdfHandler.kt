package com.michaeltroger.gruenerpass

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.google.zxing.*
import com.google.zxing.common.BitMatrix
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

private const val FILENAME = "certificate.pdf"
private const val QR_CODE_SIZE = 1920

class PdfHandler(ctx: Context) {

    private val context = ctx.applicationContext

    private val qrCodeReader = QRCodeReader()
    private val qrCodeWriter = MultiFormatWriter()

    private var bitmapDocument: Bitmap? = null
    private var bitmapQrCode: Bitmap? = null

    private val file = File(context.cacheDir, FILENAME)

    fun getQrBitmap() = bitmapQrCode
    fun getPdfBitmap() = bitmapDocument

    suspend fun doesFileExist(): Boolean = withContext(Dispatchers.IO) {
        file.exists()
    }

    suspend fun deleteFile() = withContext(Dispatchers.IO) {
        if (doesFileExist()) {
            file.delete()
        }
        bitmapDocument = null
        bitmapQrCode = null
    }

    suspend fun parsePdfIntoBitmap() = withContext(Dispatchers.IO) {
        val renderer = PdfRenderer(ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY))
        val page: PdfRenderer.Page = renderer.openPage(0)

        bitmapDocument = Bitmap.createBitmap(page.width * 3, page.height * 3, Bitmap.Config.ARGB_8888)!!
        page.render(bitmapDocument!!, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

        page.close()
        renderer.close()

        extractQrCodeIfAvailable(bitmapDocument!!)
    }

    private fun extractQrCodeIfAvailable(bitmap: Bitmap) {
        val intArray = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(intArray, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        val source: LuminanceSource = RGBLuminanceSource(bitmap.width, bitmap.height, intArray)
        val binaryBitmap = BinaryBitmap(HybridBinarizer(source))

        try {
            qrCodeReader.decode(binaryBitmap).text?.let {
                encodeQrCodeAsBitmap(it)
            }
        } catch (ignore: Exception) {}
    }

    private fun encodeQrCodeAsBitmap(source: String) {
        val result: BitMatrix = try {
            qrCodeWriter.encode(source, BarcodeFormat.QR_CODE, QR_CODE_SIZE, QR_CODE_SIZE, null)
        } catch (ignore: Exception) {
            return
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

        bitmapQrCode = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        bitmapQrCode!!.setPixels(pixels, 0, QR_CODE_SIZE, 0, 0, w, h)
    }

    suspend fun copyPdfToCache(uri: Uri) = withContext(Dispatchers.IO) {
        val inputStream = context.contentResolver.openInputStream(uri)!!

        val output = FileOutputStream(file)
        val buffer = ByteArray(1024)
        var size: Int
        while (inputStream.read(buffer).also { size = it } != -1) {
            output.write(buffer, 0, size)
        }

        inputStream.close()
        output.close()
    }
}