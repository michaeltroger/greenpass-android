package com.michaeltroger.gruenerpass.pager.pdfpage

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.michaeltroger.gruenerpass.R
import com.michaeltroger.gruenerpass.model.PAGE_INDEX_QR_CODE
import com.michaeltroger.gruenerpass.model.PdfRenderer
import kotlinx.coroutines.*

class CertificateAdapter(
    private val renderer: PdfRenderer,
    private val hasQrCode: Boolean
) : RecyclerView.Adapter<CertificateAdapter.ViewHolder>() {

    private val scope = CoroutineScope(
        Job() + Dispatchers.Main
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.layout_item_certificate, parent, false))

    override fun getItemCount() = calculateItemCount()

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        scope.launch {
            if (position != 0) {
                holder.deleteIcon?.isVisible = false
                holder.name?.isVisible = false
            } else {
                holder.deleteIcon?.setOnClickListener {
                    showDoYouWantToDeleteDialog(it.context)
                }
            }

            if (position == 0 && hasQrCode) {
                holder.imageView?.setImageBitmap(renderer.getQrCodeIfPresent(PAGE_INDEX_QR_CODE)!!)
            } else {
                if (hasQrCode) {
                    holder.imageView?.setImageBitmap(renderer.renderPage(position - 1))
                } else {
                    holder.imageView?.setImageBitmap(renderer.renderPage(position))
                }
            }
        }
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        scope.cancel()
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var imageView: ImageView? = null
        var name: EditText? = null
        var deleteIcon: ImageButton? = null
        init {
            imageView = view.findViewById(R.id.page)
            name = view.findViewById(R.id.name)
            deleteIcon = view.findViewById(R.id.deleteIcon)
        }
    }

    private fun calculateItemCount(): Int {
        if (hasQrCode) {
           return renderer.getPageCount() + 1
        } else {
            return renderer.getPageCount()
        }
    }

    private fun showDoYouWantToDeleteDialog(context: Context) {
        val dialog = MaterialAlertDialogBuilder(context)
            .setMessage(context.getString(R.string.dialog_delete_confirmation_message))
            .setPositiveButton(R.string.ok)  { _, _ ->
                //TODO
            }
            .setNegativeButton(context.getString(R.string.cancel), null)
            .create()
        dialog.show()
    }
}