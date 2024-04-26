package com.michaeltroger.gruenerpass

import android.content.Context
import android.content.SharedPreferences
import androidx.navigation.NavDestination
import androidx.navigation.NavDirections
import com.michaeltroger.gruenerpass.lock.AppLockedRepo
import com.michaeltroger.gruenerpass.lock.LockFragmentDirections
import com.michaeltroger.gruenerpass.settings.getBooleanFlow
import com.michaeltroger.gruenerpass.start.StartFragmentDirections
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class GetMainDestinationFlowUseCase @Inject constructor(
    @ApplicationContext context: Context,
    private val lockedRepo: AppLockedRepo,
    sharedPrefs: SharedPreferences,
) {
    private val showListLayout =
        sharedPrefs.getBooleanFlow(
            context.getString(R.string.key_preference_show_list_layout),
            false
        )

    operator fun invoke(currentDestination: NavDestination?): Flow<NavDirections?> {
        return combine(
            lockedRepo.isAppLocked(),
            showListLayout,
        ) { isLocked: Boolean, showListLayout: Boolean ->
            when {
                isLocked && currentDestination?.id != R.id.lockFragment -> {
                    NavGraphDirections.actionGlobalLockFragment()
                }
                !isLocked && currentDestination?.id == R.id.lockFragment -> {
                    if (showListLayout) {
                        LockFragmentDirections.actionGlobalCertificatesListFragment()
                    } else {
                        LockFragmentDirections.actionGlobalCertificatesFragment()
                    }
                }
                !isLocked && currentDestination?.id == R.id.startFragment -> {
                    if (showListLayout) {
                        StartFragmentDirections.actionGlobalCertificatesListFragment()
                    } else {
                        StartFragmentDirections.actionGlobalCertificatesFragment()
                    }
                }
                else -> {
                    null
                }
            }
        }
    }
}