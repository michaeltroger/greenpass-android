package com.michaeltroger.gruenerpass.states

import com.michaeltroger.gruenerpass.db.Certificate

sealed class ViewState {
    abstract val fullBrightness: Boolean
    abstract val showLockAppButton: Boolean
    abstract val showDeleteAllButton: Boolean
    abstract val showAddButton: Boolean
    abstract val showOpenSettingsButton: Boolean

    data class Initial(
        override val fullBrightness: Boolean
    ) : ViewState() {
        override val showLockAppButton = false
        override val showDeleteAllButton = false
        override val showAddButton = false
        override val showOpenSettingsButton = false
    }

    data class Empty(
        override val fullBrightness: Boolean,
        override val showLockAppButton: Boolean,
    ) : ViewState() {
        override val showDeleteAllButton = false
        override val showAddButton = false
        override val showOpenSettingsButton = true
    }

    data class Normal(
        val documents: List<Certificate>,
        val searchQrCode: Boolean,
        override val fullBrightness: Boolean,
        override val showLockAppButton: Boolean,
    ) : ViewState() {
        override val showDeleteAllButton = true
        override val showAddButton = true
        override val showOpenSettingsButton = true
    }

    data class Locked(
        override val fullBrightness: Boolean
    ) : ViewState() {
        override val showLockAppButton = false
        override val showDeleteAllButton = false
        override val showAddButton = false
        override val showOpenSettingsButton = false
    }
}
