package com.michaeltroger.gruenerpass.certificates.pager.item.partials

import android.view.View
import com.michaeltroger.gruenerpass.R
import com.michaeltroger.gruenerpass.databinding.ItemCertificateHeaderBinding
import com.xwray.groupie.Item
import com.xwray.groupie.viewbinding.BindableItem

@Suppress("LongParameterList")
class CertificateHeaderItem(
    private val documentName: String,
    private val fileName: String,
    private val onDeleteCalled: () -> Unit,
    private val onDocumentNameClicked: () -> Unit,
    private val onShareCalled: () -> Unit,
) : BindableItem<ItemCertificateHeaderBinding>() {

    override fun initializeViewBinding(view: View) = ItemCertificateHeaderBinding.bind(view)
    override fun getLayout() = R.layout.item_certificate_header

    override fun bind(viewBinding: ItemCertificateHeaderBinding, position: Int) {
        viewBinding.apply {
            deleteIcon.setOnClickListener {
                onDeleteCalled()
            }
            shareIcon.setOnClickListener {
                onShareCalled()
            }
            name.text = documentName
            name.setOnClickListener {
                onDocumentNameClicked()
            }
        }

    }

    override fun isSameAs(other: Item<*>): Boolean {
        return viewType == other.viewType
    }

    override fun hasSameContentAs(other: Item<*>): Boolean {
        return (other as? CertificateHeaderItem)?.fileName == fileName
            && (other as? CertificateHeaderItem)?.documentName == documentName
    }
}
