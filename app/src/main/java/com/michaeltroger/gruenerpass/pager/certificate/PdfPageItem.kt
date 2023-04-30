package com.michaeltroger.gruenerpass.pager.certificate

import android.view.View
import androidx.core.view.isVisible
import com.michaeltroger.gruenerpass.R
import com.michaeltroger.gruenerpass.databinding.ItemPdfPageBinding
import com.michaeltroger.gruenerpass.model.PdfRenderer
import com.xwray.groupie.GroupDataObserver
import com.xwray.groupie.Item
import com.xwray.groupie.viewbinding.BindableItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

private const val TAG_LOADED = "pdf_loaded"

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
            renderer.renderPage(pageIndex)?.let {
                viewBinding.pdfPage.setImageBitmap(it)
                viewBinding.pdfPage.tag = TAG_LOADED
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
        return (other as? PdfPageItem)?.pageIndex == pageIndex && (other as? PdfPageItem)?.fileName == fileName
    }
}
