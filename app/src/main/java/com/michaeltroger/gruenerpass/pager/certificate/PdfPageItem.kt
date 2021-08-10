package com.michaeltroger.gruenerpass.pager.certificate

import android.view.View
import com.michaeltroger.gruenerpass.R
import com.michaeltroger.gruenerpass.databinding.ItemPdfPageBinding
import com.michaeltroger.gruenerpass.model.PdfRenderer
import com.xwray.groupie.GroupDataObserver
import com.xwray.groupie.Item
import com.xwray.groupie.viewbinding.BindableItem
import kotlinx.coroutines.*

class PdfPageItem(
    private val renderer: PdfRenderer,
    private val fileName: String,
    private val pageIndex: Int
    ) : BindableItem<ItemPdfPageBinding>() {

    private val scope = CoroutineScope(
        Job() + Dispatchers.Main
    )

    override fun initializeViewBinding(view: View): ItemPdfPageBinding = ItemPdfPageBinding.bind(view)
    override fun getLayout() = R.layout.item_pdf_page

    override fun bind(viewBinding: ItemPdfPageBinding, position: Int) {
        scope.launch {
            viewBinding.certificate.setImageBitmap(null)
            viewBinding.certificate.setImageBitmap(renderer.renderPage(pageIndex))
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
        return (other as? PdfPageItem)?.pageIndex == pageIndex && (other as? PdfPageItem)?.fileName == fileName
    }
}