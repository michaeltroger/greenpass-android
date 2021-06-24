package com.michaeltroger.gruenerpass

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
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
import java.io.File
import java.io.FileOutputStream

private const val FILENAME = "certificate.pdf"

class MainActivity : AppCompatActivity() {

    private lateinit var file: File
    private var bitmap: Bitmap? = null

    private var certificateImage: ImageView? = null
    private var addButton: Button? = null
    private var deleteMenuItem: MenuItem? = null

    private var resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.also { uri ->
                copyFileToCache(uri)
                renderPdf(ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY))
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)

        certificateImage = findViewById(R.id.certificate)
        addButton = findViewById(R.id.add)

        file = File(cacheDir, FILENAME)

        if (file.exists()) {
            renderPdf(ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY))
        }

        addButton?.setOnClickListener {
            openFilePicker()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        deleteMenuItem = menu.findItem(R.id.delete)
        deleteMenuItem?.isEnabled = file.exists()
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.delete -> {
            if (file.exists()) {
                file.delete()
                bitmap = null
            }
            showEmptyState()
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

    private fun renderPdf(fileDescriptor: ParcelFileDescriptor) {
        val renderer = PdfRenderer(fileDescriptor)
        val page: PdfRenderer.Page = renderer.openPage(0)

        bitmap = Bitmap.createBitmap(page.width * 3, page.height * 3, Bitmap.Config.ARGB_8888)
        page.render(bitmap!!, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        certificateImage?.setImageBitmap(bitmap)

        page.close()
        renderer.close()

        showCertificateState()
    }

    private fun showEmptyState() {
        addButton?.isVisible = true
        certificateImage?.setImageResource(0)
        certificateImage?.isVisible = false
        deleteMenuItem?.isEnabled = false
    }

    private fun showCertificateState() {
        addButton?.isVisible = false
        certificateImage?.isVisible = true
        deleteMenuItem?.isEnabled = true
    }

    private fun copyFileToCache(uri: Uri) {
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