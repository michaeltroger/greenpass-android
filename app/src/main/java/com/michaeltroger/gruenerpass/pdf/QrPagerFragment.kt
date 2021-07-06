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

class QrPagerFragment : Fragment() {

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
        qrCode?.setImageBitmap(vm.getQrBitmap())
        if (vm.getQrBitmap() == null) {
            requireActivity().recreate()
        }
    }

}