package com.michaeltroger.gruenerpass

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
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
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputLayout
import com.michaeltroger.gruenerpass.databinding.FragmentMainBinding
import com.michaeltroger.gruenerpass.db.Certificate
import com.michaeltroger.gruenerpass.pager.certificates.CertificateAdapter
import com.michaeltroger.gruenerpass.pager.certificates.CertificateItem
import com.michaeltroger.gruenerpass.pager.certificates.ItemTouchHelperCallback
import com.michaeltroger.gruenerpass.states.ViewEvent
import com.michaeltroger.gruenerpass.states.ViewState
import com.xwray.groupie.Group
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext

class MainFragment : Fragment(R.layout.fragment_main) {

    private val vm by activityViewModels<MainViewModel> { MainViewModelFactory(app = requireActivity().application)}

    @OptIn(ObsoleteCoroutinesApi::class)
    private val thread = newSingleThreadContext("RenderContext")

    private val dialogs: MutableMap<String, AlertDialog?> = hashMapOf()

    private val adapter = CertificateAdapter()
    private var itemTouchHelper: ItemTouchHelper? = null

    private lateinit var binding: FragmentMainBinding

    private val resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.also(vm::setUri)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setHasOptionsMenu(true)

        binding = FragmentMainBinding.bind(view)

        PagerSnapHelper().attachToRecyclerView(binding.certificates)
        binding.certificates.layoutManager = object : LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false) {
            override fun checkLayoutParams(lp: RecyclerView.LayoutParams): Boolean {
                if (itemCount > 1) {
                    lp.width = (width * 0.95).toInt()
                } else {
                    lp.width = width
                }
                return true;
            }

            override fun canScrollHorizontally(): Boolean {
                return itemCount > 1
            }
        }
        itemTouchHelper = ItemTouchHelper(ItemTouchHelperCallback(adapter) {
            vm.onDragFinished(it)
        })
        itemTouchHelper?.attachToRecyclerView(binding.certificates)

        try { // reduce scroll sensitivity for horizontal scrolling to improve vertical scrolling
            val touchSlopField = RecyclerView::class.java.getDeclaredField("mTouchSlop")
            touchSlopField.isAccessible = true
            val touchSlop = touchSlopField.get(binding.certificates) as Int
            touchSlopField.set(binding.certificates, touchSlop * 8)
        } catch (ignore: Exception) {}

        binding.certificates.adapter = adapter

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
                        ViewEvent.ScrollToLastCertificate -> scrollToLastCertificateAfterItemUpdate()
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
        binding.progressIndicator.isVisible = true
        binding.progressIndicator.show()
    }

    private fun showCertificateState(documents: List<Certificate>) {
        binding.progressIndicator.isVisible = false
        val items = documents.map {
            CertificateItem(
                requireContext().applicationContext,
                fileName = it.id,
                documentName = it.name,
                dispatcher= thread,
                onDeleteCalled = { showDoYouWantToDeleteDialog(it.id) },
                onDocumentNameChanged = { updatedDocumentName: String ->
                    vm.onDocumentNameChanged(filename = it.id, documentName = updatedDocumentName)
                },
                onStartDrag = { viewholder ->
                    itemTouchHelper?.startDrag(viewholder)
                }
            )
        }
        adapter.setData(documents.map { it.id }.toList())
        adapter.update(items)
    }

    private fun scrollToLastCertificateAfterItemUpdate() {
       lifecycleScope.launch {
           delay(1000)
           binding.certificates.smoothScrollToPosition(adapter.itemCount - 1)
       }
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
        binding.root.let {
            Snackbar.make(it, R.string.error_reading_pdf, Snackbar.LENGTH_LONG).show()
        }
    }

    private fun closeAllDialogs() {
        dialogs.values.filterNotNull().forEach {
            if (it.isShowing) it.dismiss()
        }
    }

}