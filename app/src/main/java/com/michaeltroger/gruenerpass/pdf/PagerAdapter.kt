package com.michaeltroger.gruenerpass.pdf

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class PagerAdapter(fragment: Fragment): FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int {
        return if (PdfHandler.getQrBitmap() == null) {
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