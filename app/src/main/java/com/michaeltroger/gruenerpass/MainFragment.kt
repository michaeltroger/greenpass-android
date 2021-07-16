package com.michaeltroger.gruenerpass

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.android.material.textfield.TextInputLayout
import com.michaeltroger.gruenerpass.pager.PagerAdapter
import com.michaeltroger.gruenerpass.states.ViewEvent
import com.michaeltroger.gruenerpass.states.ViewState
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class MainFragment : Fragment(R.layout.fragment_main) {

    private val vm by activityViewModels<MainViewModel>()

    private lateinit var adapter: PagerAdapter
    private lateinit var layoutMediator: TabLayoutMediator

    private var addButton: Button? = null
    private var deleteMenuItem: MenuItem? = null
    private var viewPager: ViewPager2? = null
    private var tabLayout: TabLayout? = null
    private var root: ConstraintLayout? = null
    private var progressIndicator: CircularProgressIndicator? = null

    private var dialogs: MutableMap<String, AlertDialog?> = hashMapOf()

    private val resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.also(vm::setUri)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setHasOptionsMenu(true)

        root = view.findViewById(R.id.root)
        viewPager = view.findViewById(R.id.pager)
        tabLayout = view.findViewById(R.id.tab_layout)
        addButton = view.findViewById(R.id.add)
        progressIndicator = view.findViewById(R.id.progress_indicator)

        adapter = PagerAdapter(this, hasQrCode = { vm.hasQrCode })
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

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.viewState.collect {
                    when (it) {
                        ViewState.Certificate -> showCertificateState()
                        ViewState.Empty -> showEmptyState()
                        ViewState.Loading -> showLoadingState()
                    }.let{}
                }
            }
        }

        addButton?.setOnClickListener {
            openFilePicker()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.viewEvent.collect {
                    when (it) {
                        ViewEvent.CloseAllDialogs -> closeAllDialogs()
                        ViewEvent.ShowDeleteDialog -> showDoYouWantToDeleteDialog()
                        ViewEvent.ShowPasswordDialog -> showEnterPasswordDialog()
                        ViewEvent.ShowReplaceDialog -> showDoYouWantToReplaceDialog()
                        ViewEvent.ErrorParsingFile -> showFileCanNotBeReadError()
                    }.let{}
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu, menu)
        deleteMenuItem = menu.findItem(R.id.delete)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.delete -> {
            showDoYouWantToDeleteDialog()
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

    private fun showLoadingState() {
        progressIndicator?.isVisible = true
        progressIndicator?.show()
    }

    private fun showEmptyState() {
        progressIndicator?.isVisible = false
        addButton?.isVisible = true
        viewPager?.isVisible = false
        tabLayout?.isVisible = false
        deleteMenuItem?.isVisible = false
        viewPager?.adapter = null
        layoutMediator.detach()
    }

    private fun showCertificateState() {
        progressIndicator?.isVisible = false
        addButton?.isVisible = false
        tabLayout?.isVisible = true
        viewPager?.isVisible = true
        deleteMenuItem?.isVisible = true
        if (viewPager?.adapter == null) {
            viewPager?.adapter = adapter
        }
        if (layoutMediator.isAttached) {
            layoutMediator.detach()
        }
        layoutMediator.attach()
    }

    private fun showDoYouWantToDeleteDialog() {
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setMessage(getString(R.string.dialog_delete_confirmation_message))
            .setPositiveButton(R.string.ok)  { _, _ ->
                vm.onDeleteConfirmed()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .create()
        dialogs["delete"] = dialog
        dialog.show()
    }

    private fun showDoYouWantToReplaceDialog() {
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setMessage(getString(R.string.dialog_replace_confirmation_message))
            .setPositiveButton(R.string.ok)  { _, _ ->
                vm.onReplaceConfirmed()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .create()
        dialogs["replace"] = dialog
        dialog.show()
    }

    private fun showEnterPasswordDialog() {
        val customAlertDialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.layout_password_dialog, null, false)

        val passwordTextField = customAlertDialogView.findViewById<TextInputLayout>(R.id.password_text_field)

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.dialog_password_protection_title))
            .setView(customAlertDialogView)
            .setPositiveButton(R.string.ok)  { _, _ ->
                vm.onPasswordEntered(passwordTextField.editText!!.text.toString())
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .create()
        dialogs["password"] = dialog
        dialog.show()
    }

    private fun showFileCanNotBeReadError() {
        root?.let {
            Snackbar.make(it, R.string.error_reading_pdf, Snackbar.LENGTH_LONG).show()
        }
    }

    private fun closeAllDialogs() {
        dialogs.values.filterNotNull().forEach {
            if (it.isShowing) it.dismiss()
        }
    }

}