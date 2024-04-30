package com.michaeltroger.gruenerpass.certificateslist

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.withStarted
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import com.michaeltroger.gruenerpass.AddFile
import com.michaeltroger.gruenerpass.R
import com.michaeltroger.gruenerpass.certificates.CertificatesMenuProvider
import com.michaeltroger.gruenerpass.certificates.CertificatesViewModel
import com.michaeltroger.gruenerpass.certificates.dialogs.CertificateDialogs
import com.michaeltroger.gruenerpass.certificates.dialogs.CertificateErrors
import com.michaeltroger.gruenerpass.certificates.sharing.PdfSharing
import com.michaeltroger.gruenerpass.certificates.states.ViewEvent
import com.michaeltroger.gruenerpass.certificates.states.ViewState
import com.michaeltroger.gruenerpass.certificateslist.pager.item.CertificateListItem
import com.michaeltroger.gruenerpass.databinding.FragmentCertificatesListBinding
import com.michaeltroger.gruenerpass.db.Certificate
import com.xwray.groupie.GroupieAdapter
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CertificatesListFragment : Fragment(R.layout.fragment_certificates_list) {

    private val vm by viewModels<CertificatesViewModel>()

    private val adapter = GroupieAdapter()

    private var binding: FragmentCertificatesListBinding? = null

    @Inject
    lateinit var pdfSharing: PdfSharing
    @Inject
    lateinit var certificateDialogs: CertificateDialogs
    @Inject
    lateinit var certificateErrors: CertificateErrors

    private lateinit var menuProvider: CertificatesMenuProvider

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        menuProvider = CertificatesMenuProvider(requireContext(), vm, forceHiddenScrollButtons = true)
        requireActivity().addMenuProvider(menuProvider, viewLifecycleOwner, Lifecycle.State.RESUMED)

        binding = FragmentCertificatesListBinding.bind(view)
        val binding = binding!!

        binding.certificates.adapter = adapter

        binding.certificates.addItemDecoration(
            DividerItemDecoration(
                requireContext(),
                DividerItemDecoration.VERTICAL
            )
        )

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
            ViewEvent.ShowParsingFileError -> {
                binding?.root?.let {
                    certificateErrors.showFileErrorSnackbar(it)
                }
            }
            is ViewEvent.GoToCertificate -> goToCertificate(it)
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
            ViewEvent.ShowSettingsScreen -> findNavController().navigate(
                CertificatesListFragmentDirections.actionGlobalSettingsFragment()
            )
            ViewEvent.ShowMoreScreen -> findNavController().navigate(
                CertificatesListFragmentDirections.actionGlobalMoreFragment()
            )
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
        binding?.certificates?.adapter = null
        binding = null
        super.onDestroyView()
    }

    private fun updateState(state: ViewState) {
        menuProvider.updateMenuState(state)
        binding?.addButton?.isVisible = state.showAddButton
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
                onChangeDocumentNameClicked = {
                    vm.onChangeDocumentNameSelected(certificate.id, certificate.name)
                },
                onOpenDetails = {
                    findNavController().navigate(
                        CertificatesListFragmentDirections.navigateToCertificateDetails(certificate.id)
                    )
                },
                onShareCalled = {
                    vm.onShareSelected(certificate)
                },
            )
        }
        adapter.update(items)
    }

    private fun goToCertificate(event: ViewEvent.GoToCertificate) {
        lifecycleScope.launch {
            withStarted {
                binding?.certificates?.scrollToPosition(event.position)
                findNavController().navigate(
                    CertificatesListFragmentDirections.navigateToCertificateDetails(event.id)
                )
            }
        }
    }
}
