package com.michaeltroger.gruenerpass.pager.pdfpage

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.michaeltroger.gruenerpass.R
import com.michaeltroger.gruenerpass.model.PAGE_INDEX_QR_CODE
import com.michaeltroger.gruenerpass.model.PdfRenderer
import kotlinx.coroutines.*

class CertificatesAdapter(
    private val renderer: PdfRenderer
) : RecyclerView.Adapter<CertificatesAdapter.ViewHolder>() {

    private val scope = CoroutineScope(
        Job() + Dispatchers.Main
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.layout_item_certificates, parent, false))

    override fun getItemCount() = 2

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        scope.launch {
            val hasQrCode = renderer.hasQrCode(PAGE_INDEX_QR_CODE)
            holder.recyclerView!!.layoutManager = LinearLayoutManager(holder.recyclerView!!.context)
            holder.recyclerView!!.adapter = CertificateAdapter(renderer, hasQrCode = hasQrCode)
        }
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        scope.cancel()
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var recyclerView: RecyclerView? = null
        init {
            recyclerView = view.findViewById(R.id.certificate)
        }
    }
}