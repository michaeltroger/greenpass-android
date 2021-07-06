package com.michaeltroger.gruenerpass.pdf

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.michaeltroger.gruenerpass.MainViewModel
import com.michaeltroger.gruenerpass.R

class PdfPagerFragment : Fragment() {

    private val vm by activityViewModels<MainViewModel>()
    private var certificate: ImageView? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.layout_item_pdf, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        certificate = view.findViewById(R.id.certificate)
        certificate?.setImageBitmap(vm.getPdfBitmap())
        if (vm.getPdfBitmap() == null) {
            requireActivity().apply {
                invalidateOptionsMenu()
                recreate()
            }
        }
    }

}