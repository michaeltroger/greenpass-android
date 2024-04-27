package com.michaeltroger.gruenerpass.navigation;

import android.app.Application
import android.content.SharedPreferences
import com.michaeltroger.gruenerpass.R
import com.michaeltroger.gruenerpass.lock.AppLockedRepo
import com.michaeltroger.gruenerpass.settings.getBooleanFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class GetStartDestinationUseCase @Inject constructor(
    app: Application
) {

    @Inject
    lateinit var lockedRepo: AppLockedRepo

    @Inject
    lateinit var sharedPrefs: SharedPreferences

    private val showListLayout by lazy {
        sharedPrefs.getBooleanFlow(
            app.getString(R.string.key_preference_show_list_layout),
            false
        )
    }

    suspend operator fun invoke(): Int {
        return combine(
            lockedRepo.isAppLocked(),
            showListLayout,
        ) { isAppLocked, showListLayout ->
            when {
                isAppLocked -> R.id.lockFragment
                showListLayout -> R.id.certificatesListFragment
                else -> R.id.certificatesFragment
            }
        }.first()
    }
}
