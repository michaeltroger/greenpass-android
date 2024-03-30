package com.michaeltroger.gruenerpass.dialogs

import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import com.michaeltroger.gruenerpass.R
import com.michaeltroger.gruenerpass.db.Certificate
import com.michaeltroger.gruenerpass.documentorder.DocumentOrderItem
import com.xwray.groupie.GroupieAdapter

interface CertificateDialogs {
    fun closeAllDialogs()
    fun showEnterPasswordDialog(
        context: Context,
        onPasswordEntered: (String) -> Unit,
        onCancelled: () -> Unit
    )
    fun showDoYouWantToDeleteDialog(context: Context, id: String, onDeleteConfirmed: (String) -> Unit)
    fun showDoYouWantToDeleteAllDialog(context: Context, onDeleteAllConfirmed: () -> Unit)
    fun showWarningDialog(context: Context)
    fun showChangeDocumentNameDialog(
        context: Context,
        originalDocumentName: String,
        onDocumentNameChanged: (String) -> Unit
    )
    fun showChangeDocumentOrder(
        context: Context,
        originalOrder: List<Certificate>,
        onOrderChanged: (List<String>) -> Unit
    )
}

class CertificateDialogsImpl : CertificateDialogs {

    private var dialog: Dialog? = null

    override fun showDoYouWantToDeleteAllDialog(context: Context, onDeleteAllConfirmed: () -> Unit) {
        val dialog = MaterialAlertDialogBuilder(context)
            .setMessage(context.getString(R.string.dialog_delete_all_confirmation_message))
            .setPositiveButton(R.string.ok) { _, _ ->
                onDeleteAllConfirmed()
            }
            .setNegativeButton(context.getString(R.string.cancel), null)
            .setOnDismissListener {
                this.dialog = null
            }
            .create()
        this.dialog = dialog
        dialog.show()
    }

    override fun showWarningDialog(context: Context) {
        val dialog = MaterialAlertDialogBuilder(context)
            .setMessage(context.getString(R.string.dialog_warning))
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
            .setMessage(context.getString(R.string.dialog_delete_confirmation_message))
            .setPositiveButton(R.string.ok) { _, _ ->
                onDeleteConfirmed(id)
            }
            .setNegativeButton(context.getString(R.string.cancel), null)
            .setOnDismissListener {
                this.dialog = null
            }
            .create()
        this.dialog = dialog
        dialog.show()
    }

    override fun showChangeDocumentNameDialog(context: Context, originalDocumentName: String, onDocumentNameChanged: (String) -> Unit) {
        val customAlertDialogView = LayoutInflater.from(context)
            .inflate(R.layout.layout_document_name_dialog, null, false)

        val textField = customAlertDialogView.findViewById<TextInputLayout>(R.id.document_name_text_field).apply {
            editText!!.setText(originalDocumentName)
        }

        val dialog = MaterialAlertDialogBuilder(context)
            .setTitle(context.getString(R.string.dialog_document_name_title))
            .setView(customAlertDialogView)
            .setPositiveButton(R.string.ok) { _, _ ->
                onDocumentNameChanged(textField.editText!!.text.toString())
            }
            .setNegativeButton(context.getString(R.string.cancel), null)
            .setOnDismissListener {
                this.dialog = null
            }
            .create()
        this.dialog = dialog
        dialog.show()
    }

    override fun showChangeDocumentOrder(context: Context, originalOrder: List<Certificate>, onOrderChanged: (List<String>) -> Unit) {
        val customAlertDialogView = LayoutInflater.from(context)
            .inflate(R.layout.layout_document_order_dialog, null, false)

        val myAdapter = GroupieAdapter()
        val list = originalOrder.toMutableList()

        fun onUpClicked(id: String) {
            val index = list.indexOfFirst {
                it.id == id
            }
            val original = list.getOrNull(index)
            val new = list.getOrNull(index - 1)
            if (new != null) {
                list[index - 1] = original!!
                list[index] = new
                myAdapter.notifyItemMoved(index, index - 1)
            }
        }

        fun onDownClicked(id: String) {
            val index = list.indexOfFirst {
                it.id == id
            }
            val original = list.getOrNull(index)
            val new = list.getOrNull(index + 1)
            if (new != null) {
                list[index + 1] = original!!
                list[index] = new
                myAdapter.notifyItemMoved(index, index + 1)
            }
        }

        myAdapter.update(
            originalOrder.map { certificate ->
                DocumentOrderItem(
                    fileName = certificate.id,
                    documentName = certificate.name,
                    onDownClicked = {
                        onDownClicked(certificate.id)
                    },
                    onUpClicked = {
                        onUpClicked(certificate.id)
                    }
                )
            }
        )

        customAlertDialogView.findViewById<RecyclerView>(R.id.document_order).adapter = myAdapter

        val dialog = MaterialAlertDialogBuilder(context)
            .setTitle(context.getString(R.string.dialog_document_order_title))
            .setView(customAlertDialogView)
            .setPositiveButton(R.string.ok) { _, _ ->
                onOrderChanged(list.map { it.id })
            }
            .setNegativeButton(context.getString(R.string.cancel), null)
            .setOnDismissListener {
                this.dialog = null
            }
            .create()
        this.dialog = dialog
        dialog.show()
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
            .setTitle(context.getString(R.string.dialog_password_protection_title))
            .setView(customAlertDialogView)
            .setPositiveButton(R.string.ok) { _, _ ->
                onPasswordEntered(passwordTextField.editText!!.text.toString())
            }
            .setNegativeButton(context.getString(R.string.cancel)) { _, _ ->
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
