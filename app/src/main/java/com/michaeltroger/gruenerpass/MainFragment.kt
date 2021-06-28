package com.michaeltroger.gruenerpass

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.michaeltroger.gruenerpass.pdf.PagerAdapter
import com.michaeltroger.gruenerpass.pdf.PdfHandler
import kotlinx.coroutines.launch

class MainFragment : Fragment() {

    private lateinit var adapter: PagerAdapter
    private lateinit var layoutMediator: TabLayoutMediator

    private var addButton: Button? = null
    private var deleteMenuItem: MenuItem? = null
    private var viewPager: ViewPager2? = null
    private var tabLayout: TabLayout? = null
    private var root: ConstraintLayout? = null

    private val resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.also { uri ->
                lifecycleScope.launch {
                    if (PdfHandler.copyPdfToCache(uri) && PdfHandler.parsePdfIntoBitmap()) {
                        showCertificateState()
                    } else {
                        showErrorState()
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        adapter = PagerAdapter(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_main, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setHasOptionsMenu(true)

        root = view.findViewById(R.id.root)

        viewPager = view.findViewById(R.id.pager)
        tabLayout = view.findViewById(R.id.tab_layout)
        layoutMediator = TabLayoutMediator(tabLayout!!, viewPager!!) { tab, position ->
            val textRes: Int
            when (adapter.itemCount) {
                1 -> {
                    tabLayout?.isVisible = false
                    textRes = R.string.tab_title_pdf
                }
                else -> {
                    tabLayout?.isVisible = true
                    textRes = when(position) {
                        0 -> R.string.tab_title_qr
                        else -> R.string.tab_title_pdf
                    }
                }
            }
            tab.text = getString(textRes)
        }

        addButton = view.findViewById(R.id.add)
        addButton?.setOnClickListener {
            openFilePicker()
        }

        lifecycleScope.launch {
            if (PdfHandler.doesFileExist()) {
                if (PdfHandler.parsePdfIntoBitmap()) {
                    showCertificateState()
                } else {
                    showErrorState()
                }
            } else {
                showEmptyState()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu, menu)
        deleteMenuItem = menu.findItem(R.id.delete)

        lifecycleScope.launch {
            deleteMenuItem?.isEnabled = PdfHandler.doesFileExist()
        }
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.delete -> {
            lifecycleScope.launch {
                PdfHandler.deleteFile()
                showEmptyState()
            }
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/pdf"
        }
        resultLauncher.launch(intent)
    }

    private fun showEmptyState() {
        addButton?.isVisible = true
        viewPager?.isVisible = false
        tabLayout?.isVisible = false
        deleteMenuItem?.isEnabled = false
        viewPager?.adapter = null
        layoutMediator.detach()
    }

    private fun showCertificateState() {
        addButton?.isVisible = false
        tabLayout?.isVisible = true
        viewPager?.isVisible = true
        deleteMenuItem?.isEnabled = true
        viewPager?.adapter = adapter
        if (!layoutMediator.isAttached) {
            layoutMediator.attach()
        }
    }

    private fun showErrorState() {
        showEmptyState()
        root?.let {
            Snackbar.make(it, R.string.error_reading_pdf, Snackbar.LENGTH_LONG).show()
        }
    }

}