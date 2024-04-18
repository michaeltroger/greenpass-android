package com.michaeltroger.gruenerpass.certificateslist

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.michaeltroger.gruenerpass.AddFile
import com.michaeltroger.gruenerpass.R
import com.michaeltroger.gruenerpass.certificates.CertificatesMenuProvider
import com.michaeltroger.gruenerpass.certificates.CertificatesViewModel
import com.michaeltroger.gruenerpass.certificates.dialogs.CertificateDialogs
import com.michaeltroger.gruenerpass.certificates.sharing.PdfSharing
import com.michaeltroger.gruenerpass.certificates.states.ViewEvent
import com.michaeltroger.gruenerpass.certificates.states.ViewState
import com.michaeltroger.gruenerpass.certificateslist.pager.item.CertificateListItem
import com.michaeltroger.gruenerpass.databinding.FragmentCertificatesListBinding
import com.michaeltroger.gruenerpass.db.Certificate
import com.xwray.groupie.GroupieAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class CertificatesListFragment : Fragment(R.layout.fragment_certificates_list) {

    private val vm by viewModels<CertificatesViewModel>()

    private val adapter = GroupieAdapter()

    private var binding: FragmentCertificatesListBinding? = null

    @Inject
    lateinit var pdfSharing: PdfSharing
    @Inject
    lateinit var certificateDialogs: CertificateDialogs

    private lateinit var menuProvider: CertificatesMenuProvider

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        menuProvider = CertificatesMenuProvider(requireContext(), vm)
        requireActivity().addMenuProvider(menuProvider, viewLifecycleOwner, Lifecycle.State.RESUMED)

        binding = FragmentCertificatesListBinding.bind(view)
        val binding = binding!!

        binding.certificates.adapter = adapter

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
                onCancelled = vm::onPasswordDialogAborted
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
                    scope = lifecycleScope,
                    originalOrder = it.originalOrder,
                    onOrderChanged = vm::onOrderChangeConfirmed
                )
            }
            ViewEvent.ShowWarningDialog -> certificateDialogs.showWarningDialog(requireContext())
            ViewEvent.ShowSettingsScreen -> findNavController().navigate(R.id.navigate_to_settings)
            ViewEvent.ShowMoreScreen -> findNavController().navigate(R.id.navigate_to_more)
            ViewEvent.AddFile -> (requireActivity() as? AddFile)?.addFile()
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

    private fun updateState(state: ViewState) {
        menuProvider.updateMenuState(state)
        binding!!.addButton.isVisible = state.showAddButton
        when (state) {
            is ViewState.Initial -> {} // nothing to do
            is ViewState.Empty -> {
                adapter.clear()
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
            CertificateListItem(
                fileName = certificate.id,
                documentName = certificate.name,
                searchBarcode = searchBarcode,
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
}