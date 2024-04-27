package com.michaeltroger.gruenerpass.certificates

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.michaeltroger.gruenerpass.AddFile
import com.michaeltroger.gruenerpass.R
import com.michaeltroger.gruenerpass.barcode.BarcodeRenderer
import com.michaeltroger.gruenerpass.certificates.dialogs.CertificateDialogs
import com.michaeltroger.gruenerpass.certificates.pager.item.CertificateItem
import com.michaeltroger.gruenerpass.certificates.sharing.PdfSharing
import com.michaeltroger.gruenerpass.certificates.states.ViewEvent
import com.michaeltroger.gruenerpass.certificates.states.ViewState
import com.michaeltroger.gruenerpass.databinding.FragmentCertificatesBinding
import com.michaeltroger.gruenerpass.db.Certificate
import com.xwray.groupie.GroupieAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import javax.inject.Inject

private const val TOUCH_SLOP_FACTOR = 8

@AndroidEntryPoint
class CertificatesFragment : Fragment(R.layout.fragment_certificates) {

    private val vm by viewModels<CertificatesViewModel>()

    @OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
    private val thread = newSingleThreadContext("RenderContext")

    private val adapter = GroupieAdapter()

    private var binding: FragmentCertificatesBinding? = null

    @Inject
    lateinit var pdfSharing: PdfSharing
    @Inject
    lateinit var certificateDialogs: CertificateDialogs
    @Inject
    lateinit var barcodeRenderer: BarcodeRenderer

    private lateinit var menuProvider: CertificatesMenuProvider

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        menuProvider = CertificatesMenuProvider(requireContext(), vm)
        requireActivity().addMenuProvider(menuProvider, viewLifecycleOwner, Lifecycle.State.RESUMED)

        binding = FragmentCertificatesBinding.bind(view)
        val binding = binding!!

        PagerSnapHelper().attachToRecyclerView(binding.certificates)

        try { // reduce scroll sensitivity for horizontal scrolling to improve vertical scrolling
            val touchSlopField = RecyclerView::class.java.getDeclaredField("mTouchSlop")
            touchSlopField.isAccessible = true
            val touchSlop = touchSlopField.get(binding.certificates) as Int
            touchSlopField.set(binding.certificates, touchSlop * TOUCH_SLOP_FACTOR)
        } catch (ignore: Exception) {}

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
                CertificatesFragmentDirections.actionGlobalSettingsFragment()
            )
            ViewEvent.ShowMoreScreen -> findNavController().navigate(
                CertificatesFragmentDirections.actionGlobalMoreFragment()
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

    private fun goToCertificate(event: ViewEvent.GoToCertificate) {
        lifecycleScope.launch {
            delay(event.delayMs)
            binding!!.certificates.smoothScrollToPosition(event.position)
        }
    }

    private fun showFileCanNotBeReadError() {
        binding!!.root.let {
            Snackbar.make(it, R.string.error_reading_pdf, Snackbar.LENGTH_LONG).show()
        }
    }
}
