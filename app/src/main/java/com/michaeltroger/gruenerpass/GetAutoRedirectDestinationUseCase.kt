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

    operator fun invoke(navController: NavController): Flow<Result> {
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
    ): Result {
        val currentDestinationId = navBackStackEntry.destination.id
        val destination = when {
            // locked:
            isAppLocked -> {
                if (currentDestinationId == R.id.lockFragment) {
                    null
                } else {
                    NavGraphDirections.actionGlobalLockFragmentClearedBackstack()
                }
            }
            // unlocked:
            currentDestinationId == R.id.lockFragment -> {
                if (showListLayout) {
                    NavGraphDirections.actionGlobalCertificatesListFragmentClearedBackstack()
                } else {
                    NavGraphDirections.actionGlobalCertificatesFragmentClearedBackstack()
                }
            }
            currentDestinationId in listOf(
                R.id.moreFragment,
                R.id.settingsFragment,
                R.id.certificateDetailsFragment,
            ) -> {
                if (hasPendingFile) {
                    return Result.NavigateBack
                } else {
                    null
                }
            }
            currentDestinationId == R.id.certificatesFragment && showListLayout-> {
                NavGraphDirections.actionGlobalCertificatesListFragmentClearedBackstack()
            }
            currentDestinationId == R.id.certificatesListFragment && !showListLayout -> {
                NavGraphDirections.actionGlobalCertificatesFragmentClearedBackstack()
            }
            else -> {
                null // do nothing
            }
        } ?: return Result.NothingTodo

        return Result.NavigateTo(destination)
    }

    sealed class Result {
        data class NavigateTo(val navDirections: NavDirections): Result()
        data object NavigateBack: Result()
        data object NothingTodo: Result()
    }
}
