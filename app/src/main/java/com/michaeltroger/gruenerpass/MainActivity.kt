package com.michaeltroger.gruenerpass

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.google.zxing.*
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

import com.google.zxing.common.HybridBinarizer


private const val FILENAME = "certificate.pdf"
private const val QR_CODE_SIZE = 600

class MainActivity : AppCompatActivity() {

    private val qrCodeReader = QRCodeReader()
    private val qrCodeWriter = MultiFormatWriter()

    private lateinit var file: File
    private var bitmapDocument: Bitmap? = null
    private var bitmapQrCode: Bitmap? = null

    private var certificateImage: ImageView? = null
    private var addButton: Button? = null
    private var deleteMenuItem: MenuItem? = null

    private val resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.also { uri ->
                lifecycleScope.launch {
                    copyPdfToCache(uri)
                    parsePdfIntoBitmap()
                    showCertificateState()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)

        certificateImage = findViewById(R.id.certificate)
        addButton = findViewById(R.id.add)

        file = File(cacheDir, FILENAME)

        lifecycleScope.launch {
            if (doesFileExist()) {
                parsePdfIntoBitmap()
                showCertificateState()
            } else {
                showEmptyState()
            }
        }

        addButton?.setOnClickListener {
            openFilePicker()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        deleteMenuItem = menu.findItem(R.id.delete)

        lifecycleScope.launch {
            deleteMenuItem?.isEnabled = doesFileExist()
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.delete -> {
            lifecycleScope.launch {
                if (doesFileExist()) {
                    deleteFile()
                }
                bitmapDocument = null
                showEmptyState()
            }
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/pdf"
        }
        resultLauncher.launch(intent)
    }

    private fun showEmptyState() {
        addButton?.isVisible = true
        certificateImage?.setImageResource(0)
        certificateImage?.isVisible = false
        deleteMenuItem?.isEnabled = false
    }

    private fun showCertificateState() {
        addButton?.isVisible = false
        certificateImage?.setImageBitmap(bitmapQrCode)
        certificateImage?.isVisible = true
        deleteMenuItem?.isEnabled = true
    }

    private suspend fun doesFileExist(): Boolean = withContext(Dispatchers.IO) {
        file.exists()
    }

    private suspend fun deleteFile() = withContext(Dispatchers.IO) {
        file.delete()
    }

    private suspend fun parsePdfIntoBitmap() = withContext(Dispatchers.IO) {
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

    private suspend fun copyPdfToCache(uri: Uri) = withContext(Dispatchers.IO) {
        val inputStream = contentResolver.openInputStream(uri)!!

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