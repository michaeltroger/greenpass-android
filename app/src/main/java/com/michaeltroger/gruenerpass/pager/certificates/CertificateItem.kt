package com.michaeltroger.gruenerpass.pager.certificates

import android.content.Context
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.michaeltroger.gruenerpass.R
import com.michaeltroger.gruenerpass.databinding.ItemCertificateBinding
import com.michaeltroger.gruenerpass.model.PAGE_INDEX_QR_CODE
import com.michaeltroger.gruenerpass.model.PdfRenderer
import com.michaeltroger.gruenerpass.model.PdfRendererImpl
import com.michaeltroger.gruenerpass.pager.certificate.CertificateHeaderItem
import com.michaeltroger.gruenerpass.pager.certificate.PdfPageItem
import com.michaeltroger.gruenerpass.pager.certificate.QrCodeItem
import com.xwray.groupie.GroupDataObserver
import com.xwray.groupie.GroupieAdapter
import com.xwray.groupie.Item
import com.xwray.groupie.viewbinding.BindableItem
import kotlinx.coroutines.*

class CertificateItem(
    context: Context,
    private val fileName: String,
    dispatcher: CoroutineDispatcher,
    private val documentName: String,
    private val renderer: PdfRenderer = PdfRendererImpl(context, fileName = fileName, dispatcher),
    private val onDeleteCalled: () -> Unit
) : BindableItem<ItemCertificateBinding>() {

    private val scope = CoroutineScope(
        Job() + Dispatchers.Main
    )

    override fun initializeViewBinding(view: View): ItemCertificateBinding = ItemCertificateBinding.bind(view)
    override fun getLayout() = R.layout.item_certificate

    override fun bind(viewBinding: ItemCertificateBinding, position: Int) {
        scope.launch {
            val adapter = GroupieAdapter()
            adapter.add(CertificateHeaderItem(documentName, onDeleteCalled = onDeleteCalled))
            if (renderer.hasQrCode(PAGE_INDEX_QR_CODE)) {
                adapter.add(QrCodeItem(renderer))
            }
            for (pageIndex in 0 until renderer.getPageCount()) {
                adapter.add(PdfPageItem(renderer, pageIndex = pageIndex))
            }

            viewBinding.certificate.layoutManager = LinearLayoutManager(viewBinding.root.context)
            viewBinding.certificate.adapter = adapter
        }
    }

    override fun unregisterGroupDataObserver(groupDataObserver: GroupDataObserver) {
        super.unregisterGroupDataObserver(groupDataObserver)
        scope.cancel()
        renderer.onCleared()
    }

    override fun isSameAs(other: Item<*>): Boolean {
        return viewType == other.viewType
    }

    override fun hasSameContentAs(other: Item<*>): Boolean {
        return (other as? CertificateItem)?.fileName == fileName
    }
}