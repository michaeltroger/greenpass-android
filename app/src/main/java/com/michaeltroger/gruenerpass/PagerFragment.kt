package com.michaeltroger.gruenerpass

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment

class PagerFragment(private val position: Int, private val size: Int, private val pdfHandler: PdfHandler) : Fragment() {

    private var certificate: ImageView? = null
    private var qrCode: ImageView? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.layout_item, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        certificate = view.findViewById(R.id.certificate)
        qrCode = view.findViewById(R.id.qrcode)

         when (size) {
            1 -> {
                certificate?.setImageBitmap(pdfHandler.getPdfBitmap())
                qrCode?.setImageBitmap(null)
                certificate?.isVisible = true
                qrCode?.isVisible = false
            }
            else -> when (position) {
                0 -> {
                    qrCode?.setImageBitmap(pdfHandler.getQrBitmap())
                    certificate?.setImageBitmap(null)
                    certificate?.isVisible = false
                    qrCode?.isVisible = true
                }
                else -> {
                    certificate?.setImageBitmap(pdfHandler.getPdfBitmap())
                    qrCode?.setImageBitmap(null)
                    certificate?.isVisible = true
                    qrCode?.isVisible = false
                }
            }
        }
    }

}