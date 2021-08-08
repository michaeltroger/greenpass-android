package com.michaeltroger.gruenerpass

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputLayout
import com.michaeltroger.gruenerpass.pager.certificates.AddCertificateItem
import com.michaeltroger.gruenerpass.pager.certificates.CertificateItem
import com.michaeltroger.gruenerpass.states.ViewEvent
import com.michaeltroger.gruenerpass.states.ViewState
import com.xwray.groupie.GroupieAdapter
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class MainFragment : Fragment(R.layout.fragment_main) {

    private val vm by activityViewModels<MainViewModel> { MainViewModelFactory(requireContext())}

    private var addButton: Button? = null
    private var deleteMenuItem: MenuItem? = null
    private var root: ConstraintLayout? = null
    private var progressIndicator: CircularProgressIndicator? = null
    private var certificates: RecyclerView? = null

    private val dialogs: MutableMap<String, AlertDialog?> = hashMapOf()

    private val adapter = GroupieAdapter()

    private val resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.also(vm::setUri)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        root = view.findViewById(R.id.root)
        addButton = view.findViewById(R.id.add)
        progressIndicator = view.findViewById(R.id.progress_indicator)
        certificates = view.findViewById(R.id.certificates)
        PagerSnapHelper().attachToRecyclerView(certificates)
        certificates!!.layoutManager = object : LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false) {
            override fun checkLayoutParams(lp: RecyclerView.LayoutParams): Boolean {
                lp.width = (width * 0.95).toInt();
                return true;
            }
        }
        val itemTouchHelper = ItemTouchHelper(ItemTouchHelperCallback(adapter))
        itemTouchHelper.attachToRecyclerView(certificates)

        addButton?.setOnClickListener {
            openFilePicker()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.viewState.collect {
                    when (it) {
                        is ViewState.Certificate -> showCertificateState(hasQrCode = it.hasQrCode)
                        ViewState.Empty -> showEmptyState()
                        ViewState.Loading -> showLoadingState()
                    }.let{}
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.viewEvent.collect {
                    when (it) {
                        ViewEvent.CloseAllDialogs -> closeAllDialogs()
                        ViewEvent.ShowPasswordDialog -> showEnterPasswordDialog()
                        ViewEvent.ErrorParsingFile -> showFileCanNotBeReadError()
                    }.let{}
                }
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

    private fun showLoadingState() {
        progressIndicator?.isVisible = true
        progressIndicator?.show()
    }

    private fun showEmptyState() {
        progressIndicator?.isVisible = false
        addButton?.isVisible = true
        deleteMenuItem?.isVisible = false
        certificates?.adapter = null
        certificates?.isVisible = false
    }

    private fun showCertificateState(hasQrCode: Boolean) {
        progressIndicator?.isVisible = false
        addButton?.isVisible = false
        certificates?.isVisible = true
        deleteMenuItem?.isVisible = true
        if (certificates?.adapter == null) {
            adapter.add(CertificateItem(vm.pdfRenderer) { showDoYouWantToDeleteDialog() })
            adapter.add(CertificateItem(vm.pdfRenderer) { showDoYouWantToDeleteDialog() })
            adapter.add(CertificateItem(vm.pdfRenderer) { showDoYouWantToDeleteDialog() })
            adapter.add(AddCertificateItem(onAddCalled = { openFilePicker() }))
            certificates!!.adapter = adapter
        }
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