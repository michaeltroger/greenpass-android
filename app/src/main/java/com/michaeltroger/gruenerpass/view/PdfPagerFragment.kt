package com.michaeltroger.gruenerpass.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.michaeltroger.gruenerpass.MainViewModel
import com.michaeltroger.gruenerpass.R
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

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

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.bitmapState.collect {
                    if (vm.getPdfBitmap()?.generationId != certificate?.tag) {
                        certificate?.setImageBitmap(vm.getPdfBitmap())
                        certificate?.tag = vm.getPdfBitmap()?.generationId
                    }
                }
            }
        }
    }

}