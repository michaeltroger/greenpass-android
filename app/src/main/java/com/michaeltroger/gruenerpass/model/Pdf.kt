package com.michaeltroger.gruenerpass.model

import android.app.ActivityManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.google.zxing.*
import com.google.zxing.common.BitMatrix
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.encryption.InvalidPasswordException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream


const val PDF_FILENAME = "certificate.pdf"
private const val QR_CODE_SIZE = 400
private const val MULTIPLIER_PDF_RESOLUTION = 2
private const val MAX_BITMAP_SIZE = 100 * 1024 * 1024

class Pdf(private val context: Context) {
    private val activityManager: ActivityManager?
        get() = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager

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
        var fileDescriptor: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        var page: PdfRenderer.Page? = null
        try {
            fileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(fileDescriptor)
            page = renderer.openPage(0)
        } catch (exception: Exception) {
            try {
                page?.close()
                renderer?.close()
                fileDescriptor?.close()
            } catch (ignore: Exception) {}
            deleteFile()
            return@withContext false
        }

        var width: Int = page.width
        var height: Int = page.height
        if (activityManager?.isLowRamDevice == false) {
            width *= MULTIPLIER_PDF_RESOLUTION
            height *= MULTIPLIER_PDF_RESOLUTION
        }

        bitmapDocument = null
        bitmapDocument = try {
            Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)!!
        } catch (e: OutOfMemoryError) {
            Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)!!
        }

        if (bitmapDocument!!.byteCount > MAX_BITMAP_SIZE) {
            bitmapDocument = null
            bitmapDocument = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)!!
        }
        val canvas = Canvas(bitmapDocument!!)
        canvas.drawColor(Color.WHITE)
        canvas.drawBitmap(bitmapDocument!!, 0f, 0f, null)
        page.render(bitmapDocument!!, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

        try {
            page.close()
            renderer.close()
            fileDescriptor.close()
        } catch (ignore: Exception) {}

        if (bitmapDocument!!.byteCount > MAX_BITMAP_SIZE) {
            deleteFile()
            return@withContext false
        }

        extractQrCodeIfAvailable(bitmapDocument!!)
        return@withContext true
    }

    private fun extractQrCodeIfAvailable(bitmap: Bitmap) {
        try {
            val intArray = IntArray(bitmap.width * bitmap.height)
            bitmap.getPixels(intArray, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
            val source: LuminanceSource = RGBLuminanceSource(bitmap.width, bitmap.height, intArray)
            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))

            qrCodeReader.decode(binaryBitmap).text?.let {
                encodeQrCodeAsBitmap(it)
            }
        } catch (ignore: Exception) {}
    }

    private fun encodeQrCodeAsBitmap(source: String) {
        val result: BitMatrix = try {
            val hintMap = HashMap<EncodeHintType, Any>()
            hintMap[EncodeHintType.ERROR_CORRECTION] = ErrorCorrectionLevel.Q
            qrCodeWriter.encode(source, BarcodeFormat.QR_CODE, QR_CODE_SIZE, QR_CODE_SIZE, hintMap)
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

        bitmapQrCode = null
        bitmapQrCode = Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565)
        bitmapQrCode!!.setPixels(pixels, 0, QR_CODE_SIZE, 0, 0, w, h)
    }

    suspend fun isPdfPasswordProtected(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)!!.use {
                try {
                    return@withContext PDDocument.load(it).checkIfPasswordProtectedAndClose()
                } catch (exception: InvalidPasswordException) {
                    return@withContext true
                }
            }
        } catch (exception: Exception) {
            return@withContext false
        }
    }
    /**
     * @return true if successful
     */
    suspend fun copyPdfToCache(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)!!.use {
                deleteFile() // clear old file first if it exists
                it.copyTo(FileOutputStream(file))
                return@withContext true
            }
        } catch (exception: Exception) {
            return@withContext false
        }
    }

    /**
     * @return true if successful
     */
    suspend fun decryptAndCopyPdfToCache(uri: Uri, password: String): Boolean = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)!!.use {
                with(PDDocument.load(it, password)) {
                    deleteFile() // clear old file first if it exists
                    removePasswordCopyAndClose()
                }
                return@withContext true
            }
        } catch (exception: Exception) {
            return@withContext false
        }
    }

    private fun PDDocument.removePasswordCopyAndClose() = use {
        isAllSecurityToBeRemoved = true
        save(FileOutputStream(file))
    }

    private fun PDDocument.checkIfPasswordProtectedAndClose(): Boolean = use {
        return isEncrypted
    }
}