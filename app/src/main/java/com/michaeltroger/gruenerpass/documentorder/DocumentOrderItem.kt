package com.michaeltroger.gruenerpass.documentorder

import android.view.View
import com.michaeltroger.gruenerpass.R
import com.michaeltroger.gruenerpass.databinding.ItemDocumentOrderBinding
import com.xwray.groupie.Item
import com.xwray.groupie.viewbinding.BindableItem
import com.xwray.groupie.viewbinding.GroupieViewHolder

class DocumentOrderItem(
    private val fileName: String,
    private val documentName: String,
    private val onUpClicked: () -> Unit,
    private val onDownClicked: () -> Unit,
) : BindableItem<ItemDocumentOrderBinding>() {

    override fun initializeViewBinding(view: View): ItemDocumentOrderBinding = ItemDocumentOrderBinding.bind(view)
    override fun getLayout() = R.layout.item_document_order

    override fun bind(viewBinding: ItemDocumentOrderBinding, position: Int) {
        // nothing to do
    }

    override fun bind(viewHolder: GroupieViewHolder<ItemDocumentOrderBinding>,
                      position: Int,
                      payloads: MutableList<Any>) {
        super.bind(viewHolder, position, payloads)

        viewHolder.binding.apply {
            documentNameTextField.text = documentName
            up.setOnClickListener {
                onUpClicked()
            }
            down.setOnClickListener {
                onDownClicked()
            }
        }

    }

    override fun isSameAs(other: Item<*>): Boolean {
        return viewType == other.viewType
    }

    override fun hasSameContentAs(other: Item<*>): Boolean {
        return (other as? DocumentOrderItem)?.fileName == fileName
            && (other as? DocumentOrderItem)?.documentName == documentName
    }
}
