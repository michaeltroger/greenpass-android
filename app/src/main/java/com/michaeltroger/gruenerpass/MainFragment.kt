package com.michaeltroger.gruenerpass

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.WindowManager.LayoutParams
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
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
import com.michaeltroger.gruenerpass.locator.Locator
import com.michaeltroger.gruenerpass.pager.certificates.CertificateAdapter
import com.michaeltroger.gruenerpass.pager.certificates.CertificateItem
import com.michaeltroger.gruenerpass.pager.certificates.ItemTouchHelperCallback
import com.michaeltroger.gruenerpass.more.MoreActivity
import com.michaeltroger.gruenerpass.settings.SettingsActivity
import com.michaeltroger.gruenerpass.states.ViewEvent
import com.michaeltroger.gruenerpass.states.ViewState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import java.util.concurrent.Executor
import kotlinx.coroutines.DelicateCoroutinesApi

private const val WIDTH_FACTOR_MULTIPLE_DOCS = 0.95
private const val TOUCH_SLOP_FACTOR = 8
private const val SCROLL_TO_LAST_DELAY_MS = 1000L

@Suppress("TooManyFunctions")
class MainFragment : Fragment(R.layout.fragment_main), MenuProvider {

    private var addMenuButton: MenuItem? = null
    private val vm by activityViewModels<MainViewModel> { MainViewModelFactory(app = requireActivity().application)}

    @OptIn(DelicateCoroutinesApi::class)
    private val thread = newSingleThreadContext("RenderContext")

    private val dialogs: MutableMap<String, AlertDialog?> = hashMapOf()

    private val adapter = CertificateAdapter()
    private var itemTouchHelper: ItemTouchHelper? = null

    private lateinit var binding: FragmentMainBinding

    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo

    private val resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.also(vm::setUri)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)

        binding = FragmentMainBinding.bind(view)
        executor = ContextCompat.getMainExecutor(requireContext())
        biometricPrompt = BiometricPrompt(this, executor, MyAuthenticationCallback())

        promptInfo = Locator.biometricPromptInfo(requireContext())

        PagerSnapHelper().attachToRecyclerView(binding.certificates)
        binding.certificates.layoutManager = MyInnerLayoutManager(requireContext())
        itemTouchHelper = ItemTouchHelper(ItemTouchHelperCallback(adapter) {
            vm.onDragFinished(it)
        }).apply {
            attachToRecyclerView(binding.certificates)
        }

        try { // reduce scroll sensitivity for horizontal scrolling to improve vertical scrolling
            val touchSlopField = RecyclerView::class.java.getDeclaredField("mTouchSlop")
            touchSlopField.isAccessible = true
            val touchSlop = touchSlopField.get(binding.certificates) as Int
            touchSlopField.set(binding.certificates, touchSlop * TOUCH_SLOP_FACTOR)
        } catch (ignore: Exception) {}

        binding.certificates.adapter = adapter

        binding.authenticate.setOnClickListener {
            biometricPrompt.authenticate(promptInfo)
        }

        binding.addButton.setOnClickListener {
            openFilePicker()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.viewState.collect {
                    updateMenuState()
                    updateScreenBrightness(fullBrightness = it.fullBrightness)
                    when (it) {
                        is ViewState.Loading -> showLoadingState()
                        is ViewState.Empty -> showEmptyState()
                        is ViewState.Normal -> showCertificateState(
                            documents = it.documents,
                            searchQrCode = it.searchQrCode,
                        )
                        is ViewState.Locked -> showLockedState()
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.viewEvent.collect {
                    when (it) {
                        ViewEvent.CloseAllDialogs -> closeAllDialogs()
                        ViewEvent.ShowPasswordDialog -> showEnterPasswordDialog()
                        ViewEvent.ErrorParsingFile -> showFileCanNotBeReadError()
                        ViewEvent.ScrollToLastCertificate -> scrollToLastCertificateAfterItemUpdate()
                    }
                }
            }
        }
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.menu, menu)
        addMenuButton = menu.findItem(R.id.add)
        updateMenuState()
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean = when (menuItem.itemId) {
        R.id.add -> {
            openFilePicker()
            true
        }
        R.id.openMore -> {
            val intent = Intent(requireContext(), MoreActivity::class.java)
            startActivity(intent)
            true
        }
        R.id.openSettings -> {
            val intent = Intent(requireContext(), SettingsActivity::class.java)
            startActivity(intent)
            true
        }
        else -> false
    }

    private fun updateMenuState() {
        addMenuButton?.isVisible = vm.viewState.value::class.java == ViewState.Normal::class.java
    }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/pdf"
        }
        resultLauncher.launch(intent)
    }

    private fun showLockedState() {
        binding.progressIndicator.isVisible = false
        binding.addButton.isVisible = false
        binding.authenticate.isVisible = true
        adapter.clear()
        biometricPrompt.authenticate(promptInfo)
    }

    private fun showLoadingState() {
        binding.progressIndicator.isVisible = true
        binding.addButton.isVisible = false
        binding.authenticate.isVisible = false
        binding.progressIndicator.show()
    }

    private fun showEmptyState() {
        binding.progressIndicator.isVisible = false
        binding.addButton.isVisible = true
        binding.authenticate.isVisible = false
        adapter.clear()
    }

    private fun showCertificateState(documents: List<Certificate>, searchQrCode: Boolean) {
        binding.progressIndicator.isVisible = false
        binding.authenticate.isVisible = false
        binding.addButton.isVisible = false
        val items = documents.map {
            CertificateItem(
                requireContext().applicationContext,
                fileName = it.id,
                documentName = it.name,
                searchQrCode = searchQrCode,
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
           delay(SCROLL_TO_LAST_DELAY_MS)
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

    private fun updateScreenBrightness(fullBrightness: Boolean) {
        requireActivity().window.apply {
            attributes.apply {
                screenBrightness = if (fullBrightness) {
                    LayoutParams.BRIGHTNESS_OVERRIDE_FULL
                } else {
                    LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                }
            }
            addFlags(LayoutParams.SCREEN_BRIGHTNESS_CHANGED)
        }
    }

    private inner class MyAuthenticationCallback : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            // onUserInteraction() is not called by android in this case so we call it manually
            requireActivity().onUserInteraction()
            vm.onAuthenticationSuccess()
        }
    }
}

private class MyInnerLayoutManager(context: Context) : LinearLayoutManager(context, RecyclerView.HORIZONTAL, false) {
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
