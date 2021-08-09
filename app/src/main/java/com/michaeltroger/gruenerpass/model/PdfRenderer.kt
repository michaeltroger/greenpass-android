package com.michaeltroger.gruenerpass.model

import android.app.ActivityManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.google.zxing.*
import com.google.zxing.common.BitMatrix
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File

private const val QR_CODE_SIZE = 400
private const val PDF_RESOLUTION_MULTIPLIER = 2
private const val MAX_BITMAP_SIZE = 100 * 1024 * 1024

const val PAGE_INDEX_QR_CODE = 0

interface PdfRenderer {
    suspend fun loadFile(): Boolean
    fun getPageCount(): Int
    fun onCleared()
    suspend fun hasQrCode(pageIndex: Int): Boolean
    suspend fun getQrCodeIfPresent(pageIndex: Int): Bitmap?
    suspend fun renderPage(pageIndex: Int): Bitmap
}

class PdfRendererImpl(private val context: Context, val fileName: String, private val renderContext: CoroutineDispatcher): com.michaeltroger.gruenerpass.model.PdfRenderer {

    private val file = File(context.filesDir, fileName)

    private val activityManager: ActivityManager?
        get() = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager

    private val qrCodeReader = QRCodeReader()
    private val qrCodeWriter = MultiFormatWriter()

    private var renderer: PdfRenderer? = null
    private var fileDescriptor: ParcelFileDescriptor? = null

    /**
     * @return true if successful
     */
    override suspend fun loadFile(): Boolean = withContext(renderContext) {
        try {
            fileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(fileDescriptor!!)
            renderer!!.openPage(0).use {  }
            return@withContext true
        } catch (exception: Exception) {
            if (file.exists()) {
                file.delete()
            }
            return@withContext false
        }
    }

    override fun getPageCount(): Int = renderer?.pageCount ?: 0

    override fun onCleared() {
        try {
            renderer?.use {}
            fileDescriptor?.use {}
        } catch (ignore: Exception) {}
    }

    override suspend fun hasQrCode(pageIndex: Int): Boolean = withContext(renderContext) {
        return@withContext !renderPage(pageIndex).extractQrCodeText().isNullOrEmpty()
    }

    override suspend fun getQrCodeIfPresent(pageIndex: Int): Bitmap? = withContext(renderContext) {
       val qrText = renderPage(pageIndex).extractQrCodeText() ?: return@withContext null
       return@withContext encodeQrCodeAsBitmap(qrText)
    }

    override suspend fun renderPage(pageIndex: Int): Bitmap = withContext(renderContext) {
        if (renderer == null) {
            loadFile()
        }
        return@withContext renderer!!.openPage(pageIndex).renderAndClose()
    }

    private fun PdfRenderer.Page.renderAndClose(): Bitmap = use {
        val bitmap = createBitmap()
        render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        bitmap
    }

    private fun PdfRenderer.Page.createBitmap(): Bitmap {
        var renderWidth: Int = width
        var renderHeight: Int = height
        if (activityManager?.isLowRamDevice == false) {
            renderWidth *= PDF_RESOLUTION_MULTIPLIER
            renderHeight *= PDF_RESOLUTION_MULTIPLIER
        }
        var bitmap = try {
            Bitmap.createBitmap(renderWidth, renderHeight, Bitmap.Config.ARGB_8888)
        } catch (e: OutOfMemoryError) {
            Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        }
        if (bitmap.byteCount > MAX_BITMAP_SIZE) {
            bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        }

        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        canvas.drawBitmap(bitmap, 0f, 0f, null)

        return bitmap
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

    private fun encodeQrCodeAsBitmap(source: String): Bitmap {
        val hintMap = HashMap<EncodeHintType, Any>()
        hintMap[EncodeHintType.ERROR_CORRECTION] = ErrorCorrectionLevel.Q
        val result: BitMatrix = qrCodeWriter.encode(source, BarcodeFormat.QR_CODE, QR_CODE_SIZE, QR_CODE_SIZE, hintMap)

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