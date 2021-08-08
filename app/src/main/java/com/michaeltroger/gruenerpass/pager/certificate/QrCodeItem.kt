package com.michaeltroger.gruenerpass.pager.certificate

import android.view.View
import com.michaeltroger.gruenerpass.R
import com.michaeltroger.gruenerpass.databinding.ItemQrCodeBinding
import com.michaeltroger.gruenerpass.model.PAGE_INDEX_QR_CODE
import com.michaeltroger.gruenerpass.model.PdfRenderer
import com.xwray.groupie.GroupDataObserver
import com.xwray.groupie.viewbinding.BindableItem
import kotlinx.coroutines.*

class QrCodeItem(private val renderer: PdfRenderer) : BindableItem<ItemQrCodeBinding>() {

    private val scope = CoroutineScope(
        Job() + Dispatchers.Main
    )

    override fun initializeViewBinding(view: View): ItemQrCodeBinding = ItemQrCodeBinding.bind(view)
    override fun getLayout() = R.layout.item_qr_code

    override fun bind(viewBinding: ItemQrCodeBinding, position: Int) {
        scope.launch {
            viewBinding.qrcode.setImageBitmap(renderer.getQrCodeIfPresent(PAGE_INDEX_QR_CODE)!!)
        }
    }

    override fun unregisterGroupDataObserver(groupDataObserver: GroupDataObserver) {
        super.unregisterGroupDataObserver(groupDataObserver)
        scope.cancel()
    }
}