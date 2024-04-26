package com.michaeltroger.gruenerpass.certificatedetails

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.michaeltroger.gruenerpass.R
import com.michaeltroger.gruenerpass.barcode.BarcodeRenderer
import com.michaeltroger.gruenerpass.certificatedetails.states.DetailsViewState
import com.michaeltroger.gruenerpass.certificates.dialogs.CertificateDialogs
import com.michaeltroger.gruenerpass.certificates.pager.item.CertificateItem
import com.michaeltroger.gruenerpass.certificates.sharing.PdfSharing
import com.michaeltroger.gruenerpass.certificates.states.ViewEvent
import com.michaeltroger.gruenerpass.databinding.FragmentCertificateDetailsBinding
import com.michaeltroger.gruenerpass.db.Certificate
import com.xwray.groupie.GroupieAdapter
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext

@AndroidEntryPoint
class CertificateDetailsFragment : Fragment(R.layout.fragment_certificate_details) {

    private val vm by viewModels<CertificateDetailsViewModel>()

    @OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
    private val thread = newSingleThreadContext("RenderContext")

    private val adapter = GroupieAdapter()

    private var binding: FragmentCertificateDetailsBinding? = null

    @Inject
    lateinit var pdfSharing: PdfSharing
    @Inject
    lateinit var certificateDialogs: CertificateDialogs
    @Inject
    lateinit var barcodeRenderer: BarcodeRenderer

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentCertificateDetailsBinding.bind(view)
        val binding = binding!!

        binding.certificateFullscreen.layoutManager = object : LinearLayoutManager(
            requireContext(),
            RecyclerView.HORIZONTAL,
            false,
        ) {
            override fun canScrollVertically(): Boolean {
                return false
            }

            override fun canScrollHorizontally(): Boolean {
                return false
            }
        }
        binding.certificateFullscreen.adapter = adapter

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
            else -> {
                // do nothing
            }
        }
    }

    override fun onDestroyView() {
        binding!!.certificateFullscreen.adapter = null
        binding = null
        super.onDestroyView()
    }

    private fun updateState(state: DetailsViewState) {
        when (state) {
            is DetailsViewState.Normal -> showCertificateState(
                certificate = state.document,
                searchBarcode = state.searchBarcode,
            )
            is DetailsViewState.Deleted -> {
                findNavController().popBackStack() // todo check: adapter.clear()
            }
            DetailsViewState.Initial -> {
                // nothing to do
            }
        }
    }

    private fun showCertificateState(certificate: Certificate, searchBarcode: Boolean) {
        val item = CertificateItem(
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

        adapter.update(listOf(item))
    }
}
