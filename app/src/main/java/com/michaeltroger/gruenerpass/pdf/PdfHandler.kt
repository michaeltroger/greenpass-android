package com.michaeltroger.gruenerpass.pdf

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.google.zxing.*
import com.google.zxing.common.BitMatrix
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import com.michaeltroger.gruenerpass.GruenerPassApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import android.app.ActivityManager
import android.content.Context
import com.itextpdf.text.pdf.PdfReader
import com.itextpdf.text.pdf.PdfStamper
import java.io.InputStream


const val PDF_FILENAME = "certificate.pdf"
private const val QR_CODE_SIZE = 400
private const val MULTIPLIER_PDF_RESOLUTION = 2

object PdfHandler {

    private val context = GruenerPassApplication.instance
    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

    private val qrCodeReader = QRCodeReader()
    private val qrCodeWriter = MultiFormatWriter()

    private var bitmapDocument: Bitmap? = null
    private var bitmapQrCode: Bitmap? = null

    private val file = File(context.filesDir, PDF_FILENAME)

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

    /**
     * @return true if successful
     */
    suspend fun parsePdfIntoBitmap(): Boolean = withContext(Dispatchers.IO) {
        val fileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer: PdfRenderer
        try {
            renderer = PdfRenderer(fileDescriptor)
        } catch (exception: Exception) {
            deleteFile()
            return@withContext false
        }

        val page: PdfRenderer.Page = renderer.openPage(0)

        var width: Int = page.width
        var height: Int = page.height
        if (!activityManager.isLowRamDevice) {
            width *= MULTIPLIER_PDF_RESOLUTION
            height *= MULTIPLIER_PDF_RESOLUTION
        }
        bitmapDocument = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)!!
        page.render(bitmapDocument!!, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

        page.close()
        renderer.close()

        extractQrCodeIfAvailable(bitmapDocument!!)
        return@withContext true
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

        bitmapQrCode = Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565)
        bitmapQrCode!!.setPixels(pixels, 0, QR_CODE_SIZE, 0, 0, w, h)
    }

    suspend fun copyPdfToCache(uri: Uri): Pair<Boolean, Uri?> = withContext(Dispatchers.IO) {
        var inputStream: InputStream
        try {
            inputStream = context.contentResolver.openInputStream(uri)!!
        } catch (exception: Exception) {
            return@withContext false to null
        }

        try {
            PdfReader(inputStream)
        } catch (exception: Exception) {
            // file is password protected
            return@withContext false to uri
        } finally {
            inputStream.close()
        }

        // pdf is not password protected -> proceed
        try {
            inputStream = context.contentResolver.openInputStream(uri)!! // reopen input stream, was closed by PdfReader
            inputStream.copyTo(FileOutputStream(file))
            return@withContext true to null
        } catch (exception: Exception) {
            return@withContext false to null
        } finally {
            inputStream.close()
        }
    }

    suspend fun decryptAndCopyPdfToCache(uri: Uri, password: String): Boolean = withContext(Dispatchers.IO) {
        var inputStream: InputStream? = null
        try {
            inputStream = context.contentResolver.openInputStream(uri)!!
            val reader = PdfReader(inputStream, password.toByteArray())
            val stamper = PdfStamper(reader, FileOutputStream(file))
            stamper.close()
            reader.close()
            return@withContext true
        } catch (exception: Exception) {
            return@withContext false
        } finally {
            inputStream?.close()
        }
    }
}