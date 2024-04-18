package com.michaeltroger.gruenerpass.certificates.dialogs

import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import com.michaeltroger.gruenerpass.R
import com.michaeltroger.gruenerpass.db.Certificate
import com.michaeltroger.gruenerpass.certificates.documentorder.DocumentOrderItem
import com.xwray.groupie.GroupieAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

interface CertificateDialogs {
    fun closeAllDialogs()
    fun showEnterPasswordDialog(
        context: Context,
        onPasswordEntered: (String) -> Unit,
        onCancelled: () -> Unit
    )
    fun showDoYouWantToDeleteDialog(context: Context, id: String, onDeleteConfirmed: (String) -> Unit)
    fun showDoYouWantToDeleteAllDialog(context: Context, onDeleteAllConfirmed: () -> Unit)
    fun showDoYouWantToDeleteFilteredDialog(context: Context, documentCount: Int, onDeleteFilteredConfirmed: () -> Unit)
    fun showWarningDialog(context: Context)
    fun showChangeDocumentNameDialog(
        context: Context,
        originalDocumentName: String,
        onDocumentNameChanged: (String) -> Unit
    )
    fun showChangeDocumentOrder(
        context: Context,
        scope: CoroutineScope,
        originalOrder: List<Certificate>,
        onOrderChanged: (List<String>) -> Unit
    ): Job
}

class CertificateDialogsImpl @Inject constructor() : CertificateDialogs {

    private var dialog: Dialog? = null

    override fun showDoYouWantToDeleteAllDialog(context: Context, onDeleteAllConfirmed: () -> Unit) {
        val dialog = MaterialAlertDialogBuilder(context)
            .setMessage(R.string.dialog_delete_all_confirmation_message)
            .setPositiveButton(R.string.ok) { _, _ ->
                onDeleteAllConfirmed()
            }
            .setNegativeButton(R.string.cancel, null)
            .setOnDismissListener {
                this.dialog = null
            }
            .create()
        this.dialog = dialog
        dialog.show()
    }

    override fun showDoYouWantToDeleteFilteredDialog(
        context: Context,
        documentCount: Int,
        onDeleteFilteredConfirmed: () -> Unit
    ) {
        val dialog = MaterialAlertDialogBuilder(context)
            .setMessage(context.resources.getQuantityString(
                R.plurals.dialog_delete_filtered_confirmation_text,
                documentCount,
                documentCount
            ))
            .setPositiveButton(R.string.ok) { _, _ ->
                onDeleteFilteredConfirmed()
            }
            .setNegativeButton(R.string.cancel, null)
            .setOnDismissListener {
                this.dialog = null
            }
            .create()
        this.dialog = dialog
        dialog.show()
    }

    override fun showWarningDialog(context: Context) {
        val dialog = MaterialAlertDialogBuilder(context)
            .setTitle(R.string.dialog_warning_title)
            .setMessage(R.string.dialog_warning_description)
            .setPositiveButton(R.string.ok, null)
            .setOnDismissListener {
                this.dialog = null
            }
            .create()
        this.dialog = dialog
        dialog.show()
    }

    override fun showDoYouWantToDeleteDialog(context: Context, id: String, onDeleteConfirmed: (String) -> Unit) {
        val dialog = MaterialAlertDialogBuilder(context)
            .setMessage(R.string.dialog_delete_confirmation_message)
            .setPositiveButton(R.string.ok) { _, _ ->
                onDeleteConfirmed(id)
            }
            .setNegativeButton(R.string.cancel, null)
            .setOnDismissListener {
                this.dialog = null
            }
            .create()
        this.dialog = dialog
        dialog.show()
    }

