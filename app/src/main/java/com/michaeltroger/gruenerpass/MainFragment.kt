package com.michaeltroger.gruenerpass

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.WindowManager.LayoutParams
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputLayout
import com.michaeltroger.gruenerpass.authentication.BiometricAuthenticationCallback
import com.michaeltroger.gruenerpass.databinding.FragmentMainBinding
import com.michaeltroger.gruenerpass.db.Certificate
import com.michaeltroger.gruenerpass.extensions.getUri
import com.michaeltroger.gruenerpass.locator.Locator
import com.michaeltroger.gruenerpass.more.MoreActivity
import com.michaeltroger.gruenerpass.pager.certificates.CertificateAdapter
import com.michaeltroger.gruenerpass.pager.certificates.CertificateItem
import com.michaeltroger.gruenerpass.pager.certificates.CertificateLinearLayoutManager
import com.michaeltroger.gruenerpass.pager.certificates.ItemTouchHelperCallback
import com.michaeltroger.gruenerpass.search.SearchQueryTextListener
import com.michaeltroger.gruenerpass.settings.SettingsActivity
import com.michaeltroger.gruenerpass.states.ViewEvent
import com.michaeltroger.gruenerpass.states.ViewState
import java.io.File
import java.util.concurrent.Executor
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext

private const val TOUCH_SLOP_FACTOR = 8
private const val SCROLL_TO_DELAY_MS = 1000L
private const val PDF_MIME_TYPE = "application/pdf"

@Suppress("TooManyFunctions")
class MainFragment : Fragment(R.layout.fragment_main), MenuProvider {

