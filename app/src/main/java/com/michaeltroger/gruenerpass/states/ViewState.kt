package com.michaeltroger.gruenerpass.states

import com.michaeltroger.gruenerpass.db.Certificate

sealed class ViewState {
    abstract val fullBrightness: Boolean
    abstract val showLockMenuItem: Boolean
    abstract val showDeleteAllMenuItem: Boolean
    abstract val showAddMenuItem: Boolean
    abstract val showSettingsMenuItem: Boolean
    abstract val showExportAllMenuItem: Boolean
    abstract val showAuthenticateButton: Boolean
    abstract val showAddButton: Boolean
    abstract val showScrollToFirstMenuItem: Boolean
    abstract val showScrollToLastMenuItem: Boolean

    data class Initial(
        override val fullBrightness: Boolean
    ) : ViewState() {
        override val showLockMenuItem = false
        override val showDeleteAllMenuItem = false
        override val showAddMenuItem = false
        override val showSettingsMenuItem = false
        override val showExportAllMenuItem = false
        override val showAuthenticateButton = false
        override val showAddButton = false
        override val showScrollToFirstMenuItem = false
        override val showScrollToLastMenuItem = false
    }

    data class Empty(
        override val fullBrightness: Boolean,
        override val showLockMenuItem: Boolean,
    ) : ViewState() {
        override val showDeleteAllMenuItem = false
        override val showAddMenuItem = false
        override val showSettingsMenuItem = true
        override val showExportAllMenuItem = false
        override val showAuthenticateButton = false
        override val showAddButton = true
        override val showScrollToFirstMenuItem = false
        override val showScrollToLastMenuItem = false
    }

    data class Normal(
        val documents: List<Certificate>,
        val searchQrCode: Boolean,
        override val fullBrightness: Boolean,
        override val showLockMenuItem: Boolean,
        override val showScrollToFirstMenuItem: Boolean,
        override val showScrollToLastMenuItem: Boolean,
    ) : ViewState() {
        override val showDeleteAllMenuItem = true
        override val showAddMenuItem = true
        override val showSettingsMenuItem = true
        override val showExportAllMenuItem = true
        override val showAuthenticateButton = false
        override val showAddButton = false
    }

    data class Locked(
        override val fullBrightness: Boolean
    ) : ViewState() {
        override val showLockMenuItem = false
        override val showDeleteAllMenuItem = false
        override val showAddMenuItem = false
        override val showSettingsMenuItem = false
        override val showExportAllMenuItem = false
        override val showAuthenticateButton = true
        override val showAddButton = false
        override val showScrollToFirstMenuItem = false
        override val showScrollToLastMenuItem = false
    }
}
