package com.michaeltroger.gruenerpass.certificates.pager.item.partials

import android.content.Context
import android.graphics.Bitmap
import android.view.View
import androidx.core.view.isVisible
import com.michaeltroger.gruenerpass.R
import com.michaeltroger.gruenerpass.barcode.BarcodeRenderer
import com.michaeltroger.gruenerpass.cache.BitmapCache
import com.michaeltroger.gruenerpass.databinding.ItemCertificatePartialPdfPageBinding
import com.michaeltroger.gruenerpass.settings.BarcodeSearchMode
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
    private val searchBarcode: BarcodeSearchMode,
    ) : BindableItem<ItemCertificatePartialPdfPageBinding>() {

    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO
    )

    private var job: Job? = null

    private val barcodeCacheKey = "barcode-$fileName-$pageIndex"
    private val pdfCacheKey = "pdf-$fileName-$pageIndex"

    override fun initializeViewBinding(view: View): ItemCertificatePartialPdfPageBinding
        = ItemCertificatePartialPdfPageBinding.bind(view)

    override fun getLayout() = R.layout.item_certificate_partial_pdf_page

    override fun bind(viewBinding: ItemCertificatePartialPdfPageBinding, position: Int) {
        job = scope.launch {
            val context = viewBinding.root.context

            var pdf: Bitmap? = BitmapCache.memoryCache.get(pdfCacheKey)
            if(!isActive) return@launch
            var barcode: Bitmap? = BitmapCache.memoryCache.get(barcodeCacheKey)
            if(!isActive) return@launch

            if (pdf == null) {
                val bitmaps = generateBitmaps(context) { isActive } ?: return@launch
                pdf = bitmaps.first
                barcode = bitmaps.second
                if(!isActive) return@launch

                if (barcode != null) {
                    BitmapCache.memoryCache.put(
                        barcodeCacheKey,
                        barcode
                    )
                }
                BitmapCache.memoryCache.put(
                    pdfCacheKey,
                    pdf
                )
                if(!isActive) return@launch
            }

            withContext(Dispatchers.Main) {
                if(!isActive) return@withContext
                viewBinding.pdfPage.setImageBitmap(pdf)
                viewBinding.pdfPage.tag = TAG_PDF_LOADED

                if (searchBarcode != BarcodeSearchMode.DISABLED && barcode != null) {
                    viewBinding.barcode.setImageBitmap(barcode)
                    viewBinding.barcodeWrapper.isVisible = true
                    viewBinding.barcode.tag = TAG_BARCODE_LOADED
                }

                viewBinding.progressIndicatorWrapper.isVisible = false
            }
        }
    }

    @Suppress("ReturnCount")
    private suspend fun generateBitmaps(context: Context, isActive: () -> Boolean): Pair<Bitmap, Bitmap?>? {
        val pdf: Bitmap
        val barcode: Bitmap?

        val tempPdf = pdfRenderer.renderPage(pageIndex = pageIndex, highResolution = searchBarcode == BarcodeSearchMode.EXTENDED) ?: return null
        if(!isActive()) return null

        barcode = if (searchBarcode != BarcodeSearchMode.DISABLED) {
            barcodeRenderer.getBarcodeIfPresent(document = tempPdf, tryExtraHard = searchBarcode == BarcodeSearchMode.EXTENDED)
        } else {
            null
        }
        if(!isActive()) return null

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

        return pdf to barcode
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
