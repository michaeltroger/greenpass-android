package com.michaeltroger.gruenerpass

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.*
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
import com.michaeltroger.gruenerpass.pager.certificates.CertificateItem
import com.michaeltroger.gruenerpass.states.ViewEvent
import com.michaeltroger.gruenerpass.states.ViewState
import com.xwray.groupie.Group
import com.xwray.groupie.GroupieAdapter
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import java.util.*

class MainFragment : Fragment(R.layout.fragment_main) {

    private val vm by activityViewModels<MainViewModel> { MainViewModelFactory(requireContext())}

    private var root: ConstraintLayout? = null
    private var progressIndicator: CircularProgressIndicator? = null
    private var certificates: RecyclerView? = null
    private val thread = newSingleThreadContext("RenderContext")

    private val dialogs: MutableMap<String, AlertDialog?> = hashMapOf()

    private val adapter = GroupieAdapter()

    private val resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.also(vm::setUri)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setHasOptionsMenu(true)

        root = view.findViewById(R.id.root)
        progressIndicator = view.findViewById(R.id.progress_indicator)
        certificates = view.findViewById(R.id.certificates)
        PagerSnapHelper().attachToRecyclerView(certificates)
        certificates!!.layoutManager = object : LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false) {
            override fun checkLayoutParams(lp: RecyclerView.LayoutParams): Boolean {
                if (itemCount > 1) {
                    lp.width = (width * 0.95).toInt();
                }
                return true;
            }
        }
        val itemTouchHelper = ItemTouchHelper(ItemTouchHelperCallback(adapter))
        itemTouchHelper.attachToRecyclerView(certificates)
        certificates!!.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.viewState.collect {
                    when (it) {
                        is ViewState.Certificate -> showCertificateState(documents = it.documents)
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

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.add -> {
            openFilePicker()
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

    private fun showCertificateState(documents: SortedMap<String, String>) {
        progressIndicator?.isVisible = false
        val items = mutableListOf<Group>()
        documents.forEach {
            items.add(CertificateItem(
                requireContext().applicationContext,
                fileName = it.key,
                documentName = it.value,
                dispatcher= thread) {
                showDoYouWantToDeleteDialog(it.key)
            })
        }
        adapter.update(items)
    }

    private fun showDoYouWantToDeleteDialog(id: String) {
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setMessage(getString(R.string.dialog_delete_confirmation_message))
            .setPositiveButton(R.string.ok)  { _, _ ->
               vm.onDeleteConfirmed(id)
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