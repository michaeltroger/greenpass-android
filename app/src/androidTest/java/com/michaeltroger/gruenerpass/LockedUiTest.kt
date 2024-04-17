package com.michaeltroger.gruenerpass

import androidx.core.content.edit
import androidx.preference.PreferenceManager
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.michaeltroger.gruenerpass.robots.MainActivityRobot
import com.michaeltroger.gruenerpass.utils.FailingTestWatcher
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class LockedUiTest {

    @get:Rule
    val failingTestWatcher = FailingTestWatcher()

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        PreferenceManager.getDefaultSharedPreferences(context).edit {
            putBoolean(context.getString(R.string.key_preference_biometric), true)
        }
    }

    @Test
    fun verifyAppLocked() {
        ActivityScenario.launch(MainActivity::class.java)
        MainActivityRobot()
            .verifyLockedState()
    }
}
