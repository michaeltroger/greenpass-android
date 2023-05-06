package com.michaeltroger.gruenerpass.model

import android.app.ActivityManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.LuminanceSource
import com.google.zxing.MultiFormatWriter
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.BitMatrix
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File

private const val QR_CODE_SIZE = 400
private const val PDF_RESOLUTION_MULTIPLIER = 2
private const val MAX_BITMAP_SIZE = 100 * 1024 * 1024

const val PAGE_INDEX_QR_CODE = 0

object PdfRendererBuilder {
    fun create(
        context: Context,
        fileName: String,
        renderContext: CoroutineDispatcher
    ): com.michaeltroger.gruenerpass.model.PdfRenderer = PdfRendererImpl(
        context = context,
        fileName = fileName,
        renderContext = renderContext
    )
}

interface PdfRenderer {
    @Throws(Exception::class)
    suspend fun loadFile()
    suspend fun getPageCount(): Int
    fun close()
    suspend fun getQrCodeIfPresent(pageIndex: Int): Bitmap?
    suspend fun renderPage(pageIndex: Int): Bitmap?
}

private class PdfRendererImpl(
    private val context: Context,
    val fileName: String,
    private val renderContext: CoroutineDispatcher
): com.michaeltroger.gruenerpass.model.PdfRenderer {

    private val file = File(context.filesDir, fileName)

    private val activityManager: ActivityManager?
        get() = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager

    private val qrCodeReader = QRCodeReader()
    private val qrCodeWriter = MultiFormatWriter()

    private var renderer: PdfRenderer? = null
    private var fileDescriptor: ParcelFileDescriptor? = null

    @Suppress("TooGenericExceptionCaught")
    @Throws(Exception::class)
    override suspend fun loadFile(): Unit = withContext(renderContext) {
        try {
            fileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(fileDescriptor!!)
            renderer!!.openPage(0).use {  }
        } catch (exception: Exception) {
            if (file.exists()) {
                file.delete()
            }
            throw exception
        }
    }


    @Suppress("SwallowedException", "TooGenericExceptionCaught")
    override suspend fun getPageCount(): Int = withContext(renderContext) {
        if (renderer == null) {
            try {
                loadFile()
            } catch (ignore: Exception) {}
            if (!isActive) return@withContext 0
        }
        renderer?.pageCount ?: 0
    }

    override fun close() {
        try {
            renderer?.use {}
            fileDescriptor?.use {}
        } catch (ignore: Exception) {}
    }

    override suspend fun getQrCodeIfPresent(pageIndex: Int): Bitmap? = withContext(renderContext) {
       val qrText = renderPage(pageIndex)?.extractQrCodeText() ?: return@withContext null
       encodeQrCodeAsBitmap(qrText)
    }

    @Suppress("SwallowedException", "TooGenericExceptionCaught")
    override suspend fun renderPage(pageIndex: Int): Bitmap? = withContext(renderContext) {
        if (renderer == null) {
            try {
                loadFile()
            } catch (ignore: Exception) {}
            if (!isActive) return@withContext null
        }
        renderer?.openPage(pageIndex)?.renderAndClose { isActive }
    }

    private fun PdfRenderer.Page.renderAndClose(isActive: () -> Boolean): Bitmap? = use {
        if (!isActive()) return@use null
        val bitmap = createBitmap()
        if (!isActive()) return@use null
        render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        bitmap
    }

    @Suppress("SwallowedException")
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
