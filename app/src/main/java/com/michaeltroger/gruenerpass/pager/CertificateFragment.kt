package com.michaeltroger.gruenerpass.pager

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.michaeltroger.gruenerpass.MainViewModel
import com.michaeltroger.gruenerpass.R
import com.michaeltroger.gruenerpass.pager.pdfpage.SinglePageAdapter

class CertificateFragment : Fragment() {

    private val vm by activityViewModels<MainViewModel>()
    private var certificate: RecyclerView? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.layout_item_pdf, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        certificate = view.findViewById(R.id.certificate)
        certificate!!.layoutManager = LinearLayoutManager(requireContext())
        certificate!!.adapter = SinglePageAdapter(vm.pdfRenderer)
    }

}