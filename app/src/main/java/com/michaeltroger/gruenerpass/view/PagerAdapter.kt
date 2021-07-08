package com.michaeltroger.gruenerpass.view

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.michaeltroger.gruenerpass.view.PdfPagerFragment
import com.michaeltroger.gruenerpass.view.QrPagerFragment

class PagerAdapter(fragment: Fragment, private val hasQrCode: () -> Boolean): FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int {
        return if (!hasQrCode()) {
            1
        } else {
            2
        }
    }

    override fun createFragment(position: Int): Fragment = when (itemCount) {
        1 -> PdfPagerFragment()
        else -> {
            when (position) {
                0 -> QrPagerFragment()
                else -> PdfPagerFragment()
            }
        }
    }
}