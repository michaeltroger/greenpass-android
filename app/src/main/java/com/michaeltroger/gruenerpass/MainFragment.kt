package com.michaeltroger.gruenerpass

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
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
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.michaeltroger.gruenerpass.databinding.FragmentMainBinding
import com.michaeltroger.gruenerpass.db.Certificate
import com.michaeltroger.gruenerpass.locator.Locator
import com.michaeltroger.gruenerpass.pager.certificates.CertificateItem
import com.michaeltroger.gruenerpass.search.SearchQueryTextListener
import com.michaeltroger.gruenerpass.states.ViewEvent
import com.michaeltroger.gruenerpass.states.ViewState
import com.xwray.groupie.GroupieAdapter
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext

private const val TOUCH_SLOP_FACTOR = 8
private const val PDF_MIME_TYPE = "application/pdf"

@Suppress("TooManyFunctions")
class MainFragment : Fragment(R.layout.fragment_main) {

    private val vm by activityViewModels<MainViewModel> { MainViewModelFactory(app = requireActivity().application) }

    @OptIn(DelicateCoroutinesApi::class)
    private val thread = newSingleThreadContext("RenderContext")

    private val adapter = GroupieAdapter()

    private var binding: FragmentMainBinding? = null

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
        val binding = binding!!

        PagerSnapHelper().attachToRecyclerView(binding.certificates)

        try { // reduce scroll sensitivity for horizontal scrolling to improve vertical scrolling
            val touchSlopField = RecyclerView::class.java.getDeclaredField("mTouchSlop")
            touchSlopField.isAccessible = true
            val touchSlop = touchSlopField.get(binding.certificates) as Int
            touchSlopField.set(binding.certificates, touchSlop * TOUCH_SLOP_FACTOR)
        } catch (ignore: Exception) {}

        binding.certificates.adapter = adapter

        binding.authenticate.setOnClickListener {
            authenticate()
        }

