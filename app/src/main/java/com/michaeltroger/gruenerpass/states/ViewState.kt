package com.michaeltroger.gruenerpass.states

import com.michaeltroger.gruenerpass.db.Certificate

sealed class ViewState {
    abstract val fullBrightness: Boolean
    abstract val showLockMenuItem: Boolean
    abstract val showDeleteAllMenuItem: Boolean
    abstract val showAddMenuItem: Boolean
    abstract val showSettingsMenuItem: Boolean
    abstract val showExportAllMenuItem: Boolean

    data class Initial(
        override val fullBrightness: Boolean
    ) : ViewState() {
        override val showLockMenuItem = false
        override val showDeleteAllMenuItem = false
        override val showAddMenuItem = false
        override val showSettingsMenuItem = false
        override val showExportAllMenuItem = false
    }

    data class Empty(
        override val fullBrightness: Boolean,
        override val showLockMenuItem: Boolean,
    ) : ViewState() {
        override val showDeleteAllMenuItem = false
        override val showAddMenuItem = false
        override val showSettingsMenuItem = true
        override val showExportAllMenuItem = false
    }

    data class Normal(
        val documents: List<Certificate>,
        val searchQrCode: Boolean,
        override val fullBrightness: Boolean,
        override val showLockMenuItem: Boolean,
    ) : ViewState() {
        override val showDeleteAllMenuItem = true
        override val showAddMenuItem = true
        override val showSettingsMenuItem = true
        override val showExportAllMenuItem = true
    }

    data class Locked(
        override val fullBrightness: Boolean
    ) : ViewState() {
        override val showLockMenuItem = false
        override val showDeleteAllMenuItem = false
        override val showAddMenuItem = false
        override val showSettingsMenuItem = false
        override val showExportAllMenuItem = false
    }
}
