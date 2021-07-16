package com.michaeltroger.gruenerpass.pager

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.michaeltroger.gruenerpass.MainViewModel
import com.michaeltroger.gruenerpass.R
import kotlinx.coroutines.launch

class QrCodeFragment : Fragment() {

    private val vm by activityViewModels<MainViewModel>()
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
            val bitmap = vm.pdfRenderer.getQrCodeIfPresent(0)
            bitmap?.let {
                qrCode?.setImageBitmap(it)
            }
        }
    }

}