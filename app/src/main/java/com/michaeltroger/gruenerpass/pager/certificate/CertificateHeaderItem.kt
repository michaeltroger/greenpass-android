package com.michaeltroger.gruenerpass.pager.certificate

import android.view.View
import com.michaeltroger.gruenerpass.R
import com.michaeltroger.gruenerpass.databinding.ItemCertificateHeaderBinding
import com.michaeltroger.gruenerpass.pager.certificates.CertificateItem
import com.xwray.groupie.Item
import com.xwray.groupie.viewbinding.BindableItem

class CertificateHeaderItem(private val documentName: String, private val onDeleteCalled: () -> Unit) : BindableItem<ItemCertificateHeaderBinding>() {

    override fun initializeViewBinding(view: View): ItemCertificateHeaderBinding = ItemCertificateHeaderBinding.bind(view)
    override fun getLayout() = R.layout.item_certificate_header

    override fun bind(viewBinding: ItemCertificateHeaderBinding, position: Int) {
        viewBinding.deleteIcon.setOnClickListener {
            onDeleteCalled()
        }
        viewBinding.name.setText(documentName)
    }

    override fun isSameAs(other: Item<*>): Boolean {
        return viewType == other.viewType
    }

    override fun hasSameContentAs(other: Item<*>): Boolean {
        return (other as? CertificateHeaderItem)?.documentName == documentName
    }
}