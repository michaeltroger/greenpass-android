package com.michaeltroger.gruenerpass.certificates

import android.content.Context
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.appcompat.widget.SearchView
import androidx.core.view.MenuProvider
import com.michaeltroger.gruenerpass.R
import com.michaeltroger.gruenerpass.certificates.search.SearchQueryTextListener
import com.michaeltroger.gruenerpass.certificates.states.ViewState

class CertificatesMenuProvider(
    private val context: Context,
    private val vm: CertificatesViewModel,
    private val forceHiddenScrollButtons: Boolean = false,
) : MenuProvider {
    private var searchView: SearchView? = null
    private var menu: Menu? = null

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.menu, menu)
        this.menu = menu

        val searchMenuItem = menu.findItem(R.id.search)
        searchView = searchMenuItem.actionView as SearchView
        searchView?.queryHint = context.getString(R.string.search_query_hint)
        restorePendingSearchQueryFilter(searchMenuItem)
        searchView?.setOnQueryTextListener(SearchQueryTextListener {
            vm.onSearchQueryChanged(it)
        })

        updateMenuState(vm.viewState.value)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean = when (menuItem.itemId) {
        R.id.add -> {
            vm.onAddFileSelected()
            true
        }

        R.id.warning -> {
            vm.onShowWarningDialogSelected()
            true
        }

        R.id.openMore -> {
            vm.onShowMoreSelected()
            true
        }

        R.id.openSettings -> {
            vm.onShowSettingsSelected()
            true
        }

        R.id.deleteFiltered -> {
            vm.onDeleteFilteredSelected()
            true
        }

        R.id.deleteAll -> {
            vm.onDeleteAllSelected()
            true
        }

        R.id.lock -> {
            vm.lockApp()
            true
        }

        R.id.export_filtered -> {
            vm.onExportFilteredSelected()
            true
        }

        R.id.export_all -> {
            vm.onExportAllSelected()
            true
        }

        R.id.scrollToFirst -> {
            vm.onScrollToFirstSelected()
            true
        }

        R.id.scrollToLast -> {
            vm.onScrollToLastSelected()
            true
        }

        R.id.changeOrder -> {
            vm.onChangeOrderSelected()
            true
        }

        else -> false
    }

    fun onPause() {
        searchView?.setOnQueryTextListener(null) // avoids an empty string to be sent
    }

    private fun restorePendingSearchQueryFilter(searchMenuItem: MenuItem) {
        val pendingFilter = (vm.viewState.value as? ViewState.Normal)?.filter ?: return
        if (pendingFilter.isNotEmpty()) {
            searchMenuItem.expandActionView()
            searchView?.setQuery(pendingFilter, false)
            searchView?.clearFocus()
        }
    }

    fun updateMenuState(state: ViewState) {
        menu?.apply {
            findItem(R.id.add)?.isVisible = state.showAddMenuItem
            findItem(R.id.warning)?.isVisible = state.showWarningButton
            findItem(R.id.openSettings)?.isVisible = state.showSettingsMenuItem
            findItem(R.id.deleteAll)?.isVisible = state.showDeleteAllMenuItem
            findItem(R.id.deleteFiltered)?.isVisible = state.showDeleteFilteredMenuItem
            findItem(R.id.lock)?.isVisible = state.showLockMenuItem
            findItem(R.id.export_all)?.isVisible = state.showExportAllMenuItem
            findItem(R.id.export_filtered)?.isVisible = state.showExportFilteredMenuItem
            findItem(R.id.changeOrder)?.isVisible = state.showChangeOrderMenuItem
            findItem(R.id.scrollToFirst)?.isVisible = if (forceHiddenScrollButtons) {
                false
            } else {
                state.showScrollToFirstMenuItem
            }
            findItem(R.id.scrollToLast)?.isVisible = if (forceHiddenScrollButtons) {
                false
            } else {
                state.showScrollToLastMenuItem
            }
            findItem(R.id.search)?.apply {
                isVisible = state.showSearchMenuItem
                if (!state.showSearchMenuItem) {
                    collapseActionView()
                }
            }
            findItem(R.id.openMore)?.isVisible = state.showMoreMenuItem
        }
    }
}
