package com.michaeltroger.gruenerpass.dialogs

import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import com.michaeltroger.gruenerpass.R

interface CertificateDialogs {
    fun closeAllDialogs()
    fun showEnterPasswordDialog(
        context: Context,
        onPasswordEntered: (String) -> Unit,
        onCancelled: () -> Unit
    )
    fun showDoYouWantToDeleteDialog(context: Context, id: String, onDeleteConfirmed: (String) -> Unit)
    fun showDoYouWantToDeleteAllDialog(context: Context, onDeleteAllConfirmed: () -> Unit)
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

    override fun showDoYouWantToDeleteDialog(context: Context, id: String, onDeleteConfirmed: (String) -> Unit) {
        val dialog = MaterialAlertDialogBuilder(context)
            .setMessage(context.getString(R.string.dialog_delete_confirmation_message))
            .setPositiveButton(R.string.ok) { _, _ ->
                this.dialog = null
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
