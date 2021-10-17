package com.michaeltroger.gruenerpass.pager.certificate

import android.view.View
import androidx.core.view.isVisible
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
            renderer.getBarcodeIfPresent(PAGE_INDEX_BARCODE)?.let {
                viewBinding.barcode.setImageBitmap(it)
            }
            viewBinding.progressIndicator.isVisible = false
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