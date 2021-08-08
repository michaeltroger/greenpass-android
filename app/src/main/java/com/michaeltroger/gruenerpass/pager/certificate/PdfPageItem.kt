package com.michaeltroger.gruenerpass.pager.certificate

import android.view.View
import com.michaeltroger.gruenerpass.R
import com.michaeltroger.gruenerpass.databinding.ItemPdfPageBinding
import com.michaeltroger.gruenerpass.model.PdfRenderer
import com.xwray.groupie.GroupDataObserver
import com.xwray.groupie.viewbinding.BindableItem
import kotlinx.coroutines.*

class PdfPageItem(private val renderer: PdfRenderer, private val pageIndex: Int) : BindableItem<ItemPdfPageBinding>() {

    private val scope = CoroutineScope(
        Job() + Dispatchers.Main
    )

    override fun initializeViewBinding(view: View): ItemPdfPageBinding = ItemPdfPageBinding.bind(view)
    override fun getLayout() = R.layout.item_pdf_page

    override fun bind(viewBinding: ItemPdfPageBinding, position: Int) {
        scope.launch {
            viewBinding.certificate.setImageBitmap(renderer.renderPage(pageIndex))
        }
    }

    override fun unregisterGroupDataObserver(groupDataObserver: GroupDataObserver) {
        super.unregisterGroupDataObserver(groupDataObserver)
        scope.cancel()
    }
}