package com.michaeltroger.gruenerpass.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.michaeltroger.gruenerpass.R
import com.michaeltroger.gruenerpass.model.PdfRenderer
import kotlinx.coroutines.launch

class QrPagerFragment(private val pdfRenderer: PdfRenderer) : Fragment() {

    private var qrCode: ImageView? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.layout_item_qr, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        qrCode = view.findViewById(R.id.qrcode)

        lifecycleScope.launch {
            val bitmap = pdfRenderer.getQrCodeIfPresent(0)
            bitmap?.let {
                if (it.generationId != qrCode?.tag) {
                    qrCode?.setImageBitmap(it)
                    qrCode?.tag = it.generationId
                }
            }
        }
    }

}