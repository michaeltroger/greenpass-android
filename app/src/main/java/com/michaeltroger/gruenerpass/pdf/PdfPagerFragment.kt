package com.michaeltroger.gruenerpass.pdf

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import com.michaeltroger.gruenerpass.R

class PdfPagerFragment : Fragment() {

    private var certificate: ImageView? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.layout_item_pdf, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        certificate = view.findViewById(R.id.certificate)
        certificate?.setImageBitmap(PdfHandler.getPdfBitmap())
    }

}