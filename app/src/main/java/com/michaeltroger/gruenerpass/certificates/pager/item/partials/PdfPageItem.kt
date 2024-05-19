package com.michaeltroger.gruenerpass.certificates.pager.item.partials

import android.content.Context
import android.graphics.Bitmap
import android.view.View
import androidx.core.view.isVisible
import coil.imageLoader
import coil.memory.MemoryCache
import com.michaeltroger.gruenerpass.R
import com.michaeltroger.gruenerpass.barcode.BarcodeRenderer
import com.michaeltroger.gruenerpass.databinding.ItemCertificatePartialPdfPageBinding
import com.xwray.groupie.Item
import com.xwray.groupie.viewbinding.BindableItem
import com.xwray.groupie.viewbinding.GroupieViewHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG_PDF_LOADED = "pdf_loaded"
private const val TAG_BARCODE_LOADED = "barcode_loaded"

class PdfPageItem(
    private val pdfRenderer: com.michaeltroger.gruenerpass.pdfrenderer.PdfRenderer,
    private val barcodeRenderer: BarcodeRenderer,
    private val fileName: String,
    private val pageIndex: Int,
    private val searchBarcode: Boolean,
    private val extraHardBarcodeSearch: Boolean,
    ) : BindableItem<ItemCertificatePartialPdfPageBinding>() {

    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO
    )

    private var job: Job? = null

    private val barcodeCacheKey = MemoryCache.Key("barcode-$fileName-$pageIndex")
    private val pdfCacheKey = MemoryCache.Key("pdf-$fileName-$pageIndex")

    override fun initializeViewBinding(view: View): ItemCertificatePartialPdfPageBinding
        = ItemCertificatePartialPdfPageBinding.bind(view)

    override fun getLayout() = R.layout.item_certificate_partial_pdf_page

    @Suppress("CyclomaticComplexMethod")
    override fun bind(viewBinding: ItemCertificatePartialPdfPageBinding, position: Int) {
        job = scope.launch {
            val context = viewBinding.root.context
            val imageLoader = context.imageLoader

            var pdf: Bitmap? = imageLoader.memoryCache?.get(pdfCacheKey)?.bitmap
            if(!isActive) return@launch
            var barcode: Bitmap? = imageLoader.memoryCache?.get(barcodeCacheKey)?.bitmap
            if(!isActive) return@launch

            if (pdf == null) {
                val tempPdf = pdfRenderer.renderPage(pageIndex, extraHardBarcodeSearch) ?: return@launch
                if(!isActive) return@launch
                if (searchBarcode) {
                    barcode = barcodeRenderer.getBarcodeIfPresent(tempPdf, extraHardBarcodeSearch)
                    if(!isActive) return@launch
                    if (barcode != null) {
                        imageLoader.memoryCache?.set(
                            barcodeCacheKey,
                            MemoryCache.Value(barcode)
                        )
                    }
                }
                pdf = if (tempPdf.width > context.screenWidth || tempPdf.height > context.screenHeight) {
                    Bitmap.createScaledBitmap(
                        tempPdf,
                        context.screenWidth,
                        (context.screenWidth.toFloat() / tempPdf.width * tempPdf.height).toInt(),
                        true
                    )
                } else {
                    tempPdf
                }
                imageLoader.memoryCache?.set(
                    pdfCacheKey,
                    MemoryCache.Value(pdf)
                )
                if(!isActive) return@launch
            }

            withContext(Dispatchers.Main) {
                if(!isActive) return@withContext
                viewBinding.pdfPage.setImageBitmap(pdf)
                viewBinding.pdfPage.tag = TAG_PDF_LOADED

                if (searchBarcode && barcode != null) {
                    viewBinding.barcode.setImageBitmap(barcode)
                    viewBinding.barcodeWrapper.isVisible = true
                    viewBinding.barcode.tag = TAG_BARCODE_LOADED
                }

                viewBinding.progressIndicatorWrapper.isVisible = false
            }
        }
    }

    override fun unbind(viewHolder: GroupieViewHolder<ItemCertificatePartialPdfPageBinding>) {
        super.unbind(viewHolder)
        job?.cancel()

        viewHolder.binding.barcodeWrapper.isVisible = false
        viewHolder.binding.barcode.tag = null
        viewHolder.binding.barcode.setImageBitmap(null)

        viewHolder.binding.pdfPage.tag = null
        viewHolder.binding.pdfPage.setImageBitmap(null)

        viewHolder.binding.progressIndicatorWrapper.isVisible = true
    }

    override fun isSameAs(other: Item<*>): Boolean {
        return viewType == other.viewType
    }

    override fun hasSameContentAs(other: Item<*>): Boolean {
        return (other as? PdfPageItem)?.pageIndex == pageIndex &&
                (other as? PdfPageItem)?.fileName == fileName
    }

    private val Context.screenWidth: Int
        get() = resources.displayMetrics.widthPixels

    private val Context.screenHeight: Int
        get() = resources.displayMetrics.heightPixels
}
