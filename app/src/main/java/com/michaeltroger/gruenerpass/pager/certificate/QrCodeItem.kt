package com.michaeltroger.gruenerpass.pager.certificate

import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.view.updateMargins
import com.michaeltroger.gruenerpass.R
import com.michaeltroger.gruenerpass.databinding.ItemQrCodeBinding
import com.michaeltroger.gruenerpass.model.PAGE_INDEX_QR_CODE
import com.michaeltroger.gruenerpass.model.PdfRenderer
import com.xwray.groupie.GroupDataObserver
import com.xwray.groupie.Item
import com.xwray.groupie.viewbinding.BindableItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

private const val TAG_LOADED = "qr_loaded"

class QrCodeItem(
    private val renderer: PdfRenderer,
    private val fileName: String
    ) : BindableItem<ItemQrCodeBinding>() {

    private val scope = CoroutineScope(
        Job() + Dispatchers.Main
    )

    override fun initializeViewBinding(view: View): ItemQrCodeBinding = ItemQrCodeBinding.bind(view)
    override fun getLayout() = R.layout.item_qr_code

    override fun bind(viewBinding: ItemQrCodeBinding, position: Int) {
        scope.launch {
            val qrCode = renderer.getQrCodeIfPresent(PAGE_INDEX_QR_CODE)
            if (qrCode == null) {
                viewBinding.root.isVisible = false
                (viewBinding.root.layoutParams as? ViewGroup.MarginLayoutParams)?.updateMargins(
                    bottom = 0
                )
            } else {
                viewBinding.root.isVisible = true
                (viewBinding.root.layoutParams as? ViewGroup.MarginLayoutParams)?.updateMargins(
                    bottom = viewBinding.root.context.resources.getDimensionPixelSize(R.dimen.space_very_small)
                )
                viewBinding.qrcode.setImageBitmap(qrCode)
                viewBinding.qrcode.tag = TAG_LOADED
            }
        }
    }

    override fun unregisterGroupDataObserver(groupDataObserver: GroupDataObserver) {
        super.unregisterGroupDataObserver(groupDataObserver)
        scope.cancel()
    }

    override fun isSameAs(other: Item<*>): Boolean {
        return viewType == other.viewType
    }

    override fun hasSameContentAs(other: Item<*>): Boolean {
        return (other as? QrCodeItem)?.fileName == fileName
    }
}
