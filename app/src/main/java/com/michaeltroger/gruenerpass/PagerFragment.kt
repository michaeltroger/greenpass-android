package com.michaeltroger.gruenerpass

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment

class PagerFragment(private val position: Int, private val size: Int, private val pdfHandler: PdfHandler) : Fragment() {

    private var image: ImageView? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.layout_item, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        image = view.findViewById(R.id.certificate)
        val bitmap = when (size) {
            1 -> pdfHandler.getPdfBitmap()
            else -> when (position) {
                0 -> pdfHandler.getQrBitmap()
                else -> pdfHandler.getPdfBitmap()
            }
        }
        image?.setImageBitmap(bitmap)
    }

}