        binding.addButton.setOnClickListener {
            vm.onAddFileSelected()
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
                    handleEvent(it)
                }
            }
        }
    }

    @Suppress("LongMethod", "CyclomaticComplexMethod")
    private fun handleEvent(it: ViewEvent) {
        when (it) {
            ViewEvent.CloseAllDialogs -> certificateDialogs.closeAllDialogs()
            ViewEvent.ShowPasswordDialog -> certificateDialogs.showEnterPasswordDialog(
                context = requireContext(),
                onPasswordEntered = vm::onPasswordEntered,
                onCancelled = vm::deletePendingFileIfExists
            )

            ViewEvent.ErrorParsingFile -> showFileCanNotBeReadError()
            is ViewEvent.ScrollToLastCertificate -> scrollToLastCertificate(it.delayMs)
            is ViewEvent.ScrollToFirstCertificate -> scrollToFirstCertificate(it.delayMs)
            is ViewEvent.ExportAll -> {
                pdfSharing.openShareAllFilePicker(
                    context = requireContext(),
                    certificates = it.list,
                )
            }
            is ViewEvent.ExportFiltered -> {
                pdfSharing.openShareAllFilePicker(
                    context = requireContext(),
                    certificates = it.list,
                )
            }
            ViewEvent.DeleteAll -> {
                certificateDialogs.showDoYouWantToDeleteAllDialog(
                    context = requireContext(),
                    onDeleteAllConfirmed = vm::onDeleteAllConfirmed
                )
            }
            is ViewEvent.DeleteFiltered -> {
                certificateDialogs.showDoYouWantToDeleteFilteredDialog(
                    context = requireContext(),
                    onDeleteFilteredConfirmed = vm::onDeleteFilteredConfirmed,
                    documentCount = it.documentCount
                )
            }
            is ViewEvent.ChangeDocumentOrder -> {
                certificateDialogs.showChangeDocumentOrder(
                    context = requireContext(),
                    originalOrder = it.originalOrder,
                    onOrderChanged = vm::onOrderChangeConfirmed
                )
            }
            ViewEvent.ShowWarningDialog -> certificateDialogs.showWarningDialog(requireContext())
            ViewEvent.ShowSettings -> findNavController().navigate(R.id.navigate_to_settings)
            ViewEvent.ShowMore -> findNavController().navigate(R.id.navigate_to_more)
            ViewEvent.AddFile -> documentPick.launch(arrayOf(PDF_MIME_TYPE))
            is ViewEvent.ShowDoYouWantToDeleteDialog -> {
                certificateDialogs.showDoYouWantToDeleteDialog(
                    context = requireContext(),
                    id = it.id,
                    onDeleteConfirmed = vm::onDeleteConfirmed
                )
            }
            is ViewEvent.ChangeDocumentName -> {
                certificateDialogs.showChangeDocumentNameDialog(
                    context = requireContext(),
                    originalDocumentName = it.name,
                    onDocumentNameChanged = { newName ->
                        vm.onDocumentNameChangeConfirmed(documentName = newName, filename = it.id)
                    }
                )

            }
            is ViewEvent.Share -> {
                pdfSharing.openShareFilePicker(
                    context = requireContext(),
                    certificate = it.certificate,
                )
            }
        }
    }

    override fun onDestroyView() {
        binding!!.certificates.adapter = null
        binding = null
        super.onDestroyView()
    }

    private fun authenticate() {
        BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(requireContext()),
            MyAuthenticationCallback()
        ).authenticate(Locator.biometricPromptInfo(requireContext()))
    }

    private fun updateState(state: ViewState) {
        menuProvider.updateMenuState(state)
        binding!!.addButton.isVisible = state.showAddButton
        binding!!.authenticate.isVisible = state.showAuthenticateButton
        when (state) {
            is ViewState.Initial -> {} // nothing to do
            is ViewState.Empty -> {
                adapter.clear()
            }

            is ViewState.Locked -> {
                adapter.clear()
                authenticate()
            }

            is ViewState.Normal -> showCertificateState(
                documents = state.documents,
                searchBarcode = state.searchBarcode,
            )
        }
    }

    override fun onPause() {
        super.onPause()
        menuProvider.onPause()
    }

    private fun showCertificateState(documents: List<Certificate>, searchBarcode: Boolean) {
        val items = documents.map { certificate ->
            CertificateItem(
                requireContext().applicationContext,
                fileName = certificate.id,
                documentName = certificate.name,
                searchBarcode = searchBarcode,
                dispatcher = thread,
                onDeleteCalled = {
                    vm.onDeleteCalled(certificate.id)
                },
                onDocumentNameClicked = {
                    vm.onChangeDocumentNameSelected(certificate.id, certificate.name)
                },
                onShareCalled = {
                    vm.onShareSelected(certificate)
                },
            )
        }
        adapter.update(items)
    }

    private fun scrollToLastCertificate(delayMs: Long) {
        lifecycleScope.launch {
            delay(delayMs)
            binding!!.certificates.smoothScrollToPosition(adapter.itemCount - 1)
        }
    }

    private fun scrollToFirstCertificate(delayMs: Long) {
        lifecycleScope.launch {
            delay(delayMs)
            binding!!.certificates.smoothScrollToPosition(0)
        }
    }

    private fun showFileCanNotBeReadError() {
        binding!!.root.let {
            Snackbar.make(it, R.string.error_reading_pdf, Snackbar.LENGTH_LONG).show()
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
                vm.onAddFileSelected()
                true
            }

            R.id.warning -> {
                vm.onShowWarningDialogSelected()
                true
            }

            R.id.openMore -> {
                vm.onShowMoreSelected()
                true
            }

            R.id.openSettings -> {
                vm.onShowSettingsSelected()
                true
            }

            R.id.deleteFiltered -> {
                vm.onDeleteFilteredSelected()
                true
            }

            R.id.deleteAll -> {
                vm.onDeleteAllSelected()
                true
            }

            R.id.lock -> {
                vm.lockApp()
                true
            }

            R.id.export_filtered -> {
                vm.onExportFilteredSelected()
                true
            }

            R.id.export_all -> {
                vm.onExportAllSelected()
                true
            }

            R.id.scrollToFirst -> {
                vm.onScrollToFirstSelected()
                true
            }

            R.id.scrollToLast -> {
                vm.onScrollToLastSelected()
                true
            }

            R.id.changeOrder -> {
                vm.onChangeOrderSelected()
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
                findItem(R.id.warning)?.isVisible = state.showWarningButton
                findItem(R.id.openSettings)?.isVisible = state.showSettingsMenuItem
                findItem(R.id.deleteAll)?.isVisible = state.showDeleteAllMenuItem
                findItem(R.id.deleteFiltered)?.isVisible = state.showDeleteFilteredMenuItem
                findItem(R.id.lock)?.isVisible = state.showLockMenuItem
                findItem(R.id.export_all)?.isVisible = state.showExportAllMenuItem
                findItem(R.id.export_filtered)?.isVisible = state.showExportFilteredMenuItem
                findItem(R.id.changeOrder)?.isVisible = state.showChangeOrderMenuItem
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
