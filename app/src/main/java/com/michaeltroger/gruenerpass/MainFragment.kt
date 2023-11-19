package com.michaeltroger.gruenerpass

import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.WindowManager.LayoutParams
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.SearchView
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.michaeltroger.gruenerpass.databinding.FragmentMainBinding
import com.michaeltroger.gruenerpass.db.Certificate
import com.michaeltroger.gruenerpass.locator.Locator
import com.michaeltroger.gruenerpass.pager.certificates.CertificateAdapter
import com.michaeltroger.gruenerpass.pager.certificates.CertificateItem
import com.michaeltroger.gruenerpass.pager.certificates.ItemTouchHelperCallback
import com.michaeltroger.gruenerpass.search.SearchQueryTextListener
import com.michaeltroger.gruenerpass.states.ViewEvent
import com.michaeltroger.gruenerpass.states.ViewState
import java.util.concurrent.Executor
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext

private const val TOUCH_SLOP_FACTOR = 8
private const val SCROLL_TO_DELAY_MS = 1000L
private const val PDF_MIME_TYPE = "application/pdf"

@Suppress("TooManyFunctions")
class MainFragment : Fragment(R.layout.fragment_main) {

    private val vm by activityViewModels<MainViewModel> { MainViewModelFactory(app = requireActivity().application) }

    @OptIn(DelicateCoroutinesApi::class)
    private val thread = newSingleThreadContext("RenderContext")

    private val adapter = CertificateAdapter()
    private var itemTouchHelper: ItemTouchHelper? = null

    private lateinit var binding: FragmentMainBinding

    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo

    private val pdfSharing = Locator.pdfSharing()
    private val certificateDialogs = Locator.certificateDialogs()

    private val menuProvider = MainMenuProvider()

    private val documentPick = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@registerForActivityResult
        vm.setPendingFile(uri)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().addMenuProvider(menuProvider, viewLifecycleOwner, Lifecycle.State.RESUMED)

        binding = FragmentMainBinding.bind(view)
        executor = ContextCompat.getMainExecutor(requireContext())
        biometricPrompt = BiometricPrompt(
            this,
            executor,
            MyAuthenticationCallback()
        )

        promptInfo = Locator.biometricPromptInfo(requireContext())

