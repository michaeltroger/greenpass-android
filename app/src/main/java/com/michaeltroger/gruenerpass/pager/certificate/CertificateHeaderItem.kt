package com.michaeltroger.gruenerpass.pager.certificate

import android.view.View
import com.michaeltroger.gruenerpass.R
import com.michaeltroger.gruenerpass.databinding.ItemCertificateHeaderBinding
import com.xwray.groupie.Item
import com.xwray.groupie.viewbinding.BindableItem
import com.xwray.groupie.viewbinding.GroupieViewHolder
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Surface
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.michaeltroger.gruenerpass.theme.GreenPassTheme

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CertificateHeader(
    documentName: String,
    onDeleteCalled: () -> Unit,
    onDocumentNameChanged: (String) -> Unit,
    onStartDrag: () -> Unit
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxSize() ) {
            TextField(
                value = documentName,
                onValueChange = { onDocumentNameChanged(it) },
                singleLine = true,
                maxLines = 1,
                colors = TextFieldDefaults.textFieldColors(
                    backgroundColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent
                ),
                modifier = Modifier.width(200.dp)
            )
            IconButton(
                onClick = { onDeleteCalled() }
            ) {
                Icon(Icons.Filled.Delete, contentDescription = "")
            }
            IconButton(
                onClick = { /*TODO*/ },
                modifier = Modifier.combinedClickable(
                        onClick = {},
                        onLongClick = { onStartDrag() }
                    )
            ) {
                Icon(Icons.Filled.Menu, contentDescription = "")
            }
            Spacer(modifier = Modifier.width(dimensionResource(R.dimen.space_small)))
        }
    }

}

@Preview(showBackground = true)
@Composable
fun CertificateHeaderPreview() {
    GreenPassTheme {
        CertificateHeader(
            documentName = "My PDF",
            onDeleteCalled = {  },
            onDocumentNameChanged = { },
            onStartDrag = { }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun CertificateHeaderDarkPreview() {
    GreenPassTheme(darkTheme = true) {
        CertificateHeader(
            documentName = "My PDF",
            onDeleteCalled = { },
            onDocumentNameChanged = { },
            onStartDrag = { }
        )
    }
}

class CertificateHeaderItem(
    private val documentName: String,
    private val fileName: String,
    private val onDeleteCalled: () -> Unit,
    private val onDocumentNameChanged: (String) -> Unit,
    private val onStartDrag: () -> Unit
) : BindableItem<ItemCertificateHeaderBinding>() {

    override fun initializeViewBinding(view: View): ItemCertificateHeaderBinding = ItemCertificateHeaderBinding.bind(view)
    override fun getLayout() = R.layout.item_certificate_header

    override fun bind(viewBinding: ItemCertificateHeaderBinding, position: Int) {
        viewBinding.composeView.setContent {
            CertificateHeader(
                documentName = documentName,
                onDeleteCalled = { onDeleteCalled() },
                onDocumentNameChanged = { onDocumentNameChanged(it) },
                onStartDrag = { onStartDrag() }
            )
        }
    }

    override fun bind(
        viewHolder: GroupieViewHolder<ItemCertificateHeaderBinding>,
        position: Int,
        payloads: MutableList<Any>
    ) {
        super.bind(viewHolder, position, payloads)

    }

    override fun isSameAs(other: Item<*>): Boolean {
        return viewType == other.viewType
    }

    override fun hasSameContentAs(other: Item<*>): Boolean {
        return (other as? CertificateHeaderItem)?.fileName == fileName && (other as? CertificateHeaderItem)?.documentName == documentName
    }
}