    override fun showChangeDocumentNameDialog(
        context: Context,
        originalDocumentName: String,
        onDocumentNameChanged: (String) -> Unit
    ) {
        val customAlertDialogView = LayoutInflater.from(context)
            .inflate(R.layout.layout_document_name_dialog, null, false)

        val textField = customAlertDialogView.findViewById<TextInputLayout>(R.id.document_name_text_field).apply {
            editText!!.setText(originalDocumentName)
        }

        val dialog = MaterialAlertDialogBuilder(context)
            .setTitle(R.string.dialog_document_name_title)
            .setView(customAlertDialogView)
            .setPositiveButton(R.string.ok) { _, _ ->
                onDocumentNameChanged(textField.editText!!.text.toString())
            }
            .setNegativeButton(R.string.cancel, null)
            .setOnDismissListener {
                this.dialog = null
            }
            .create()
        this.dialog = dialog
        dialog.show()
    }

    override fun showChangeDocumentOrder(
        context: Context,
        scope: CoroutineScope,
        originalOrder: List<Certificate>,
        onOrderChanged: (List<String>) -> Unit
    ) = scope.launch {
        val customAlertDialogView = LayoutInflater.from(context)
            .inflate(R.layout.layout_document_order_dialog, null, false)

        val myAdapter = GroupieAdapter()
        customAlertDialogView.findViewById<RecyclerView>(R.id.document_order).adapter = myAdapter
        val listFlow: MutableStateFlow<List<Certificate>> = MutableStateFlow(originalOrder)

        fun onUpClicked(id: String) {
            val index = listFlow.value.indexOfFirst {
                it.id == id
            }
            val newState = listFlow.value.toMutableList()
            newState[index] = listFlow.value.getOrNull(index - 1) ?: return
            newState[index - 1] = listFlow.value[index]
            listFlow.value = newState
        }

        fun onDownClicked(id: String) {
            val index = listFlow.value.indexOfFirst {
                it.id == id
            }
            val newState = listFlow.value.toMutableList()
            newState[index] = listFlow.value.getOrNull(index + 1) ?: return
            newState[index + 1] = listFlow.value[index]
            listFlow.value = newState
        }

        val dialog = MaterialAlertDialogBuilder(context)
            .setTitle(R.string.dialog_document_order_title)
            .setView(customAlertDialogView)
            .setPositiveButton(R.string.ok) { _, _ ->
                onOrderChanged(listFlow.value.map { it.id })
            }
            .setNegativeButton(R.string.cancel, null)
            .setOnDismissListener {
                this@CertificateDialogsImpl.dialog = null
            }
            .create()
        this@CertificateDialogsImpl.dialog = dialog
        dialog.show()

        listFlow.collect { list ->
            val items = list.map { certificate ->
                DocumentOrderItem(
                    fileName = certificate.id,
                    documentName = certificate.name,
                    onDownClicked = ::onDownClicked,
                    onUpClicked = ::onUpClicked
                )
            }
            myAdapter.update(items)
        }
    }

    override fun showEnterPasswordDialog(
        context: Context,
        onPasswordEntered: (String) -> Unit,
        onCancelled: () -> Unit,
    ) {
        val customAlertDialogView = LayoutInflater.from(context)
            .inflate(R.layout.layout_password_dialog, null, false)

        val passwordTextField = customAlertDialogView.findViewById<TextInputLayout>(R.id.password_text_field)

        val dialog = MaterialAlertDialogBuilder(context)
            .setTitle(R.string.dialog_password_protection_title)
            .setView(customAlertDialogView)
            .setPositiveButton(R.string.ok) { _, _ ->
                onPasswordEntered(passwordTextField.editText!!.text.toString())
            }
            .setNegativeButton(R.string.cancel) { _, _ ->
                onCancelled()
            }
            .setOnCancelListener {
                onCancelled()
            }
            .setOnDismissListener {
                this.dialog = null
            }
            .create()
        this.dialog = dialog
        dialog.show()
    }

    override fun closeAllDialogs() {
        if (this.dialog?.isShowing == true) {
            this.dialog?.dismiss()
        }
        this.dialog = null
    }
}
