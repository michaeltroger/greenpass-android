package com.michaeltroger.gruenerpass.certificates.pager.certificates

import android.content.Context
import android.view.View
import com.michaeltroger.gruenerpass.R
import com.michaeltroger.gruenerpass.barcode.BarcodeRenderer
import com.michaeltroger.gruenerpass.databinding.ItemCertificateBinding
import com.michaeltroger.gruenerpass.certificates.pager.certificate.CertificateHeaderItem
import com.michaeltroger.gruenerpass.certificates.pager.certificate.PdfPageItem
import com.michaeltroger.gruenerpass.pdfrenderer.PdfRenderer
import com.michaeltroger.gruenerpass.pdfrenderer.PdfRendererBuilder
import com.xwray.groupie.Group
import com.xwray.groupie.GroupieAdapter
import com.xwray.groupie.Item
import com.xwray.groupie.viewbinding.BindableItem
import com.xwray.groupie.viewbinding.GroupieViewHolder
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@Suppress("LongParameterList")
class CertificateItem(
    context: Context,
    private val fileName: String,
    private val barcodeRenderer: BarcodeRenderer,
    dispatcher: CoroutineDispatcher,
    private val documentName: String,
    private val searchBarcode: Boolean,
    private val onDeleteCalled: () -> Unit,
    private val onDocumentNameClicked: () -> Unit,
    private val onShareCalled: () -> Unit,
) : BindableItem<ItemCertificateBinding>() {

    private val renderer: PdfRenderer = PdfRendererBuilder.create(context, fileName = fileName, dispatcher)

    private val adapter = GroupieAdapter()
    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.Main
    )

    private var job: Job? = null

    override fun initializeViewBinding(view: View): ItemCertificateBinding = ItemCertificateBinding.bind(view)
    override fun getLayout() = R.layout.item_certificate

    override fun bind(viewBinding: ItemCertificateBinding, position: Int) {
        // nothing to do
    }

    override fun bind(viewHolder: GroupieViewHolder<ItemCertificateBinding>,
                      position: Int,
                      payloads: MutableList<Any>) {
        super.bind(viewHolder, position, payloads)
        viewHolder.binding.certificate.adapter = adapter
        job = scope.launch {
            val itemList = mutableListOf<Group>()
            itemList.add(
                CertificateHeaderItem(
                documentName = documentName,
                fileName = fileName,
                onDeleteCalled = onDeleteCalled,
                onDocumentNameClicked = onDocumentNameClicked,
                onShareCalled = onShareCalled,
            )
            )
            for (pageIndex in 0 until renderer.getPageCount()) {
                itemList.add(
                    PdfPageItem(
                        pdfRenderer = renderer,
                        barcodeRenderer = barcodeRenderer,
                        pageIndex = pageIndex,
                        fileName = fileName,
                        searchBarcode = searchBarcode
                    )
                )
            }
            adapter.update(itemList)
        }
    }

    override fun unbind(viewHolder: GroupieViewHolder<ItemCertificateBinding>) {
        super.unbind(viewHolder)
        job?.cancel()
    }

    override fun isSameAs(other: Item<*>): Boolean {
        return viewType == other.viewType
    }

    override fun hasSameContentAs(other: Item<*>): Boolean {
        return (other as? CertificateItem)?.fileName == fileName
            && (other as? CertificateItem)?.documentName == documentName
            && (other as? CertificateItem)?.searchBarcode == searchBarcode
    }
}
