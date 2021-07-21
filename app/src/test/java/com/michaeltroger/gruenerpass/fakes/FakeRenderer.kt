package com.michaeltroger.gruenerpass.fakes

import android.graphics.Bitmap
import com.michaeltroger.gruenerpass.model.PdfRenderer

class FakeRenderer(
    private val loadSuccess: Boolean = true,
    private var hasQrCode: Boolean = true
) : PdfRenderer {

    fun overrideHasQrCode(hasQrCode: Boolean) {
        this.hasQrCode = hasQrCode
    }

    override suspend fun loadFile(): Boolean {
        return loadSuccess
    }

    override fun getPageCount(): Int {
        return 1
    }

    override fun onCleared() {
        // nothing to do in test
    }

    override suspend fun hasQrCode(pageIndex: Int): Boolean {
        return hasQrCode
    }

    override suspend fun getQrCodeIfPresent(pageIndex: Int): Bitmap? {
        return if (hasQrCode) Bitmap.createBitmap(1,1, Bitmap.Config.ALPHA_8) else null
    }

    override suspend fun renderPage(pageIndex: Int): Bitmap {
        return Bitmap.createBitmap(1,1, Bitmap.Config.ALPHA_8)
    }

}