package com.michaeltroger.gruenerpass.pager.certificates

import android.content.Context
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.michaeltroger.gruenerpass.R
import com.michaeltroger.gruenerpass.databinding.ItemCertificateBinding
import com.michaeltroger.gruenerpass.pager.certificate.CertificateHeaderItem
import com.michaeltroger.gruenerpass.pager.certificate.PdfPageItem
import com.michaeltroger.gruenerpass.pdf.PdfRenderer
import com.michaeltroger.gruenerpass.pdf.PdfRendererBuilder
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
    dispatcher: CoroutineDispatcher,
    private val documentName: String,
    private val searchQrCode: Boolean,
    private val onDeleteCalled: () -> Unit,
    private val onDocumentNameChanged: (String) -> Unit,
    private val onStartDrag: (RecyclerView.ViewHolder) -> Unit,
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
        viewHolder.binding.certificate.layoutManager = LinearLayoutManager(viewHolder.binding.root.context)
        viewHolder.binding.certificate.adapter = adapter
        job = scope.launch {
            val itemList = mutableListOf<Group>()
            itemList.add(CertificateHeaderItem(
                documentName = documentName,
                fileName = fileName,
                onDeleteCalled = onDeleteCalled,
                onDocumentNameChanged = onDocumentNameChanged,
                onStartDrag =  {
                    onStartDrag(viewHolder)
                },
                onShareCalled = onShareCalled,
            ))
            for (pageIndex in 0 until renderer.getPageCount()) {
                itemList.add(PdfPageItem(renderer, pageIndex = pageIndex, fileName = fileName, searchQrCode = searchQrCode))
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
            && (other as? CertificateItem)?.searchQrCode == searchQrCode
    }
}
