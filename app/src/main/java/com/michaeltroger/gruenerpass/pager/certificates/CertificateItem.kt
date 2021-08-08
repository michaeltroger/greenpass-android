package com.michaeltroger.gruenerpass.pager.certificates

import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.michaeltroger.gruenerpass.R
import com.michaeltroger.gruenerpass.databinding.ItemCertificateBinding
import com.michaeltroger.gruenerpass.model.PAGE_INDEX_QR_CODE
import com.michaeltroger.gruenerpass.model.PdfRenderer
import com.michaeltroger.gruenerpass.pager.certificate.CertificateHeaderItem
import com.michaeltroger.gruenerpass.pager.certificate.PdfPageItem
import com.michaeltroger.gruenerpass.pager.certificate.QrCodeItem
import com.xwray.groupie.GroupDataObserver
import com.xwray.groupie.GroupieAdapter
import com.xwray.groupie.viewbinding.BindableItem
import kotlinx.coroutines.*

class CertificateItem(private val renderer: PdfRenderer, private val onDeleteCalled: () -> Unit) : BindableItem<ItemCertificateBinding>() {

    private val scope = CoroutineScope(
        Job() + Dispatchers.Main
    )

    override fun initializeViewBinding(view: View): ItemCertificateBinding = ItemCertificateBinding.bind(view)
    override fun getLayout() = R.layout.item_certificate

    override fun bind(viewBinding: ItemCertificateBinding, position: Int) {
        scope.launch {
            val adapter = GroupieAdapter()
            adapter.add(CertificateHeaderItem(onDeleteCalled = onDeleteCalled))
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
    }
}