package com.michaeltroger.gruenerpass.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.michaeltroger.gruenerpass.R
import com.michaeltroger.gruenerpass.model.PdfRenderer

class PdfPagerFragment(pdfRenderer: PdfRenderer) : Fragment() {
    private var certificate: RecyclerView? = null
    private val pageAdapter = PdfPageAdapter(pdfRenderer)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.layout_item_pdf, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        certificate = view.findViewById(R.id.certificate)
        certificate!!.layoutManager = LinearLayoutManager(requireContext())
        certificate!!.adapter = pageAdapter
    }

}