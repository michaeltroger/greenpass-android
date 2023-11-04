package com.michaeltroger.gruenerpass.pager.certificates

import android.content.Context
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

private const val WIDTH_FACTOR_MULTIPLE_DOCS = 0.95

class CertificateLinearLayoutManager(context: Context) : LinearLayoutManager(context, RecyclerView.HORIZONTAL, false) {
    override fun checkLayoutParams(lp: RecyclerView.LayoutParams): Boolean {
        if (itemCount > 1) {
            lp.width = (width * WIDTH_FACTOR_MULTIPLE_DOCS).toInt()
        } else {
            lp.width = width
        }
        return true
    }

    override fun canScrollHorizontally(): Boolean {
        return itemCount > 1
    }
}