        PagerSnapHelper().attachToRecyclerView(binding.certificates)
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
                        ViewEvent.CloseAllDialogs -> certificateDialogs.closeAllDialogs()
                        ViewEvent.ShowPasswordDialog -> certificateDialogs.showEnterPasswordDialog(
                            context = requireContext(),
                            onPasswordEntered = vm::onPasswordEntered,
                            onCancelled = vm::deletePendingFileIfExists
                        )
                        ViewEvent.ErrorParsingFile -> showFileCanNotBeReadError()
                        ViewEvent.ScrollToLastCertificate -> scrollToLastCertificate()
                        ViewEvent.ScrollToFirstCertificate -> scrollToFirstCertificate()
                    }
                }
            }
        }
    }

    private fun updateState(state: ViewState) {
        menuProvider.updateMenuState(state)
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
                showDragButtons = state.showDragButtons
            )
        }
    }

    override fun onPause() {
        super.onPause()
        menuProvider.onPause()
    }

    private fun openFilePicker() {
        documentPick.launch(arrayOf(PDF_MIME_TYPE))
    }

    private fun showCertificateState(documents: List<Certificate>, searchQrCode: Boolean, showDragButtons: Boolean) {
        val items = documents.map {
            CertificateItem(
                requireContext().applicationContext,
                fileName = it.id,
                documentName = it.name,
                searchQrCode = searchQrCode,
                showDragButtons = showDragButtons,
                dispatcher = thread,
                onDeleteCalled = { certificateDialogs.showDoYouWantToDeleteDialog(
                    context = requireContext(),
                    id = it.id,
                    onDeleteConfirmed = vm::onDeleteConfirmed
                ) },
                onDocumentNameChanged = { updatedDocumentName: String ->
                    vm.onDocumentNameChanged(
                        filename = it.id,
                        documentName = updatedDocumentName
                    )
                },
                onStartDrag = { viewHolder -> itemTouchHelper?.startDrag(viewHolder) },
                onShareCalled = {
                    pdfSharing.openShareFilePicker(
                        context = requireContext(),
                        certificate = it,
                    )
                },
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

    private fun showFileCanNotBeReadError() {
        binding.root.let {
            Snackbar.make(it, R.string.error_reading_pdf, Snackbar.LENGTH_LONG).show()
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

    private inner class MainMenuProvider : MenuProvider {
        private var searchView: SearchView? = null
        private var menu: Menu? = null

        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
            menuInflater.inflate(R.menu.menu, menu)
            this.menu = menu

            val searchMenuItem = menu.findItem(R.id.search)
            searchView = searchMenuItem.actionView as SearchView
            searchView?.queryHint = requireContext().getString(R.string.search_query_hint)
            restorePendingSearchQueryFilter(searchMenuItem)
            searchView?.setOnQueryTextListener(SearchQueryTextListener {
                vm.onSearchQueryChanged(it)
            })

            updateMenuState(vm.viewState.value)
        }

        override fun onMenuItemSelected(menuItem: MenuItem): Boolean = when (menuItem.itemId) {
            R.id.add -> {
                openFilePicker()
                true
            }

            R.id.openMore -> {
                findNavController().navigate(R.id.navigate_to_more)
                true
            }

            R.id.openSettings -> {
                findNavController().navigate(R.id.navigate_to_settings)
                true
            }

            R.id.deleteAll -> {
                certificateDialogs.showDoYouWantToDeleteAllDialog(
                    context = requireContext(),
                    onDeleteAllConfirmed = vm::onDeleteAllConfirmed
                )
                true
            }

            R.id.lock -> {
                vm.lockApp()
                true
            }

            R.id.export_all -> {
                (vm.viewState.value as? ViewState.Normal)?.documents?.let {
                    pdfSharing.openShareAllFilePicker(
                        context = requireContext(),
                        certificates = it,
                    )
                }
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

        fun onPause() {
            searchView?.setOnQueryTextListener(null) // avoids an empty string to be sent
        }

        private fun restorePendingSearchQueryFilter(searchMenuItem: MenuItem) {
            val pendingFilter = (vm.viewState.value as? ViewState.Normal)?.filter ?: return
            if (pendingFilter.isNotEmpty()) {
                searchMenuItem.expandActionView()
                searchView?.setQuery(pendingFilter, false)
                searchView?.clearFocus()
            }
        }

        fun updateMenuState(state: ViewState) {
            menu?.apply {
                findItem(R.id.add)?.isVisible = state.showAddMenuItem
                findItem(R.id.openSettings)?.isVisible = state.showSettingsMenuItem
                findItem(R.id.deleteAll)?.isVisible = state.showDeleteAllMenuItem
                findItem(R.id.lock)?.isVisible = state.showLockMenuItem
                findItem(R.id.export_all)?.isVisible = state.showExportAllMenuItem
                findItem(R.id.scrollToFirst)?.isVisible = state.showScrollToFirstMenuItem
                findItem(R.id.scrollToLast)?.isVisible = state.showScrollToLastMenuItem
                findItem(R.id.search)?.apply {
                    isVisible = state.showSearchMenuItem
                    if (!state.showSearchMenuItem) {
                        collapseActionView()
                    }
                }
                findItem(R.id.openMore)?.isVisible = state.showMoreMenuItem
            }
        }
    }

    private inner class MyAuthenticationCallback : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            requireActivity().onUserInteraction()
            vm.onAuthenticationSuccess()
        }

        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            vm.deletePendingFileIfExists()
        }
    }
}
