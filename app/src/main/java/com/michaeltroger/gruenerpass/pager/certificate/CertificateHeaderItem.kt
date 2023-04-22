package com.michaeltroger.gruenerpass.pager.certificate

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.View
import androidx.core.widget.doOnTextChanged
import com.michaeltroger.gruenerpass.R
import com.michaeltroger.gruenerpass.databinding.ItemCertificateHeaderBinding
import com.xwray.groupie.Item
import com.xwray.groupie.viewbinding.BindableItem
import com.xwray.groupie.viewbinding.GroupieViewHolder
import android.view.inputmethod.EditorInfo

class CertificateHeaderItem(
    private val documentName: String,
    private val fileName: String,
    private val onDeleteCalled: () -> Unit,
    private val onDocumentNameChanged: (String) -> Unit,
    private val onStartDrag: () -> Unit
) : BindableItem<ItemCertificateHeaderBinding>() {

    override fun initializeViewBinding(view: View) = ItemCertificateHeaderBinding.bind(view)
    override fun getLayout() = R.layout.item_certificate_header

    override fun bind(viewBinding: ItemCertificateHeaderBinding, position: Int) {
        viewBinding.deleteIcon.setOnClickListener {
            onDeleteCalled()
        }
        viewBinding.name.setText(documentName)
        viewBinding.name.doOnTextChanged { text, _, _, _ ->
            onDocumentNameChanged(text.toString() )
        }
        viewBinding.name.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                v.clearFocus()
            }
            false
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun bind(
        viewHolder: GroupieViewHolder<ItemCertificateHeaderBinding>,
        position: Int,
        payloads: MutableList<Any>
    ) {
        super.bind(viewHolder, position, payloads)
        viewHolder.binding.handle.setOnTouchListener { _, event ->
            if (event.actionMasked == MotionEvent.ACTION_DOWN
            ) {
                onStartDrag()
            }
            false
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
