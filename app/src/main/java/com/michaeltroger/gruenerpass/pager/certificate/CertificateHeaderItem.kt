package com.michaeltroger.gruenerpass.pager.certificate

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import com.michaeltroger.gruenerpass.R
import com.michaeltroger.gruenerpass.databinding.ItemCertificateHeaderBinding
import com.xwray.groupie.Item
import com.xwray.groupie.viewbinding.BindableItem
import com.xwray.groupie.viewbinding.GroupieViewHolder

@Suppress("LongParameterList")
class CertificateHeaderItem(
    private val documentName: String,
    private val fileName: String,
    private val showDragButtons: Boolean,
    private val onDeleteCalled: () -> Unit,
    private val onDocumentNameChanged: (String) -> Unit,
    private val onStartDrag: () -> Unit,
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
            name.setText(documentName)
            name.doOnTextChanged { text, _, _, _ ->
                onDocumentNameChanged(text.toString() )
            }
            name.setOnEditorActionListener { v, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    v.clearFocus()
                }
                false
            }
        }

    }

    @SuppressLint("ClickableViewAccessibility")
    override fun bind(
        viewHolder: GroupieViewHolder<ItemCertificateHeaderBinding>,
        position: Int,
        payloads: MutableList<Any>
    ) {
        super.bind(viewHolder, position, payloads)
        viewHolder.binding.handle.isVisible = showDragButtons
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
            && (other as? CertificateHeaderItem)?.showDragButtons == showDragButtons
    }
}