    private var searchView: SearchView? = null
    private var menu: Menu? = null
    private val vm by activityViewModels<MainViewModel> { MainViewModelFactory(app = requireActivity().application) }

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
            result.data?.let { intent ->
                lifecycleScope.launch {
                    val uri = intent.getUri() ?: return@launch
                    val file = Locator.fileRepo(requireContext()).copyToApp(uri)
                    vm.setPendingFile(file)
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)

        binding = FragmentMainBinding.bind(view)
        executor = ContextCompat.getMainExecutor(requireContext())
        biometricPrompt = BiometricPrompt(
            this,
            executor,
            BiometricAuthenticationCallback(
                onSuccess = {
                    requireActivity().onUserInteraction()
                    vm.onAuthenticationSuccess()
                },
                onError = vm::deletePendingFileIfExists
            )
        )

        promptInfo = Locator.biometricPromptInfo(requireContext())

        PagerSnapHelper().attachToRecyclerView(binding.certificates)
        binding.certificates.layoutManager = CertificateLinearLayoutManager(requireContext())
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
                    updateState(it)
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
                        ViewEvent.ScrollToLastCertificate -> scrollToLastCertificate()
                        ViewEvent.ScrollToFirstCertificate -> scrollToFirstCertificate()
                    }
                }
            }
        }
    }

    private fun updateState(state: ViewState) {
        updateMenuState(state)
        updateScreenBrightness(fullBrightness = state.fullBrightness)
        updateShowOnLockedScreen(showOnLockedScreen = state.showOnLockedScreen)
        binding.addButton.isVisible = state.showAddButton
        binding.authenticate.isVisible = state.showAuthenticateButton
        when (state) {
            is ViewState.Initial -> {} // nothing to do
            is ViewState.Empty -> {
                adapter.clear()
            }

            is ViewState.Locked -> {
                adapter.clear()
                biometricPrompt.authenticate(promptInfo)
            }

            is ViewState.Normal -> showCertificateState(
                documents = state.documents,
                searchQrCode = state.searchQrCode,
            )
        }
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.menu, menu)
        this.menu = menu

        val searchMenuItem = menu.findItem(R.id.search)
        searchView = searchMenuItem.actionView as SearchView
        searchView?.queryHint = requireContext().getString(R.string.search_query_hint)
        if (vm.filter.isNotEmpty()) {
            searchMenuItem.expandActionView()
            searchView?.setQuery(vm.filter, false)
            searchView?.clearFocus()
        }
        searchView?.setOnQueryTextListener(SearchQueryTextListener {
            vm.onSearchQueryChanged(it)
        })

        updateMenuState(vm.viewState.value)
    }

    override fun onPause() {
        super.onPause()
        searchView?.setOnQueryTextListener(null) // avoids an empty string to be sent
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

        R.id.deleteAll -> {
            showDoYouWantToDeleteAllDialog()
            true
        }

        R.id.lock -> {
            vm.lockApp()
            true
        }

        R.id.export_all -> {
            openShareAllFilePicker()
            true
        }

        R.id.scrollToFirst -> {
            scrollToFirstCertificate(delayMs = 0)
            true
        }

        R.id.scrollToLast -> {
            scrollToLastCertificate(delayMs = 0)
            true
        }

        else -> false
    }

    private fun updateMenuState(state: ViewState) {
        menu?.apply {
            findItem(R.id.add)?.isVisible = state.showAddMenuItem
            findItem(R.id.openSettings)?.isVisible = state.showSettingsMenuItem
            findItem(R.id.deleteAll)?.isVisible = state.showDeleteAllMenuItem
            findItem(R.id.lock)?.isVisible = state.showLockMenuItem
            findItem(R.id.export_all)?.isVisible = state.showExportAllMenuItem
            findItem(R.id.scrollToFirst)?.isVisible = state.showScrollToFirstMenuItem
            findItem(R.id.scrollToLast)?.isVisible = state.showScrollToLastMenuItem
        }
    }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = PDF_MIME_TYPE
        }
        resultLauncher.launch(intent)
    }

    private fun openShareAllFilePicker() {
        val state = vm.viewState.value as? ViewState.Normal ?: return
        val pdfUris = state.documents.map { certificate ->
            FileProvider.getUriForFile(
                requireContext(),
                getString(R.string.pdf_file_provider_authority),
                File(requireContext().filesDir, certificate.id),
                "${certificate.name}.pdf"
            )
        }

        val shareIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(pdfUris))
            type = PDF_MIME_TYPE
        }

        startActivity(Intent.createChooser(shareIntent, null))
    }

    private fun openShareFilePicker(certificate: Certificate) {
        val pdfUri = FileProvider.getUriForFile(
            requireContext(),
            getString(R.string.pdf_file_provider_authority),
            File(requireContext().filesDir, certificate.id),
            "${certificate.name}.pdf"
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_STREAM, pdfUri)
            type = PDF_MIME_TYPE
        }

        startActivity(Intent.createChooser(shareIntent, null))
    }

    private fun showCertificateState(documents: List<Certificate>, searchQrCode: Boolean) {
        val items = documents.map {
            CertificateItem(
                requireContext().applicationContext,
                fileName = it.id,
                documentName = it.name,
                searchQrCode = searchQrCode,
                dispatcher = thread,
                onDeleteCalled = { showDoYouWantToDeleteDialog(it.id) },
                onDocumentNameChanged = { updatedDocumentName: String ->
                    vm.onDocumentNameChanged(
                        filename = it.id,
                        documentName = updatedDocumentName
                    )
                },
                onStartDrag = { viewHolder -> itemTouchHelper?.startDrag(viewHolder) },
                onShareCalled = { openShareFilePicker(it) },
            )
        }
        adapter.setData(documents.map { it.id }.toList())
        adapter.update(items)
    }

    private fun scrollToLastCertificate(delayMs: Long = SCROLL_TO_DELAY_MS) {
        lifecycleScope.launch {
            delay(delayMs)
            binding.certificates.smoothScrollToPosition(adapter.itemCount - 1)
        }
    }

    private fun scrollToFirstCertificate(delayMs: Long = SCROLL_TO_DELAY_MS) {
        lifecycleScope.launch {
            delay(delayMs)
            binding.certificates.smoothScrollToPosition(0)
        }
    }

    private fun showDoYouWantToDeleteAllDialog() {
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setMessage(getString(R.string.dialog_delete_all_confirmation_message))
            .setPositiveButton(R.string.ok) { _, _ ->
                vm.onDeleteAllConfirmed()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .create()
        dialogs["delete_all"] = dialog
        dialog.show()
    }

    private fun showDoYouWantToDeleteDialog(id: String) {
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setMessage(getString(R.string.dialog_delete_confirmation_message))
            .setPositiveButton(R.string.ok) { _, _ ->
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
            .setPositiveButton(R.string.ok) { _, _ ->
                vm.onPasswordEntered(passwordTextField.editText!!.text.toString())
            }
            .setNegativeButton(getString(R.string.cancel)) { _, _ ->
                vm.deletePendingFileIfExists()
            }
            .setOnCancelListener {
                vm.deletePendingFileIfExists()
            }
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

    private fun updateShowOnLockedScreen(showOnLockedScreen: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            requireActivity().setShowWhenLocked(showOnLockedScreen)
        }
    }
}
