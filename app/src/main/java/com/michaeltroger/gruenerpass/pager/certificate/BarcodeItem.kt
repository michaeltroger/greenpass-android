package com.michaeltroger.gruenerpass.pager.certificate

import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.view.updateMargins
import com.michaeltroger.gruenerpass.R
import com.michaeltroger.gruenerpass.databinding.ItemBarcodeBinding
import com.michaeltroger.gruenerpass.model.PAGE_INDEX_BARCODE
import com.michaeltroger.gruenerpass.model.PdfRenderer
import com.xwray.groupie.GroupDataObserver
import com.xwray.groupie.Item
import com.xwray.groupie.viewbinding.BindableItem
import kotlinx.coroutines.*

class BarcodeItem(
    private val renderer: PdfRenderer,
    private val fileName: String
    ) : BindableItem<ItemBarcodeBinding>() {

    private val scope = CoroutineScope(
        Job() + Dispatchers.Main
    )

    override fun initializeViewBinding(view: View): ItemBarcodeBinding = ItemBarcodeBinding.bind(view)
    override fun getLayout() = R.layout.item_barcode

    override fun bind(viewBinding: ItemBarcodeBinding, position: Int) {
        scope.launch {
            val qrCode = renderer.getBarcodeIfPresent(PAGE_INDEX_BARCODE)
            if (qrCode == null) {
                viewBinding.root.isVisible = false
                (viewBinding.root.layoutParams as? ViewGroup.MarginLayoutParams)?.updateMargins(bottom = 0)
            } else {
                viewBinding.root.isVisible = true
                (viewBinding.root.layoutParams as? ViewGroup.MarginLayoutParams)?.updateMargins(bottom = viewBinding.root.context.resources.getDimensionPixelSize(R.dimen.space_very_small))
                viewBinding.barcode.setImageBitmap(qrCode)
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
        return (other as? BarcodeItem)?.fileName == fileName
    }
}