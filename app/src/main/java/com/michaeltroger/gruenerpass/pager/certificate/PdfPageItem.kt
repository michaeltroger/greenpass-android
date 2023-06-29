package com.michaeltroger.gruenerpass.pager.certificate

import android.view.View
import androidx.core.view.isVisible
import com.michaeltroger.gruenerpass.R
import com.michaeltroger.gruenerpass.databinding.ItemPdfPageBinding
import com.michaeltroger.gruenerpass.pdf.PdfRenderer
import com.michaeltroger.gruenerpass.qr.QrRenderer
import com.xwray.groupie.Item
import com.xwray.groupie.viewbinding.BindableItem
import com.xwray.groupie.viewbinding.GroupieViewHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

private const val TAG_PDF_LOADED = "pdf_loaded"
private const val TAG_QR_LOADED = "qr_loaded"

class PdfPageItem(
    private val pdfRenderer: PdfRenderer,
    private val qrRenderer: QrRenderer,
    private val fileName: String,
    private val pageIndex: Int,
    private val searchQrCode: Boolean,
    ) : BindableItem<ItemPdfPageBinding>() {

    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.Main
    )

    private var job: Job? = null

    override fun initializeViewBinding(view: View): ItemPdfPageBinding = ItemPdfPageBinding.bind(view)
    override fun getLayout() = R.layout.item_pdf_page

    override fun bind(viewBinding: ItemPdfPageBinding, position: Int) {
        job = scope.launch {
            val page = pdfRenderer.renderPage(pageIndex) ?: return@launch
            if (searchQrCode) {
                qrRenderer.getQrCodeIfPresent(page)?.let { qrCode ->
                    viewBinding.qrcode.setImageBitmap(qrCode)
                    viewBinding.qrcodeWrapper.isVisible = true
                    viewBinding.qrcode.tag = TAG_QR_LOADED
                }
            }
            viewBinding.pdfPage.setImageBitmap(page)
            viewBinding.pdfPage.tag = TAG_PDF_LOADED

            viewBinding.progressIndicatorWrapper.isVisible = false
        }
    }

    override fun unbind(viewHolder: GroupieViewHolder<ItemPdfPageBinding>) {
        super.unbind(viewHolder)
        job?.cancel()
    }

    override fun isSameAs(other: Item<*>): Boolean {
        return viewType == other.viewType
    }

    override fun hasSameContentAs(other: Item<*>): Boolean {
        return (other as? PdfPageItem)?.pageIndex == pageIndex && (other as? PdfPageItem)?.fileName == fileName
    }
}
