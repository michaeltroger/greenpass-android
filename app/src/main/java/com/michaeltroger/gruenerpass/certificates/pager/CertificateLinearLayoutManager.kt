package com.michaeltroger.gruenerpass.certificates.pager

import android.content.Context
import android.util.AttributeSet
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

private const val WIDTH_FACTOR_MULTIPLE_DOCS = 0.95

class CertificateLinearLayoutManager(
    context: Context,
    attributeSet: AttributeSet,
    defStyleAttr: Int,
    defStyleRes: Int
) : LinearLayoutManager(context, attributeSet, defStyleAttr, defStyleRes) {
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
