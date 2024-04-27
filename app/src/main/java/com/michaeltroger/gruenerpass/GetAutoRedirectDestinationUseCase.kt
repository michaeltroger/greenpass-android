package com.michaeltroger.gruenerpass

import android.app.Application
import android.content.SharedPreferences
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavDirections
import com.michaeltroger.gruenerpass.lock.AppLockedRepo
import com.michaeltroger.gruenerpass.pdfimporter.PdfImporter
import com.michaeltroger.gruenerpass.settings.getBooleanFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class GetAutoRedirectDestinationUseCase @Inject constructor(
    app: Application
) {

    @Inject
    lateinit var lockedRepo: AppLockedRepo

    @Inject
    lateinit var pdfImporter: PdfImporter

    @Inject
    lateinit var sharedPrefs: SharedPreferences

    private val showListLayout by lazy {
        sharedPrefs.getBooleanFlow(
            app.getString(R.string.key_preference_show_list_layout),
            false
        )
    }

    operator fun invoke(navController: NavController): Flow<NavDirections?> {
        return combine(
            lockedRepo.isAppLocked(),
            showListLayout,
            pdfImporter.hasPendingFile(),
            navController.currentBackStackEntryFlow,
            ::autoRedirect
        )
    }

    private fun autoRedirect(
        isAppLocked: Boolean,
        showListLayout: Boolean,
        hasPendingFile: Boolean,
        navBackStackEntry: NavBackStackEntry
    ): NavDirections? {
        val currentDestinationId = navBackStackEntry.destination.id
        return when {
            // locked:
            isAppLocked -> {
                if (currentDestinationId == R.id.lockFragment) {
                    null
                } else {
                    NavGraphDirections.actionGlobalLockFragment()
                }
            }
            // unlocked:
            currentDestinationId == R.id.lockFragment -> {
                getCertificatesDestination(showListLayout)
            }
            currentDestinationId in listOf(
                R.id.moreFragment,
                R.id.settingsFragment,
                R.id.certificateDetailsFragment,
            ) -> {
                if (hasPendingFile) {
                    getCertificatesDestination(showListLayout)
                } else {
                    null
                }
            }
            currentDestinationId == R.id.certificatesFragment && showListLayout-> {
                NavGraphDirections.actionGlobalCertificatesListFragment()
            }
            currentDestinationId == R.id.certificatesListFragment && !showListLayout -> {
                NavGraphDirections.actionGlobalCertificatesFragment()
            }
            else -> {
                null // do nothing
            }
        }
    }

    private fun getCertificatesDestination(showListLayout: Boolean): NavDirections {
        return if (showListLayout) {
            NavGraphDirections.actionGlobalCertificatesListFragment()
        } else {
            NavGraphDirections.actionGlobalCertificatesFragment()
        }
    }
}
