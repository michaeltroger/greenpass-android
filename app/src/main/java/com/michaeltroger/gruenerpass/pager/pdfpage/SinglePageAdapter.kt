package com.michaeltroger.gruenerpass.pager.pdfpage

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.michaeltroger.gruenerpass.R
import com.michaeltroger.gruenerpass.model.PdfRenderer
import kotlinx.coroutines.*

class SinglePageAdapter(
    private val renderer: PdfRenderer,
) : RecyclerView.Adapter<SinglePageAdapter.ViewHolder>() {

    private val scope = CoroutineScope(
        Job() + Dispatchers.Main
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.layout_item_pdf_page, parent, false))

    override fun getItemCount() = renderer.getPageCount()

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        scope.launch {
            holder.imageView?.setImageBitmap(renderer.renderPage(position))
        }
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        scope.cancel()
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var imageView: ImageView? = null
        init {
            imageView = view.findViewById(R.id.page)
        }
    }
}