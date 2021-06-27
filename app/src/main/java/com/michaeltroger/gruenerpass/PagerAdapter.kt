package com.michaeltroger.gruenerpass

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class PagerAdapter(fragment: Fragment, private val pdfHandler: PdfHandler): FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int {
        return if (pdfHandler.getQrBitmap() == null) {
            1
        } else {
            2
        }
    }

    override fun createFragment(position: Int): Fragment {
        return PagerFragment(position = position, adapter = this, pdfHandler = pdfHandler)
    }

    override fun getItemId(position: Int): Long {
        val pdf = pdfHandler.getPdfBitmap()?.generationId?.toLong()?: 0L
        val qr = pdfHandler.getQrBitmap()?.generationId?.toLong()?: 0L
        return itemCount.toLong() + (1000 * position.toLong()) + (3 * pdf) + (5 * qr)
    }

}