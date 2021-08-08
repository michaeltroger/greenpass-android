package com.michaeltroger.gruenerpass.pager.certificates

import android.view.View
import com.michaeltroger.gruenerpass.R
import com.michaeltroger.gruenerpass.databinding.ItemAddCertificateBinding
import com.xwray.groupie.viewbinding.BindableItem

class AddCertificateItem(private val onAddCalled: () -> Unit) : BindableItem<ItemAddCertificateBinding>() {

    override fun initializeViewBinding(view: View): ItemAddCertificateBinding = ItemAddCertificateBinding.bind(view)
    override fun getLayout() = R.layout.item_add_certificate

    override fun bind(viewBinding: ItemAddCertificateBinding, position: Int) {
        viewBinding.add.setOnClickListener {
            onAddCalled()
        }
    }

}