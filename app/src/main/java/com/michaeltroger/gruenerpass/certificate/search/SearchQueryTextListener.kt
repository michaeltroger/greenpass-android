package com.michaeltroger.gruenerpass.certificate.search

import androidx.appcompat.widget.SearchView

class SearchQueryTextListener(
    private val onTextChanged: (String) -> Unit
) : SearchView.OnQueryTextListener {
    override fun onQueryTextSubmit(query: String): Boolean {
        return false
    }

    override fun onQueryTextChange(newText: String): Boolean {
        onTextChanged(newText)
        return false
    }
}
