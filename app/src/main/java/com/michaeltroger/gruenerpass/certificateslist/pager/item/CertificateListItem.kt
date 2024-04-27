package com.michaeltroger.gruenerpass.certificateslist.pager.item

import android.view.View
import com.michaeltroger.gruenerpass.R
import com.michaeltroger.gruenerpass.databinding.ItemCertificateListBinding
import com.xwray.groupie.Item
import com.xwray.groupie.viewbinding.BindableItem
import com.xwray.groupie.viewbinding.GroupieViewHolder

@Suppress("LongParameterList")
class CertificateListItem(
    private val fileName: String,
    private val documentName: String,
    private val searchBarcode: Boolean,
    private val onDeleteCalled: () -> Unit,
    private val onOpenDetails: () -> Unit,
    private val onChangeDocumentNameClicked: () -> Unit,
    private val onShareCalled: () -> Unit,
) : BindableItem<ItemCertificateListBinding>() {

    override fun initializeViewBinding(view: View): ItemCertificateListBinding = ItemCertificateListBinding.bind(view)
    override fun getLayout() = R.layout.item_certificate_list

    override fun bind(viewBinding: ItemCertificateListBinding, position: Int) {
        // nothing to do
    }

    override fun bind(viewHolder: GroupieViewHolder<ItemCertificateListBinding>,
                      position: Int,
                      payloads: MutableList<Any>) {
        super.bind(viewHolder, position, payloads)
        viewHolder.binding.apply {
            name.text = documentName
            root.setOnClickListener {
                onOpenDetails()
            }
            editNameIcon.setOnClickListener {
                onChangeDocumentNameClicked()
            }
            shareIcon.setOnClickListener {
                onShareCalled()
            }
            deleteIcon.setOnClickListener {
                onDeleteCalled()
            }
        }
    }

    override fun isSameAs(other: Item<*>): Boolean {
        return viewType == other.viewType
    }

    override fun hasSameContentAs(other: Item<*>): Boolean {
        return (other as? CertificateListItem)?.fileName == fileName
            && (other as? CertificateListItem)?.documentName == documentName
            && (other as? CertificateListItem)?.searchBarcode == searchBarcode
    }
}
