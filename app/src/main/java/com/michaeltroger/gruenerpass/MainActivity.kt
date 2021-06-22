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

    private var bitmap: Bitmap? = null
    private lateinit var certificateImage: ImageView
    private lateinit var deleteButton: Button

    private var resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.also { uri ->
                val file = copyFileToCache(uri)
                renderPdf(ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY))
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)

        certificateImage = findViewById(R.id.certificate)
        deleteButton = findViewById(R.id.add)

        val file = getFile()
        if (file.exists()) {
            renderPdf(ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY))
        }

        deleteButton.setOnClickListener {
            openFilePicker()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.delete -> {
            val file = getFile()
            if (file.exists()) {
                file.delete()
            }
            certificateImage.setImageResource(0)
            showDeleteButton()
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
        certificateImage.setImageBitmap(bitmap)

        page.close()
        renderer.close()

        showCertificate()
    }

    private fun getFile() = File(cacheDir, FILENAME)

    private fun showDeleteButton() {
        deleteButton.isVisible = true
        certificateImage.isVisible = false
    }

    private fun showCertificate() {
        deleteButton.isVisible = false
        certificateImage.isVisible = true
    }

    private fun copyFileToCache(uri: Uri): File {
        val inputStream = contentResolver.openInputStream(uri)!!
        val file = getFile()

        val output = FileOutputStream(file)
        val buffer = ByteArray(1024)
        var size: Int
        while (inputStream.read(buffer).also { size = it } != -1) {
            output.write(buffer, 0, size)
        }

        inputStream.close()
        output.close()

        return file
    }

}