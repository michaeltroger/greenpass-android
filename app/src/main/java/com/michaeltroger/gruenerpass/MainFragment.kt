package com.michaeltroger.gruenerpass

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.android.material.textfield.TextInputLayout
import com.michaeltroger.gruenerpass.pdf.PagerAdapter
import com.michaeltroger.gruenerpass.pdf.PdfHandler
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class MainFragment : Fragment(R.layout.fragment_main) {

    private val mainViewModel by activityViewModels<MainViewModel>()

    private lateinit var adapter: PagerAdapter
    private lateinit var layoutMediator: TabLayoutMediator

    private var addButton: Button? = null
    private var deleteMenuItem: MenuItem? = null
    private var replaceMenuItem: MenuItem? = null
    private var viewPager: ViewPager2? = null
    private var tabLayout: TabLayout? = null
    private var root: ConstraintLayout? = null

    private val resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.also { uri ->
                lifecycleScope.launch {
                    handleFileFromUri(uri)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        adapter = PagerAdapter(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setHasOptionsMenu(true)

        root = view.findViewById(R.id.root)
        viewPager = view.findViewById(R.id.pager)
        tabLayout = view.findViewById(R.id.tab_layout)
        addButton = view.findViewById(R.id.add)

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

        addButton?.setOnClickListener {
            openFilePicker()
        }

        val sharedFile: Uri? = arguments?.get(MainActivity.BUNDLE_KEY_URI) as? Uri
        if (sharedFile != null) {
            lifecycleScope.launch {
                if (PdfHandler.doesFileExist()) {
                    showDoYouWantToReplaceDialog(sharedFile)
                } else {
                    handleFileFromUri(sharedFile)
                }
            }
        }

        lifecycleScope.launch {
            mainViewModel.updatedUri.collect {
                if (PdfHandler.doesFileExist()) {
                    showDoYouWantToReplaceDialog(it)
                } else {
                    handleFileFromUri(it)
                }
            }
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
        replaceMenuItem = menu.findItem(R.id.replace)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.delete -> {
            showDoYouWantToDeleteDialog()
            true
        }
        R.id.replace -> {
            openFilePicker()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    private suspend fun handleFileFromUri(uri: Uri) {
        if (PdfHandler.isPdfPasswordProtected(uri)) {
            showEnterPasswordDialog(uri)
        } else {
            showEmptyState()
            if (PdfHandler.copyPdfToCache(uri) && PdfHandler.parsePdfIntoBitmap()) {
                showCertificateState()
            } else {
                showErrorState()
            }
        }
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
        deleteMenuItem?.isVisible = false
        replaceMenuItem?.isVisible = false
        viewPager?.adapter = null
        layoutMediator.detach()
    }

    private fun showCertificateState() {
        addButton?.isVisible = false
        tabLayout?.isVisible = true
        viewPager?.isVisible = true
        deleteMenuItem?.isVisible = true
        replaceMenuItem?.isVisible = true
        viewPager?.adapter = adapter
        if (!layoutMediator.isAttached) {
            layoutMediator.attach()
        }
    }

    private fun showDoYouWantToDeleteDialog() {
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.dialog_delete_confirmation_title))
            .setPositiveButton(R.string.ok)  { _, _ ->
                lifecycleScope.launch {
                    PdfHandler.deleteFile()
                    showEmptyState()
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .create();
        dialog.show();
    }

    private fun showDoYouWantToReplaceDialog(uri: Uri) {
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.dialog_replace_confirmation_title))
            .setPositiveButton(R.string.ok)  { _, _ ->
                lifecycleScope.launch {
                    handleFileFromUri(uri)
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .create();
        dialog.show();
    }

    private fun showEnterPasswordDialog(uri: Uri) {
        val customAlertDialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.layout_password_dialog, null, false)

        val passwordTextField = customAlertDialogView.findViewById<TextInputLayout>(R.id.password_text_field)

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.dialog_password_protection_title))
            .setView(customAlertDialogView)
            .setPositiveButton(R.string.ok)  { _, _ ->
                lifecycleScope.launch {
                    if (PdfHandler.decryptAndCopyPdfToCache(uri = uri, password = passwordTextField.editText!!.text.toString())) {
                        showEmptyState()
                        if (PdfHandler.parsePdfIntoBitmap()) {
                            showCertificateState()
                        } else {
                            showErrorState()
                        }
                    } else {
                        showEnterPasswordDialog(uri)
                    }
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .create();
        dialog.show();
    }

    private fun showErrorState() {
        showEmptyState()
        root?.let {
            Snackbar.make(it, R.string.error_reading_pdf, Snackbar.LENGTH_LONG).show()
        }
    }

}