package com.michaeltroger.gruenerpass

import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricPrompt
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
import com.michaeltroger.gruenerpass.barcode.BarcodeRenderer
import com.michaeltroger.gruenerpass.databinding.FragmentMainBinding
import com.michaeltroger.gruenerpass.db.Certificate
import com.michaeltroger.gruenerpass.dialogs.CertificateDialogs
import com.michaeltroger.gruenerpass.pager.certificates.CertificateItem
import com.michaeltroger.gruenerpass.search.SearchQueryTextListener
import com.michaeltroger.gruenerpass.sharing.PdfSharing
import com.michaeltroger.gruenerpass.states.ViewEvent
import com.michaeltroger.gruenerpass.states.ViewState
import com.xwray.groupie.GroupieAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import javax.inject.Inject

private const val TOUCH_SLOP_FACTOR = 8
private const val PDF_MIME_TYPE = "application/pdf"
private const val DELAY_AUTHENTICATION_PROMPT_MS = 1000L

@Suppress("TooManyFunctions")
@AndroidEntryPoint
class MainFragment : Fragment(R.layout.fragment_main) {

    private val vm by activityViewModels<MainViewModel>()

    @OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
    private val thread = newSingleThreadContext("RenderContext")

    private val adapter = GroupieAdapter()

    private var binding: FragmentMainBinding? = null

    @Inject
    lateinit var pdfSharing: PdfSharing
    @Inject
    lateinit var certificateDialogs: CertificateDialogs
    @Inject
    lateinit var barcodeRenderer: BarcodeRenderer
    @Inject
    lateinit var biometricPromptInfo: BiometricPrompt.PromptInfo

    private lateinit var menuProvider: MainMenuProvider

    private val documentPick = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@registerForActivityResult
        vm.setPendingFile(uri)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        menuProvider = MainMenuProvider(requireContext(), vm)
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
            authenticate(delayMs = 0)
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

            ViewEvent.ShowParsingFileError -> showFileCanNotBeReadError()
            is ViewEvent.ScrollToLastCertificate -> scrollToLastCertificate(it.delayMs)
            is ViewEvent.ScrollToFirstCertificate -> scrollToFirstCertificate(it.delayMs)
            is ViewEvent.ShareMultiple -> {
                pdfSharing.openShareAllFilePicker(
                    context = requireContext(),
                    certificates = it.list,
                )
            }
            ViewEvent.ShowDeleteAllDialog -> {
                certificateDialogs.showDoYouWantToDeleteAllDialog(
                    context = requireContext(),
                    onDeleteAllConfirmed = vm::onDeleteAllConfirmed
                )
            }
            is ViewEvent.ShowDeleteFilteredDialog -> {
                certificateDialogs.showDoYouWantToDeleteFilteredDialog(
                    context = requireContext(),
                    onDeleteFilteredConfirmed = vm::onDeleteFilteredConfirmed,
                    documentCount = it.documentCountToBeDeleted
                )
            }
            is ViewEvent.ShowChangeDocumentOrderDialog -> {
                certificateDialogs.showChangeDocumentOrder(
                    context = requireContext(),
                    originalOrder = it.originalOrder,
                    onOrderChanged = vm::onOrderChangeConfirmed
                )
            }
            ViewEvent.ShowWarningDialog -> certificateDialogs.showWarningDialog(requireContext())
            ViewEvent.ShowSettingsScreen -> findNavController().navigate(R.id.navigate_to_settings)
            ViewEvent.ShowMoreScreen -> findNavController().navigate(R.id.navigate_to_more)
            ViewEvent.AddFile -> documentPick.launch(arrayOf(PDF_MIME_TYPE))
            is ViewEvent.ShowDeleteDialog -> {
                certificateDialogs.showDoYouWantToDeleteDialog(
                    context = requireContext(),
                    id = it.id,
                    onDeleteConfirmed = vm::onDeleteConfirmed
                )
            }
            is ViewEvent.ShowChangeDocumentNameDialog -> {
                certificateDialogs.showChangeDocumentNameDialog(
                    context = requireContext(),
                    originalDocumentName = it.originalName,
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

    private fun authenticate(delayMs: Long = DELAY_AUTHENTICATION_PROMPT_MS) {
        lifecycleScope.launch {
            delay(delayMs)
            BiometricPrompt(
                this@MainFragment,
                MyAuthenticationCallback()
            ).authenticate(biometricPromptInfo)
        }
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
                barcodeRenderer = barcodeRenderer,
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
