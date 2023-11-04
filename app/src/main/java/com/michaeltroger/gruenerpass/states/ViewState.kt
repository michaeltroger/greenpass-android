package com.michaeltroger.gruenerpass.states

import com.michaeltroger.gruenerpass.db.Certificate

sealed class ViewState {
    abstract val fullBrightness: Boolean
    abstract val showOnLockedScreen: Boolean
    abstract val showLockMenuItem: Boolean
    abstract val showDeleteAllMenuItem: Boolean
    abstract val showAddMenuItem: Boolean
    abstract val showSettingsMenuItem: Boolean
    abstract val showExportAllMenuItem: Boolean
    abstract val showAuthenticateButton: Boolean
    abstract val showAddButton: Boolean
    abstract val showScrollToFirstMenuItem: Boolean
    abstract val showScrollToLastMenuItem: Boolean
    abstract val showSearchMenuItem: Boolean
    abstract val showMoreMenuItem: Boolean

    data class Initial(
        override val fullBrightness: Boolean,
        override val showOnLockedScreen: Boolean
    ) : ViewState() {
        override val showSearchMenuItem = false
        override val showLockMenuItem = false
        override val showDeleteAllMenuItem = false
        override val showAddMenuItem = false
        override val showSettingsMenuItem = false
        override val showExportAllMenuItem = false
        override val showAuthenticateButton = false
        override val showAddButton = false
        override val showScrollToFirstMenuItem = false
        override val showScrollToLastMenuItem = false
        override val showMoreMenuItem = true
    }

    data class Empty(
        override val fullBrightness: Boolean,
        override val showLockMenuItem: Boolean,
        override val showOnLockedScreen: Boolean
    ) : ViewState() {
        override val showSearchMenuItem = false
        override val showDeleteAllMenuItem = false
        override val showAddMenuItem = false
        override val showSettingsMenuItem = true
        override val showExportAllMenuItem = false
        override val showAuthenticateButton = false
        override val showAddButton = true
        override val showScrollToFirstMenuItem = false
        override val showScrollToLastMenuItem = false
        override val showMoreMenuItem = true
    }

    data class Normal(
        val documents: List<Certificate>,
        val searchQrCode: Boolean,
        val showDragButtons: Boolean,
        override val fullBrightness: Boolean,
        override val showLockMenuItem: Boolean,
        override val showScrollToFirstMenuItem: Boolean,
        override val showScrollToLastMenuItem: Boolean,
        override val showOnLockedScreen: Boolean,
        override val showSearchMenuItem: Boolean
    ) : ViewState() {
        override val showDeleteAllMenuItem = true
        override val showAddMenuItem = true
        override val showSettingsMenuItem = true
        override val showExportAllMenuItem = true
        override val showAuthenticateButton = false
        override val showAddButton = false
        override val showMoreMenuItem = true
    }

    data class Locked(
        override val fullBrightness: Boolean,
        override val showOnLockedScreen: Boolean
    ) : ViewState() {
        override val showSearchMenuItem = false
        override val showLockMenuItem = false
        override val showDeleteAllMenuItem = false
        override val showAddMenuItem = false
        override val showSettingsMenuItem = false
        override val showExportAllMenuItem = false
        override val showAuthenticateButton = true
        override val showAddButton = false
        override val showScrollToFirstMenuItem = false
        override val showScrollToLastMenuItem = false
        override val showMoreMenuItem = false
    }
}
